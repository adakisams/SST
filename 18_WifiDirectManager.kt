package com.sam.stt.wifi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.p2p.*
import android.os.Looper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

class WifiDirectManager(private val context: Context) {

    private val wifiP2pManager: WifiP2pManager = 
        context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
    private val channel: WifiP2pManager.Channel = 
        wifiP2pManager.initialize(context, Looper.getMainLooper(), null)

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _peers = MutableStateFlow<List<WifiP2pDevice>>(emptyList())
    val peers: StateFlow<List<WifiP2pDevice>> = _peers

    private val _connectionState = MutableStateFlow<WifiDirectState>(WifiDirectState.IDLE)
    val connectionState: StateFlow<WifiDirectState> = _connectionState

    private var receiver: WifiDirectReceiver? = null
    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null

    private val intentFilter = IntentFilter().apply {
        addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
    }

    fun startDiscovery() {
        receiver = WifiDirectReceiver()
        context.registerReceiver(receiver, intentFilter)

        wifiP2pManager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                _connectionState.value = WifiDirectState.SEARCHING
            }
            override fun onFailure(reason: Int) {
                _connectionState.value = WifiDirectState.ERROR
            }
        })
    }

    fun connectToDevice(device: WifiP2pDevice) {
        val config = WifiP2pConfig().apply {
            deviceAddress = device.deviceAddress
            wps.setup = WpsInfo.PBC
        }

        wifiP2pManager.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                _connectionState.value = WifiDirectState.CONNECTING
            }
            override fun onFailure(reason: Int) {
                _connectionState.value = WifiDirectState.ERROR
            }
        })
    }

    fun createGroup() {
        wifiP2pManager.createGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                _connectionState.value = WifiDirectState.GROUP_OWNER
            }
            override fun onFailure(reason: Int) {
                _connectionState.value = WifiDirectState.ERROR
            }
        })
    }

    fun startServer(port: Int = 8888) {
        scope.launch {
            try {
                serverSocket = ServerSocket(port)
                _connectionState.value = WifiDirectState.LISTENING
                clientSocket = serverSocket?.accept()
                _connectionState.value = WifiDirectState.CONNECTED
            } catch (e: Exception) {
                e.printStackTrace()
                _connectionState.value = WifiDirectState.ERROR
            }
        }
    }

    fun connectToServer(hostAddress: String, port: Int = 8888) {
        scope.launch {
            try {
                clientSocket = Socket()
                clientSocket?.connect(InetSocketAddress(hostAddress, port), 5000)
                _connectionState.value = WifiDirectState.CONNECTED
            } catch (e: Exception) {
                e.printStackTrace()
                _connectionState.value = WifiDirectState.ERROR
            }
        }
    }

    fun sendData(data: ByteArray): Boolean {
        return try {
            clientSocket?.getOutputStream()?.write(data)
            clientSocket?.getOutputStream()?.flush()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun receiveData(bufferSize: Int = 8192): ByteArray? {
        return try {
            val buffer = ByteArray(bufferSize)
            val bytesRead = clientSocket?.getInputStream()?.read(buffer) ?: -1
            if (bytesRead > 0) buffer.copyOf(bytesRead) else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun disconnect() {
        try {
            clientSocket?.close()
            serverSocket?.close()
            wifiP2pManager.removeGroup(channel, null)
            receiver?.let { context.unregisterReceiver(it) }
            receiver = null
            _connectionState.value = WifiDirectState.IDLE
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopDiscovery() {
        wifiP2pManager.stopPeerDiscovery(channel, null)
        receiver?.let { context.unregisterReceiver(it) }
        receiver = null
    }

    inner class WifiDirectReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                    val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                    if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                        // Wi-Fi Direct activé
                    }
                }
                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    wifiP2pManager.requestPeers(channel) { peerList ->
                        _peers.value = peerList.deviceList.toList()
                    }
                }
                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    val networkInfo = intent.getParcelableExtra<android.net.NetworkInfo>(
                        WifiP2pManager.EXTRA_NETWORK_INFO
                    )
                    if (networkInfo?.isConnected == true) {
                        wifiP2pManager.requestConnectionInfo(channel) { info ->
                            if (info.groupFormed) {
                                _connectionState.value = if (info.isGroupOwner) {
                                    WifiDirectState.GROUP_OWNER
                                } else {
                                    WifiDirectState.CONNECTED
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    enum class WifiDirectState {
        IDLE, SEARCHING, CONNECTING, CONNECTED, GROUP_OWNER, LISTENING, ERROR
    }
}
