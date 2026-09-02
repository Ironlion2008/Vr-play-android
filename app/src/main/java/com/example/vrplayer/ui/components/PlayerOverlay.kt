package com.example.vrplayer.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.example.vrplayer.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun PlayerOverlay(
    player: Player,
    title: String,
    onBackClick: () -> Unit,
    onEnvironmentClick: () -> Unit,
    onSettingsClick: () -> Unit,
    projectionMode: String,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(true) }
    var controlsLocked by remember { mutableStateOf(false) }
    var showVolumeIndicator by remember { mutableStateOf(false) }
    var showBrightnessIndicator by remember { mutableStateOf(false) }
    var currentVolume by remember { mutableFloatStateOf(0.5f) }
    var currentBrightness by remember { mutableFloatStateOf(1f) }
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp

    // Auto-hide controls
    LaunchedEffect(isVisible) {
        if (isVisible && !controlsLocked) {
            delay(3000)
            isVisible = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(controlsLocked) {
                if (!controlsLocked) {
                    detectTapGestures(
                        onTap = {
                            isVisible = !isVisible
                        }
                    )
                }
            }
            .pointerInput(controlsLocked) {
                if (!controlsLocked) {
                    detectVerticalDragGestures { change, dragAmount ->
                        val xPos = change.position.x
                        when {
                            xPos < size.width / 3 -> {
                                // Brightness control
                                currentBrightness = (currentBrightness - dragAmount / 500f)
                                    .coerceIn(0.1f, 1f)
                                showBrightnessIndicator = true
                                showVolumeIndicator = false
                            }
                            xPos > size.width * 2 / 3 -> {
                                // Volume control
                                currentVolume = (currentVolume - dragAmount / 500f)
                                    .coerceIn(0f, 1f)
                                showVolumeIndicator = true
                                showBrightnessIndicator = false
                            }
                        }
                        isVisible = true
                    }
                }
            }
    ) {
        // Brightness indicator
        AnimatedVisibility(
            visible = showBrightnessIndicator,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            VerticalIndicator(
                value = currentBrightness,
                label = "Brightness",
                icon = Icons.Default.Brightness6,
                onValueChange = { currentBrightness = it }
            )
        }

        // Volume indicator
        AnimatedVisibility(
            visible = showVolumeIndicator,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            VerticalIndicator(
                value = currentVolume,
                label = "Volume",
                icon = Icons.Default.VolumeUp,
                onValueChange = { currentVolume = it }
            )
        }

        // Controls overlay
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                // Top bar
                TopBar(
                    title = title,
                    projectionMode = projectionMode,
                    onBackClick = onBackClick,
                    onEnvironmentClick = onEnvironmentClick,
                    onSettingsClick = onSettingsClick,
                    controlsLocked = controlsLocked,
                    onLockToggle = { controlsLocked = !controlsLocked }
                )

                Spacer(modifier = Modifier.weight(1f))

                // Center controls
                CenterControls(
                    player = player,
                    isLocked = controlsLocked
                )

                Spacer(modifier = Modifier.weight(1f))

                // Bottom controls
                BottomControls(player = player)
            }
        }

        // Lock indicator when controls are locked
        if (controlsLocked && !isVisible) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Controls Locked",
                    tint = VRPrimaryDark,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun TopBar(
    title: String,
    projectionMode: String,
    onBackClick: () -> Unit,
    onEnvironmentClick: () -> Unit,
    onSettingsClick: () -> Unit,
    controlsLocked: Boolean,
    onLockToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(VRPlayerControls)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = VRTextPrimary
            )
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = VRTextPrimary,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )

        // Projection mode badge
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = VRSecondaryDark.copy(alpha = 0.8f)
        ) {
            Text(
                text = projectionMode,
                style = MaterialTheme.typography.labelSmall,
                color = VRTextPrimary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(onClick = onEnvironmentClick) {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = "Environment",
                tint = VRTextPrimary
            )
        }

        IconButton(onClick = onSettingsClick) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = VRTextPrimary
            )
        }

        IconButton(onClick = onLockToggle) {
            Icon(
                imageVector = if (controlsLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                contentDescription = if (controlsLocked) "Unlock Controls" else "Lock Controls",
                tint = if (controlsLocked) VRPrimaryDark else VRTextPrimary
            )
        }
    }
}

@Composable
private fun CenterControls(
    player: Player,
    isLocked: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rewind 10 seconds
        IconButton(
            onClick = {
                player.seekTo((player.currentPosition - 10000).coerceAtLeast(0))
            },
            enabled = !isLocked,
            modifier = Modifier.size(64.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Replay10,
                contentDescription = "Rewind 10 seconds",
                tint = VRTextPrimary,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.width(24.dp))

        // Play/Pause
        IconButton(
            onClick = {
                if (player.isPlaying) {
                    player.pause()
                } else {
                    player.play()
                }
            },
            enabled = !isLocked,
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(VRPrimaryDark)
        ) {
            Icon(
                imageVector = if (player.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (player.isPlaying) "Pause" else "Play",
                tint = Color.Black,
                modifier = Modifier.size(56.dp)
            )
        }

        Spacer(modifier = Modifier.width(24.dp))

        // Forward 10 seconds
        IconButton(
            onClick = {
                player.seekTo((player.currentPosition + 10000).coerceAtMost(player.duration))
            },
            enabled = !isLocked,
            modifier = Modifier.size(64.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Forward10,
                contentDescription = "Forward 10 seconds",
                tint = VRTextPrimary,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

@Composable
private fun BottomControls(player: Player) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(VRPlayerControls)
            .padding(16.dp)
    ) {
        // Progress bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatTime(player.currentPosition),
                style = MaterialTheme.typography.labelSmall,
                color = VRTextPrimary
            )

            Slider(
                value = player.currentPosition.toFloat(),
                onValueChange = { player.seekTo(it.toLong()) },
                valueRange = 0f..player.duration.coerceAtLeast(1).toFloat(),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                colors = SliderDefaults.colors(
                    thumbColor = VRPrimaryDark,
                    activeTrackColor = VRPrimaryDark,
                    inactiveTrackColor = VRSeekBarTrack
                )
            )

            Text(
                text = formatTime(player.duration),
                style = MaterialTheme.typography.labelSmall,
                color = VRTextPrimary
            )
        }
    }
}

@Composable
private fun VerticalIndicator(
    value: Float,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.8f))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = VRPrimaryDark,
            modifier = Modifier.size(32.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "${(value * 100).toInt()}%",
            style = MaterialTheme.typography.titleMedium,
            color = VRTextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1f,
            modifier = Modifier
                .height(150.dp)
                .width(48.dp),
            colors = SliderDefaults.colors(
                thumbColor = VRPrimaryDark,
                activeTrackColor = VRPrimaryDark,
                inactiveTrackColor = VRSeekBarTrack
            )
        )
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "00:00"
    val seconds = (ms / 1000) % 60
    val minutes = (ms / 1000 / 60) % 60
    val hours = ms / 1000 / 60 / 60

    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
