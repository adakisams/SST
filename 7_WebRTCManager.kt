package com.sam.stt.webrtc

import android.content.Context
import com.sam.stt.network.FirebaseSignaling
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.webrtc.*
import java.nio.ByteBuffer
import java.util.concurrent.Executors

class WebRTCManager(
    private val context: Context,
    private val signaling: FirebaseSignaling,
    private val roomCode: String,
    private val isSender: Boolean
) {
    private val executor = Executors.newSingleThreadExecutor()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var dataChannel: DataChannel? = null
    private var localAudioTrack: AudioTrack? = null
    private var localVideoTrack: VideoTrack? = null

    private val iceServers = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer()
    )

    private val _connectionState = MutableStateFlow<PeerConnectionState>(PeerConnectionState.NEW)
    val connectionState: StateFlow<PeerConnectionState> = _connectionState

    private val _dataChannelState = MutableStateFlow<DataChannel.State>(DataChannel.State.CLOSED)
    val dataChannelState: StateFlow<DataChannel.State> = _dataChannelState

    private var onMessageCallback: ((ByteBuffer) -> Unit)? = null
    private var onDataChannelOpenCallback: (() -> Unit)? = null

    init {
        initializePeerConnectionFactory()
    }

    private fun initializePeerConnectionFactory() {
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(null, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(null))
            .createPeerConnectionFactory()
    }

    fun createPeerConnection() {
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }

        peerConnection = peerConnectionFactory?.createPeerConnection(
            rtcConfig,
            object : PeerConnection.Observer {
                override fun onSignalingChange(newState: PeerConnection.SignalingState?) {}
                override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
                    newState?.let {
                        when (it) {
                            PeerConnection.IceConnectionState.CONNECTED,
                            PeerConnection.IceConnectionState.COMPLETED ->
                                _connectionState.value = PeerConnectionState.CONNECTED
                            PeerConnection.IceConnectionState.DISCONNECTED ->
                                _connectionState.value = PeerConnectionState.DISCONNECTED
                            PeerConnection.IceConnectionState.FAILED ->
                                _connectionState.value = PeerConnectionState.FAILED
                            else -> {}
                        }
                    }
                }
                override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState?) {}
                override fun onIceCandidate(candidate: IceCandidate?) {
                    candidate?.let {
                        scope.launch {
                            signaling.addIceCandidate(roomCode, it, isSender)
                        }
                    }
                }
                override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
                override fun onAddStream(stream: MediaStream?) {}
                override fun onRemoveStream(stream: MediaStream?) {}
                override fun onDataChannel(dc: DataChannel?) {
                    dataChannel = dc
                    setupDataChannel(dc)
                }
                override fun onRenegotiationNeeded() {}
                override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {}
            }
        )
    }

    suspend fun createOffer(): SessionDescription? = suspendCancellableCoroutine { continuation ->
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "false"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"))
        }

        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {
                desc?.let {
                    peerConnection?.setLocalDescription(object : SdpObserver {
                        override fun onSetSuccess() {
                            continuation.resume(it) {}
                        }
                        override fun onSetFailure(error: String?) {
                            continuation.resume(null) {}
                        }
                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onCreateFailure(error: String?) {}
                    }, it)
                } ?: continuation.resume(null) {}
            }
            override fun onCreateFailure(error: String?) {
                continuation.resume(null) {}
            }
            override fun onSetSuccess() {}
            override fun onSetFailure(error: String?) {}
        }, constraints)
    }

    suspend fun createAnswer(offer: SessionDescription): SessionDescription? = suspendCancellableCoroutine { continuation ->
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {
                val constraints = MediaConstraints().apply {
                    mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "false"))
                    mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"))
                }

                peerConnection?.createAnswer(object : SdpObserver {
                    override fun onCreateSuccess(desc: SessionDescription?) {
                        desc?.let {
                            peerConnection?.setLocalDescription(object : SdpObserver {
                                override fun onSetSuccess() {
                                    continuation.resume(it) {}
                                }
                                override fun onSetFailure(error: String?) {
                                    continuation.resume(null) {}
                                }
                                override fun onCreateSuccess(p0: SessionDescription?) {}
                                override fun onCreateFailure(error: String?) {}
                            }, it)
                        } ?: continuation.resume(null) {}
                    }
                    override fun onCreateFailure(error: String?) {
                        continuation.resume(null) {}
                    }
                    override fun onSetSuccess() {}
                    override fun onSetFailure(error: String?) {}
                }, constraints)
            }
            override fun onSetFailure(error: String?) {
                continuation.resume(null) {}
            }
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onCreateFailure(error: String?) {}
        }, offer)
    }

    fun setRemoteAnswer(answer: SessionDescription) {
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {}
            override fun onSetFailure(error: String?) {}
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onCreateFailure(error: String?) {}
        }, answer)
    }

    fun createDataChannel(label: String = "fileTransfer") {
        val init = DataChannel.Init().apply {
            ordered = true
            maxRetransmits = 10
        }
        dataChannel = peerConnection?.createDataChannel(label, init)
        setupDataChannel(dataChannel)
    }

    private fun setupDataChannel(dc: DataChannel?) {
        dc?.registerObserver(object : DataChannel.Observer {
            override fun onBufferedAmountChange(previousAmount: Long) {}
            override fun onStateChange() {
                _dataChannelState.value = dc.state()
                if (dc.state() == DataChannel.State.OPEN) {
                    onDataChannelOpenCallback?.invoke()
                }
            }
            override fun onMessage(buffer: DataChannel.Buffer?) {
                buffer?.let {
                    onMessageCallback?.invoke(it.data)
                }
            }
        })
    }

    fun sendData(data: ByteArray): Boolean {
        val buffer = DataChannel.Buffer(ByteBuffer.wrap(data), true)
        return dataChannel?.send(buffer) ?: false
    }

    fun sendChunk(chunk: com.sam.stt.model.FileChunk): Boolean {
        val chunkData = ByteBuffer.allocate(4 + chunk.data.size + 1)
        chunkData.putInt(chunk.sequenceNumber)
        chunkData.put(chunk.data)
        chunkData.put(if (chunk.isLast) 1.toByte() else 0.toByte())
        return sendData(chunkData.array())
    }

    fun setOnMessage(callback: (ByteBuffer) -> Unit) {
        onMessageCallback = callback
    }

    fun setOnDataChannelOpen(callback: () -> Unit) {
        onDataChannelOpenCallback = callback
    }

    fun addIceCandidate(candidate: IceCandidate) {
        peerConnection?.addIceCandidate(candidate)
    }

    fun close() {
        dataChannel?.close()
        dataChannel = null
        peerConnection?.close()
        peerConnection = null
        peerConnectionFactory?.dispose()
        peerConnectionFactory = null
        scope.cancel()
        executor.shutdown()
    }

    enum class PeerConnectionState {
        NEW, CONNECTED, DISCONNECTED, FAILED, CLOSED
    }
}
