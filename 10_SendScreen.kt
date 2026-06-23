package com.sam.stt.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.sam.stt.ui.theme.*
import com.sam.stt.network.FirebaseSignaling
import com.sam.stt.webrtc.WebRTCManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    var selectedFiles by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var compressFiles by remember { mutableStateOf(false) }
    var roomCode by remember { mutableStateOf("") }
    var isWaiting by remember { mutableStateOf(false) }
    var showQrCode by remember { mutableStateOf(false) }
    var connectionStatus by remember { mutableStateOf("Prêt") }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        selectedFiles = uris
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Envoyer", color = TextWhite) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour", tint = TextWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (!isWaiting) {
                // Sélection de fichiers
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    onClick = { filePicker.launch("*/*") }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = YellowThunder
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Touchez pour choisir des fichiers",
                            color = TextWhite,
                            fontSize = 16.sp
                        )
                        Text(
                            "Vidéos, audio, documents...",
                            color = TextGray,
                            fontSize = 12.sp
                        )
                    }
                }

                // Liste des fichiers sélectionnés
                if (selectedFiles.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "${selectedFiles.size} fichier(s) sélectionné(s)",
                        color = TextWhite,
                        fontWeight = FontWeight.Medium
                    )
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 200.dp)
                    ) {
                        items(selectedFiles) { uri ->
                            FileItem(uri = uri)
                        }
                    }
                }

                // Option compression
                if (selectedFiles.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = compressFiles,
                            onCheckedChange = { compressFiles = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = YellowThunder,
                                uncheckedColor = TextGray
                            )
                        )
                        Text(
                            "Compresser avant envoi",
                            color = TextWhite,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            "Plus rapide",
                            color = TextGray,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Bouton générer code
                Button(
                    onClick = {
                        if (selectedFiles.isNotEmpty()) {
                            roomCode = generateRoomCode()
                            isWaiting = true
                            showQrCode = true
                            connectionStatus = "En attente du destinataire..."
                            // Lancer la connexion WebRTC
                            scope.launch {
                                startSending(roomCode)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = YellowThunder,
                        contentColor = DarkBackground
                    ),
                    enabled = selectedFiles.isNotEmpty()
                ) {
                    Icon(Icons.Default.Send, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("GÉNÉRER LE CODE", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

            } else {
                // Écran d'attente avec le code
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "Votre code",
                        color = TextGray,
                        fontSize = 16.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Code à 6 chiffres
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        roomCode.toCharArray().forEach { digit ->
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(SurfaceDark, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    digit.toString(),
                                    color = YellowThunder,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // QR Code placeholder
                    if (showQrCode) {
                        Box(
                            modifier = Modifier
                                .size(200.dp)
                                .background(TextWhite, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "QR Code
$roomCode",
                                color = DarkBackground,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        connectionStatus,
                        color = BlueLightning,
                        fontSize = 14.sp
                    )

                    // Indicateur de connexion
                    if (connectionStatus.contains("attente")) {
                        Spacer(modifier = Modifier.height(16.dp))
                        CircularProgressIndicator(color = YellowThunder)
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Boutons d'action
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        OutlinedButton(
                            onClick = { /* Partager le code */ },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = BlueLightning
                            )
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Partager")
                        }

                        OutlinedButton(
                            onClick = {
                                // Suppression manuelle de la salle
                                scope.launch {
                                    FirebaseSignaling().deleteRoom(roomCode)
                                    isWaiting = false
                                    roomCode = ""
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = ErrorRed
                            )
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Annuler")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FileItem(uri: Uri) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.InsertDriveFile,
                contentDescription = null,
                tint = BlueLightning,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    uri.lastPathSegment ?: "Fichier",
                    color = TextWhite,
                    fontSize = 14.sp,
                    maxLines = 1
                )
                Text(
                    "Prêt à envoyer",
                    color = TextGray,
                    fontSize = 12.sp
                )
            }
        }
    }
}

private fun generateRoomCode(): String {
    return (100000..999999).random().toString()
}

private suspend fun startSending(roomCode: String) {
    // Initialiser WebRTC et Firestore
    // Cette fonction sera complétée dans MainActivity avec les vraies instances
}
