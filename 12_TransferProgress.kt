package com.sam.stt.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sam.stt.model.TransferProgress
import com.sam.stt.model.TransferStatus
import com.sam.stt.ui.theme.*

@Composable
fun TransferProgressView(
    progress: TransferProgress,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onDeleteRoom: () -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = if (progress.totalBytes > 0) {
            (progress.bytesTransferred.toFloat() / progress.totalBytes).coerceIn(0f, 1f)
        } else 0f,
        animationSpec = tween(durationMillis = 500),
        label = "progress"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // En-tête avec statut
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (progress.status) {
                        TransferStatus.PENDING -> "En attente..."
                        TransferStatus.IN_PROGRESS -> "Transfert en cours"
                        TransferStatus.PAUSED -> "En pause"
                        TransferStatus.COMPLETED -> "Terminé ✅"
                        TransferStatus.FAILED -> "Échoué ❌"
                        TransferStatus.CANCELLED -> "Annulé"
                    },
                    color = when (progress.status) {
                        TransferStatus.COMPLETED -> SuccessGreen
                        TransferStatus.FAILED, TransferStatus.CANCELLED -> ErrorRed
                        else -> TextWhite
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                // Badge vitesse
                if (progress.status == TransferStatus.IN_PROGRESS) {
                    SpeedBadge(speedKbps = progress.speedKbps)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Barre de progression
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(DarkBackground)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            when (progress.status) {
                                TransferStatus.COMPLETED -> SuccessGreen
                                TransferStatus.FAILED, TransferStatus.CANCELLED -> ErrorRed
                                else -> YellowThunder
                            }
                        )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Informations de progression
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "${formatBytes(progress.bytesTransferred)} / ${formatBytes(progress.totalBytes)}",
                    color = TextGray,
                    fontSize = 13.sp
                )
                Text(
                    "${(animatedProgress * 100).toInt()}%",
                    color = YellowThunder,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Temps restant
            if (progress.status == TransferStatus.IN_PROGRESS && progress.remainingSeconds > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Temps restant : ${formatDuration(progress.remainingSeconds)}",
                    color = BlueLightning,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Boutons de contrôle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                when (progress.status) {
                    TransferStatus.IN_PROGRESS -> {
                        ControlButton(
                            icon = Icons.Default.Pause,
                            label = "Pause",
                            color = BlueLightning,
                            onClick = onPause
                        )
                        ControlButton(
                            icon = Icons.Default.Close,
                            label = "Annuler",
                            color = ErrorRed,
                            onClick = onCancel
                        )
                    }
                    TransferStatus.PAUSED -> {
                        ControlButton(
                            icon = Icons.Default.PlayArrow,
                            label = "Reprendre",
                            color = SuccessGreen,
                            onClick = onResume
                        )
                        ControlButton(
                            icon = Icons.Default.Close,
                            label = "Annuler",
                            color = ErrorRed,
                            onClick = onCancel
                        )
                    }
                    TransferStatus.COMPLETED -> {
                        ControlButton(
                            icon = Icons.Default.FolderOpen,
                            label = "Ouvrir",
                            color = BlueLightning,
                            onClick = { /* Ouvrir le fichier */ }
                        )
                        ControlButton(
                            icon = Icons.Default.Delete,
                            label = "Terminer",
                            color = ErrorRed,
                            onClick = onDeleteRoom
                        )
                    }
                    TransferStatus.FAILED, TransferStatus.CANCELLED -> {
                        ControlButton(
                            icon = Icons.Default.Refresh,
                            label = "Réessayer",
                            color = YellowThunder,
                            onClick = { /* Réessayer */ }
                        )
                        ControlButton(
                            icon = Icons.Default.Delete,
                            label = "Terminer",
                            color = ErrorRed,
                            onClick = onDeleteRoom
                        )
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
private fun SpeedBadge(speedKbps: Double) {
    Box(
        modifier = Modifier
            .background(
                BlueLightning.copy(alpha = 0.2f),
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            "⚡ ${formatSpeed(speedKbps)}",
            color = BlueLightning,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FilledTonalButton(
            onClick = onClick,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = color.copy(alpha = 0.15f),
                contentColor = color
            )
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, color = color.copy(alpha = 0.8f), fontSize = 11.sp)
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 * 1024 -> "%.2f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
        bytes >= 1024 * 1024 -> "%.2f MB".format(bytes / (1024.0 * 1024.0))
        bytes >= 1024 -> "%.2f KB".format(bytes / 1024.0)
        else -> "$bytes B"
    }
}

private fun formatSpeed(kbps: Double): String {
    return when {
        kbps >= 1024 -> "%.1f MB/s".format(kbps / 1024)
        else -> "%.0f KB/s".format(kbps)
    }
}

private fun formatDuration(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
}
