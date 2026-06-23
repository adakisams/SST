package com.sam.stt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.sam.stt.ui.theme.*
import com.sam.stt.network.FirebaseSignaling
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiveScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    var codeInput by remember { mutableStateOf("") }
    var isConnecting by remember { mutableStateOf(false) }
    var connectionStatus by remember { mutableStateOf("") }
    var showPreview by remember { mutableStateOf(false) }
    var fileName by remember { mutableStateOf("") }
    var fileSize by remember { mutableStateOf(0L) }
    var fileType by remember { mutableStateOf("") }
    var senderName by remember { mutableStateOf("Anonyme") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recevoir", color = TextWhite) },
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isConnecting && !showPreview) {
                // Titre
                Text(
                    "Entrer le code",
                    color = TextWhite,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "Demandez le code à 6 chiffres à l'envoyeur",
                    color = TextGray,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
                )

                // Champs de saisie du code
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    CodeDigitBox(
                        value = codeInput.getOrNull(0)?.toString() ?: "",
                        onValueChange = { /* Géré par le clavier */ }
                    )
                    CodeDigitBox(
                        value = codeInput.getOrNull(1)?.toString() ?: "",
                        onValueChange = { }
                    )
                    CodeDigitBox(
                        value = codeInput.getOrNull(2)?.toString() ?: "",
                        onValueChange = { }
                    )
                    CodeDigitBox(
                        value = codeInput.getOrNull(3)?.toString() ?: "",
                        onValueChange = { }
                    )
                    CodeDigitBox(
                        value = codeInput.getOrNull(4)?.toString() ?: "",
                        onValueChange = { }
                    )
                    CodeDigitBox(
                        value = codeInput.getOrNull(5)?.toString() ?: "",
                        onValueChange = { }
                    )
                }

                // Clavier numérique personnalisé
                NumericKeyboard(
                    onDigitClick = { digit ->
                        if (codeInput.length < 6) {
                            codeInput += digit
                        }
                    },
                    onBackspace = {
                        if (codeInput.isNotEmpty()) {
                            codeInput = codeInput.dropLast(1)
                        }
                    },
                    onClear = {
                        codeInput = ""
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Bouton Scanner QR
                OutlinedButton(
                    onClick = { /* Ouvrir scanner QR */ },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = BlueLightning
                    )
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scanner un QR Code")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bouton Se connecter
                Button(
                    onClick = {
                        if (codeInput.length == 6) {
                            isConnecting = true
                            connectionStatus = "Connexion en cours..."
                            scope.launch {
                                connectToRoom(codeInput)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BlueLightning,
                        contentColor = TextWhite
                    ),
                    enabled = codeInput.length == 6
                ) {
                    Icon(Icons.Default.ConnectWithoutContact, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SE CONNECTER", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

            } else if (isConnecting && !showPreview) {
                // Écran de connexion en cours
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = BlueLightning,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        connectionStatus,
                        color = TextWhite,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Code : $codeInput",
                        color = YellowThunder,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    OutlinedButton(
                        onClick = {
                            isConnecting = false
                            codeInput = ""
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)
                    ) {
                        Text("Annuler")
                    }
                }

            } else if (showPreview) {
                // Prévisualisation avant acceptation
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Fichier reçu",
                        color = TextWhite,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Icône du fichier
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(BlueLightning.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.InsertDriveFile,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = BlueLightning
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Détails du fichier
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            DetailRow("Nom", fileName)
                            DetailRow("Taille", formatFileSize(fileSize))
                            DetailRow("Type", fileType)
                            DetailRow("De", senderName)
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Boutons Accepter / Refuser
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = {
                                // Accepter le transfert
                                showPreview = false
                                isConnecting = true
                                connectionStatus = "Téléchargement en cours..."
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SuccessGreen,
                                contentColor = TextWhite
                            )
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ACCEPTER")
                        }

                        Button(
                            onClick = {
                                // Refuser
                                showPreview = false
                                isConnecting = false
                                codeInput = ""
                                scope.launch {
                                    FirebaseSignaling().updateStatus(codeInput, "cancelled")
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ErrorRed,
                                contentColor = TextWhite
                            )
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("REFUSER")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CodeDigitBox(value: String, onValueChange: (String) -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(
                if (value.isNotEmpty()) BlueLightning.copy(alpha = 0.2f) else SurfaceDark,
                RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = value,
            color = if (value.isNotEmpty()) YellowThunder else TextGray,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun NumericKeyboard(
    onDigitClick: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit
) {
    val digits = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("C", "0", "⌫")
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        digits.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                row.forEach { digit ->
                    val isAction = digit == "C" || digit == "⌫"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1.5f)
                            .background(
                                if (isAction) SurfaceDark else SurfaceDark.copy(alpha = 0.5f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        TextButton(
                            onClick = {
                                when (digit) {
                                    "C" -> onClear()
                                    "⌫" -> onBackspace()
                                    else -> onDigitClick(digit)
                                }
                            }
                        ) {
                            Text(
                                digit,
                                color = when (digit) {
                                    "C" -> ErrorRed
                                    "⌫" -> TextGray
                                    else -> TextWhite
                                },
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextGray, fontSize = 14.sp)
        Text(value, color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 * 1024 -> "%.2f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
        bytes >= 1024 * 1024 -> "%.2f MB".format(bytes / (1024.0 * 1024.0))
        bytes >= 1024 -> "%.2f KB".format(bytes / 1024.0)
        else -> "$bytes B"
    }
}

private suspend fun connectToRoom(code: String) {
    // Cette fonction sera implémentée dans MainActivity
    // Elle initialise WebRTC, rejoint la salle Firestore, etc.
}
