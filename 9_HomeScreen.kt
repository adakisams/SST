package com.sam.stt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    var showLanguageDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "⚡ STT ⚡",
                        color = YellowThunder,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                subtitle = {
                    Text(
                        "Sam's Thunder Transfert",
                        color = BlueLightning.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                ),
                actions = {
                    IconButton(onClick = { showLanguageDialog = true }) {
                        Text("🌐", fontSize = 20.sp)
                    }
                }
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo / Icône centrale
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(BlueLightning.copy(alpha = 0.15f), RoundedCornerShape(60.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "⚡",
                    fontSize = 60.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Transfert P2P",
                color = TextWhite,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                "Sans stockage serveur • 100% Gratuit",
                color = TextGray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Bouton ENVOYER
            Button(
                onClick = { navController.navigate("send") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = YellowThunder,
                    contentColor = DarkBackground
                )
            ) {
                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "ENVOYER",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bouton RECEVOIR
            Button(
                onClick = { navController.navigate("receive") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BlueLightning,
                    contentColor = TextWhite
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "RECEVOIR",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Historique et Favoris
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                HomeActionButton(
                    icon = Icons.Default.History,
                    label = "Historique",
                    onClick = { navController.navigate("history") }
                )
                HomeActionButton(
                    icon = Icons.Default.Star,
                    label = "Favoris",
                    onClick = { navController.navigate("favorites") }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Info mode hors-ligne
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = SurfaceDark
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📡", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "Mode hors-ligne disponible",
                            color = TextWhite,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Wi-Fi Direct • Même sans Internet",
                            color = TextGray,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }

    if (showLanguageDialog) {
        LanguageSelectionDialog(
            onDismiss = { showLanguageDialog = false },
            onLanguageSelected = { /* Changer langue */ }
        )
    }
}

@Composable
private fun HomeActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FilledTonalButton(
            onClick = onClick,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = SurfaceDark,
                contentColor = BlueLightning
            )
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(28.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = TextGray, fontSize = 12.sp)
    }
}

@Composable
fun LanguageSelectionDialog(
    onDismiss: () -> Unit,
    onLanguageSelected: (String) -> Unit
) {
    val languages = listOf(
        "Français" to "fr",
        "English" to "en",
        "日本語" to "ja",
        "Deutsch" to "de",
        "Kabiyé" to "kbp",
        "Éwé" to "ee",
        "Nawdm" to "nmz"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Langue / Language", color = TextWhite) },
        containerColor = SurfaceDark,
        text = {
            Column {
                languages.forEach { (name, code) ->
                    TextButton(
                        onClick = {
                            onLanguageSelected(code)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(name, color = TextWhite, fontSize = 16.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", color = BlueLightning)
            }
        }
    )
}
