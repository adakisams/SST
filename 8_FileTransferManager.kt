package com.sam.stt.transfer

import android.content.Context
import android.net.Uri
import com.sam.stt.model.*
import com.sam.stt.webrtc.WebRTCManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.*
import java.security.MessageDigest
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream
import java.util.zip.InflaterInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.nio.ByteBuffer

class FileTransferManager(
    private val context: Context,
    private val webRTCManager: WebRTCManager
) {
    companion object {
        const val CHUNK_SIZE = 16384 // 16 KB par chunk
        const val BUFFER_SIZE = 8192
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _transferProgress = MutableStateFlow(TransferProgress())
    val transferProgress: StateFlow<TransferProgress> = _transferProgress

    private val receivedChunks = mutableMapOf<Int, ByteArray>()
    private var currentMetadata: FileMetadata? = null
    private var totalChunks = 0
    private var isTransferPaused = false
    private var isTransferCancelled = false

    /**
     * Envoie un fichier (ou plusieurs fichiers zippés)
     */
    suspend fun sendFile(
        uri: Uri,
        compress: Boolean = false,
        onProgress: (TransferProgress) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext false
            val fileName = getFileName(uri)
            val fileSize = getFileSize(uri)

            // Compression si demandé
            val (finalInputStream, finalSize, finalName, isCompressed) = if (compress) {
                val compressed = compressInputStream(inputStream)
                Triple(compressed.first, compressed.second, "$fileName.gz", true)
            } else {
                Triple(inputStream, fileSize, fileName, false)
            }

            val chunkCount = ((finalSize + CHUNK_SIZE - 1) / CHUNK_SIZE).toInt()
            val checksum = calculateChecksum(finalInputStream)

            // Réouvrir le stream après checksum
            val sendStream = if (compress) {
                compressInputStream(context.contentResolver.openInputStream(uri)!!).first
            } else {
                context.contentResolver.openInputStream(uri)!!
            }

            val metadata = FileMetadata(
                fileName = finalName,
                fileSize = finalSize,
                fileType = context.contentResolver.getType(uri) ?: "application/octet-stream",
                chunkCount = chunkCount,
                checksum = checksum,
                isCompressed = isCompressed
            )

            // Envoyer métadonnées d'abord
            sendMetadata(metadata)

            // Attendre que le récepteur accepte
            // (dans la vraie app, on attend un signal de confirmation)
            delay(1000)

            // Envoyer les chunks
            val buffer = ByteArray(CHUNK_SIZE)
            var bytesRead: Int
            var sequenceNumber = 0
            var totalBytesSent = 0L
            val startTime = System.currentTimeMillis()

            while (sendStream.read(buffer).also { bytesRead = it } != -1) {
                if (isTransferCancelled) {
                    sendStream.close()
                    return@withContext false
                }

                while (isTransferPaused) {
                    delay(100)
                }

                val chunkData = if (bytesRead < CHUNK_SIZE) buffer.copyOf(bytesRead) else buffer.copyOf()
                val isLast = bytesRead < CHUNK_SIZE || totalBytesSent + bytesRead >= finalSize

                val chunk = FileChunk(
                    sequenceNumber = sequenceNumber,
                    data = chunkData,
                    isLast = isLast
                )

                // Envoyer via WebRTC
                val success = webRTCManager.sendChunk(chunk)
                if (!success) {
                    // Réessayer
                    delay(100)
                    webRTCManager.sendChunk(chunk)
                }

                sequenceNumber++
                totalBytesSent += bytesRead

                // Mettre à jour la progression
                val elapsed = System.currentTimeMillis() - startTime
                val speed = if (elapsed > 0) (totalBytesSent * 1000.0 / elapsed / 1024) else 0.0
                val remaining = if (speed > 0) ((finalSize - totalBytesSent) / (speed * 1024)).toInt() else 0

                _transferProgress.value = TransferProgress(
                    bytesTransferred = totalBytesSent,
                    totalBytes = finalSize,
                    speedKbps = speed,
                    remainingSeconds = remaining,
                    status = TransferStatus.IN_PROGRESS
                )
                onProgress(_transferProgress.value)

                // Petit délai pour ne pas saturer le DataChannel
                delay(5)
            }

            sendStream.close()
            _transferProgress.value = _transferProgress.value.copy(status = TransferStatus.COMPLETED)
            true

        } catch (e: Exception) {
            e.printStackTrace()
            _transferProgress.value = _transferProgress.value.copy(status = TransferStatus.FAILED)
            false
        }
    }

    /**
     * Envoie plusieurs fichiers sous forme de ZIP
     */
    suspend fun sendMultipleFiles(
        uris: List<Uri>,
        compress: Boolean = false,
        onProgress: (TransferProgress) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val zipFile = File(context.cacheDir, "stt_transfer_${System.currentTimeMillis()}.zip")
            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                uris.forEach { uri ->
                    val name = getFileName(uri)
                    zos.putNextEntry(ZipEntry(name))
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        input.copyTo(zos)
                    }
                    zos.closeEntry()
                }
            }
            sendFile(Uri.fromFile(zipFile), compress, onProgress)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Reçoit les données via WebRTC DataChannel
     */
    fun startReceiving(onFileComplete: (File, FileMetadata) -> Unit) {
        webRTCManager.setOnMessage { buffer ->
            scope.launch {
                processReceivedData(buffer, onFileComplete)
            }
        }
    }

    private suspend fun processReceivedData(
        buffer: ByteBuffer,
        onFileComplete: (File, FileMetadata) -> Unit
    ) {
        val data = ByteArray(buffer.remaining())
        buffer.get(data)

        // Vérifier si c'est des métadonnées (JSON) ou un chunk binaire
        val str = String(data, Charsets.UTF_8)
        if (str.startsWith("{"fileName":")) {
            // C'est des métadonnées
            parseMetadata(str)
        } else {
            // C'est un chunk binaire
            processChunk(data, onFileComplete)
        }
    }

    private fun parseMetadata(json: String) {
        // Parse simple (dans la vraie app, utiliser Gson ou kotlinx.serialization)
        // Format: {"fileName":"...","fileSize":123,"fileType":"...","chunkCount":10,"checksum":"...","isCompressed":false}
        // Simplifié pour l'exemple
    }

    private fun sendMetadata(metadata: FileMetadata) {
        val json = """{"fileName":"${metadata.fileName}","fileSize":${metadata.fileSize},"fileType":"${metadata.fileType}","chunkCount":${metadata.chunkCount},"checksum":"${metadata.checksum}","isCompressed":${metadata.isCompressed}}"""
        webRTCManager.sendData(json.toByteArray(Charsets.UTF_8))
    }

    private fun processChunk(data: ByteArray, onFileComplete: (File, FileMetadata) -> Unit) {
        if (data.size < 5) return

        val seqNum = ByteBuffer.wrap(data.copyOfRange(0, 4)).int
        val isLast = data[4].toInt() == 1
        val chunkData = data.copyOfRange(5, data.size)

        receivedChunks[seqNum] = chunkData

        // Mettre à jour la progression
        currentMetadata?.let { meta ->
            val received = receivedChunks.size
            val progress = (received * 100 / meta.chunkCount)

            _transferProgress.value = TransferProgress(
                bytesTransferred = receivedChunks.values.sumOf { it.size.toLong() },
                totalBytes = meta.fileSize,
                status = if (isLast && received >= meta.chunkCount) TransferStatus.COMPLETED else TransferStatus.IN_PROGRESS
            )

            if (isLast || received >= meta.chunkCount) {
                assembleFile(meta, onFileComplete)
            }
        }
    }

    private fun assembleFile(metadata: FileMetadata, onFileComplete: (File, FileMetadata) -> Unit) {
        try {
            val outputFile = File(context.cacheDir, metadata.fileName)
            FileOutputStream(outputFile).use { fos ->
                for (i in 0 until receivedChunks.size) {
                    receivedChunks[i]?.let { fos.write(it) }
                }
            }

            // Décompresser si nécessaire
            val finalFile = if (metadata.isCompressed) {
                decompressFile(outputFile)
            } else outputFile

            onFileComplete(finalFile, metadata)
            receivedChunks.clear()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun compressInputStream(input: InputStream): Pair<InputStream, Long> {
        val baos = ByteArrayOutputStream()
        DeflaterOutputStream(baos, Deflater(Deflater.BEST_SPEED)).use { dos ->
            input.copyTo(dos)
        }
        val compressed = baos.toByteArray()
        return Pair(ByteArrayInputStream(compressed), compressed.size.toLong())
    }

    private fun decompressFile(file: File): File {
        val output = File(context.cacheDir, file.name.removeSuffix(".gz"))
        InflaterInputStream(FileInputStream(file)).use { input ->
            FileOutputStream(output).use { output ->
                input.copyTo(output)
            }
        }
        return output
    }

    private fun calculateChecksum(input: InputStream): String {
        val md = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(BUFFER_SIZE)
        var read: Int
        while (input.read(buffer).also { read = it } != -1) {
            md.update(buffer, 0, read)
        }
        input.close()
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) result = cursor.getString(idx)
                }
            }
        }
        return result ?: uri.lastPathSegment ?: "unknown_file"
    }

    private fun getFileSize(uri: Uri): Long {
        var size: Long = 0
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (idx >= 0) size = cursor.getLong(idx)
                }
            }
        }
        return size
    }

    fun pauseTransfer() {
        isTransferPaused = true
        _transferProgress.value = _transferProgress.value.copy(status = TransferStatus.PAUSED)
    }

    fun resumeTransfer() {
        isTransferPaused = false
        _transferProgress.value = _transferProgress.value.copy(status = TransferStatus.IN_PROGRESS)
    }

    fun cancelTransfer() {
        isTransferCancelled = true
        _transferProgress.value = _transferProgress.value.copy(status = TransferStatus.CANCELLED)
    }

    fun cleanup() {
        scope.cancel()
        receivedChunks.clear()
    }
}
