package com.sam.stt.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "transfer_sessions")
data class TransferSession(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val roomCode: String,
    val fileName: String,
    val fileSize: Long,
    val fileType: String,
    val senderName: String = "Anonyme",
    val timestamp: Long = System.currentTimeMillis(),
    val status: TransferStatus = TransferStatus.PENDING,
    val isFavorite: Boolean = false,
    val localPath: String? = null,
    val isIncoming: Boolean = true
)

enum class TransferStatus {
    PENDING,      // En attente d'acceptation
    IN_PROGRESS,  // Transfert en cours
    PAUSED,       // En pause
    COMPLETED,    // Terminé
    FAILED,       // Échoué
    CANCELLED     // Annulé
}

data class FileChunk(
    val sequenceNumber: Int,
    val data: ByteArray,
    val isLast: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as FileChunk
        return sequenceNumber == other.sequenceNumber &&
                data.contentEquals(other.data) &&
                isLast == other.isLast
    }

    override fun hashCode(): Int {
        var result = sequenceNumber
        result = 31 * result + data.contentHashCode()
        result = 31 * result + isLast.hashCode()
        return result
    }
}

data class FileMetadata(
    val fileName: String,
    val fileSize: Long,
    val fileType: String,
    val chunkCount: Int,
    val checksum: String,
    val isCompressed: Boolean = false
)

data class TransferProgress(
    val bytesTransferred: Long = 0,
    val totalBytes: Long = 0,
    val speedKbps: Double = 0.0,
    val remainingSeconds: Int = 0,
    val status: TransferStatus = TransferStatus.PENDING
)
