package com.sam.stt.network

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import java.util.concurrent.TimeUnit

class FirebaseSignaling(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    companion object {
        const val ROOMS_COLLECTION = "rooms"
        const val FIELD_OFFER = "offer"
        const val FIELD_ANSWER = "answer"
        const val FIELD_ICE_CANDIDATES = "iceCandidates"
        const val FIELD_STATUS = "status"
        const val FIELD_CREATED_AT = "createdAt"
        const val FIELD_METADATA = "metadata"
        const val STATUS_WAITING = "waiting"
        const val STATUS_CONNECTING = "connecting"
        const val STATUS_CONNECTED = "connected"
        const val STATUS_COMPLETED = "completed"
        const val STATUS_CANCELLED = "cancelled"
        const val ROOM_EXPIRATION_MINUTES = 10L
    }

    /**
     * Crée une salle de signalisation avec un code à 6 chiffres
     */
    suspend fun createRoom(roomCode: String, offer: SessionDescription): Boolean {
        return try {
            val roomData = hashMapOf(
                FIELD_OFFER to mapOf(
                    "type" to offer.type.canonicalForm(),
                    "sdp" to offer.description
                ),
                FIELD_STATUS to STATUS_WAITING,
                FIELD_CREATED_AT to com.google.firebase.Timestamp.now(),
                FIELD_ICE_CANDIDATES to hashMapOf(
                    "sender" to mutableListOf<Map<String, String>>(),
                    "receiver" to mutableListOf<Map<String, String>>()
                )
            )
            db.collection(ROOMS_COLLECTION).document(roomCode).set(roomData).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Rejoint une salle existante avec une réponse SDP
     */
    suspend fun joinRoom(roomCode: String, answer: SessionDescription): Boolean {
        return try {
            val updates = hashMapOf<String, Any>(
                FIELD_ANSWER to mapOf(
                    "type" to answer.type.canonicalForm(),
                    "sdp" to answer.description
                ),
                FIELD_STATUS to STATUS_CONNECTING
            )
            db.collection(ROOMS_COLLECTION).document(roomCode).update(updates).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Écoute les changements dans une salle (pour recevoir la réponse)
     */
    fun listenToRoom(roomCode: String): Flow<RoomUpdate> = callbackFlow {
        val listener = db.collection(ROOMS_COLLECTION).document(roomCode)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val status = snapshot.getString(FIELD_STATUS) ?: STATUS_WAITING
                    val answerMap = snapshot.get(FIELD_ANSWER) as? Map<*, *>
                    val iceCandidates = snapshot.get(FIELD_ICE_CANDIDATES) as? Map<*, *>
                    val metadata = snapshot.get(FIELD_METADATA) as? Map<*, *>

                    val answer = answerMap?.let {
                        val type = it["type"] as? String
                        val sdp = it["sdp"] as? String
                        if (type != null && sdp != null) {
                            SessionDescription(
                                SessionDescription.Type.fromCanonicalForm(type),
                                sdp
                            )
                        } else null
                    }

                    trySend(RoomUpdate(status, answer, iceCandidates, metadata))
                }
            }

        awaitClose { listener.remove() }
    }

    /**
     * Ajoute un candidat ICE à la salle
     */
    suspend fun addIceCandidate(roomCode: String, candidate: IceCandidate, isSender: Boolean) {
        try {
            val candidateData = mapOf(
                "sdpMid" to candidate.sdpMid,
                "sdpMLineIndex" to candidate.sdpMLineIndex,
                "candidate" to candidate.sdp
            )

            val field = if (isSender) "sender" else "receiver"
            val docRef = db.collection(ROOMS_COLLECTION).document(roomCode)

            docRef.get().await().let { snapshot ->
                val iceMap = snapshot.get(FIELD_ICE_CANDIDATES) as? Map<*, *>
                val senderList = (iceMap?.get("sender") as? List<*>)?.toMutableList() ?: mutableListOf()
                val receiverList = (iceMap?.get("receiver") as? List<*>)?.toMutableList() ?: mutableListOf()

                if (isSender) senderList.add(candidateData)
                else receiverList.add(candidateData)

                docRef.update(
                    FIELD_ICE_CANDIDATES, mapOf(
                        "sender" to senderList,
                        "receiver" to receiverList
                    )
                ).await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Met à jour le statut de la salle
     */
    suspend fun updateStatus(roomCode: String, status: String) {
        try {
            db.collection(ROOMS_COLLECTION).document(roomCode)
                .update(FIELD_STATUS, status).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Supprime manuellement une salle (bouton "Terminer")
     */
    suspend fun deleteRoom(roomCode: String): Boolean {
        return try {
            db.collection(ROOMS_COLLECTION).document(roomCode).delete().await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Nettoie les salles expirées (à appeler périodiquement)
     */
    suspend fun cleanExpiredRooms() {
        try {
            val expirationTime = com.google.firebase.Timestamp(
                java.util.Date(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(ROOM_EXPIRATION_MINUTES))
            )

            val expiredRooms = db.collection(ROOMS_COLLECTION)
                .whereLessThan(FIELD_CREATED_AT, expirationTime)
                .get()
                .await()

            for (doc in expiredRooms.documents) {
                doc.reference.delete().await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Envoie les métadonnées du fichier dans la salle
     */
    suspend fun sendMetadata(roomCode: String, metadata: com.sam.stt.model.FileMetadata) {
        try {
            val metadataMap = hashMapOf(
                "fileName" to metadata.fileName,
                "fileSize" to metadata.fileSize,
                "fileType" to metadata.fileType,
                "chunkCount" to metadata.chunkCount,
                "checksum" to metadata.checksum,
                "isCompressed" to metadata.isCompressed
            )
            db.collection(ROOMS_COLLECTION).document(roomCode)
                .update(FIELD_METADATA, metadataMap).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

data class RoomUpdate(
    val status: String,
    val answer: SessionDescription?,
    val iceCandidates: Map<*, *>?,
    val metadata: Map<*, *>?
)
