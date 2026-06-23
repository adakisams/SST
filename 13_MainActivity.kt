package com.sam.stt

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.sam.stt.network.FirebaseSignaling
import com.sam.stt.ui.screens.*
import com.sam.stt.ui.theme.STTTheme
import com.sam.stt.webrtc.WebRTCManager
import kotlinx.coroutines.*

class MainActivity : ComponentActivity() {

    private lateinit var signaling: FirebaseSignaling
    private var webRTCManager: WebRTCManager? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialiser Firebase
        FirebaseApp.initializeApp(this)
        signaling = FirebaseSignaling()

        // Nettoyer les salles expirées au démarrage
        scope.launch(Dispatchers.IO) {
            signaling.cleanExpiredRooms()
        }

        // Gérer les intents de partage depuis d'autres apps
        handleShareIntent(intent)

        setContent {
            STTTheme {
                val navController = rememberNavController()
                var sharedFiles by remember { mutableStateOf<List<Uri>>(emptyList()) }

                // Récupérer les fichiers partagés
                LaunchedEffect(intent) {
                    sharedFiles = handleShareIntent(intent)
                }

                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        HomeScreen(navController = navController)
                    }
                    composable("send") {
                        SendScreen(navController = navController)
                    }
                    composable("receive") {
                        ReceiveScreen(navController = navController)
                    }
                    composable("history") {
                        // HistoryScreen(navController = navController)
                        // À implémenter avec Room database
                    }
                    composable("favorites") {
                        // FavoritesScreen(navController = navController)
                        // À implémenter avec Room database
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleShareIntent(it) }
    }

    private fun handleShareIntent(intent: Intent): List<Uri> {
        val files = mutableListOf<Uri>()
        when (intent.action) {
            Intent.ACTION_SEND -> {
                intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let { files.add(it) }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)?.let { files.addAll(it) }
            }
        }
        return files
    }

    /**
     * Initialise WebRTC pour l'envoi
     */
    fun initializeSender(roomCode: String) {
        webRTCManager = WebRTCManager(this, signaling, roomCode, isSender = true)
        webRTCManager?.createPeerConnection()
        webRTCManager?.createDataChannel("fileTransfer")

        scope.launch {
            val offer = webRTCManager?.createOffer()
            offer?.let {
                signaling.createRoom(roomCode, it)
            }
        }
    }

    /**
     * Initialise WebRTC pour la réception
     */
    fun initializeReceiver(roomCode: String) {
        webRTCManager = WebRTCManager(this, signaling, roomCode, isSender = false)
        webRTCManager?.createPeerConnection()

        scope.launch {
            // Écouter l'offre de l'envoyeur
            signaling.listenToRoom(roomCode).collect { update ->
                when (update.status) {
                    "waiting" -> {
                        // Attendre l'offre
                        update.answer?.let { answer ->
                            // L'envoyeur a reçu la réponse
                        }
                    }
                    "connecting" -> {
                        update.answer?.let { answer ->
                            webRTCManager?.setRemoteAnswer(answer)
                        }
                    }
                    "connected" -> {
                        // Connexion établie !
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        webRTCManager?.close()
        scope.cancel()
    }
}
