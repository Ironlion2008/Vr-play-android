package com.example.vrplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.vrplayer.data.EnvironmentPreset
import com.example.vrplayer.data.LocalMedia
import com.example.vrplayer.ui.theme.*
import com.example.vrplayer.util.FileUtils

@Composable
fun EnvironmentCard(
    preset: EnvironmentPreset,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable { onSelect() }
            .then(
                if (isSelected) {
                    Modifier.border(3.dp, VRPrimaryDark, RoundedCornerShape(16.dp))
                } else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = VRCardBackground)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Gradient background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(preset.gradientColors)
                    )
            )

            // Selection indicator
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = VRPrimaryDark,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(28.dp)
                )
            }

            // Environment name
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(8.dp)
            ) {
                Text(
                    text = preset.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = VRTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun MediaCard(
    media: LocalMedia,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = VRCardBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail placeholder
            Box(
                modifier = Modifier
                    .size(80.dp, 60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(VRSurfaceDark),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.VideoFile,
                    contentDescription = null,
                    tint = VRPrimaryDark,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = media.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = VRTextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (media.resolution.isNotEmpty()) {
                        Text(
                            text = media.resolution,
                            style = MaterialTheme.typography.labelSmall,
                            color = VRPrimaryDark
                        )
                    }

                    if (media.duration > 0) {
                        Text(
                            text = FileUtils.formatDuration(media.duration),
                            style = MaterialTheme.typography.labelSmall,
                            color = VRTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = FileUtils.formatFileSize(media.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = VRTextSecondary
                )
            }

            Icon(
                imageVector = Icons.Default.PlayCircle,
                contentDescription = "Play",
                tint = VRPrimaryDark,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

@Composable
fun ServerCard(
    name: String,
    ip: String,
    port: Int,
    status: ServerStatus,
    onConnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onConnect() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = VRCardBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status indicator
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        color = when (status) {
                            ServerStatus.ONLINE -> VROnline
                            ServerStatus.OFFLINE -> VROffline
                            ServerStatus.CONNECTING -> VRConnecting
                        },
                        shape = CircleShape
                    )
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall,
                    color = VRTextPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "$ip:$port",
                    style = MaterialTheme.typography.bodySmall,
                    color = VRPrimaryDark
                )
            }

            when (status) {
                ServerStatus.CONNECTING -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = VRConnecting,
                        strokeWidth = 2.dp
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Connect",
                        tint = VRTextSecondary
                    )
                }
            }
        }
    }
}

enum class ServerStatus {
    ONLINE, OFFLINE, CONNECTING
}
