package com.example.vrplayer.ui.screens

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.vrplayer.data.EnvironmentPreset
import com.example.vrplayer.data.EnvironmentPresets
import com.example.vrplayer.data.ProjectionMode
import com.example.vrplayer.data.StereoMode
import com.example.vrplayer.ui.components.PlayerOverlay
import com.example.vrplayer.ui.theme.*

@Composable
fun PlayerScreen(
    player: ExoPlayer,
    title: String,
    videoUri: Uri?,
    environmentId: String,
    projectionMode: ProjectionMode,
    stereoMode: StereoMode,
    onBackClick: () -> Unit,
    onEnvironmentClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val environment = EnvironmentPresets.getPresetById(environmentId)
        ?: EnvironmentPresets.getPresetById("default_dark")

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VRBackgroundDark)
    ) {
        // Environment background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = environment?.gradientColors ?: listOf(
                            VRBackgroundDark,
                            VRSurfaceDark
                        )
                    )
                )
        )

        // ExoPlayer view
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = false
                    setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Player overlay
        PlayerOverlay(
            player = player,
            title = title,
            onBackClick = onBackClick,
            onEnvironmentClick = onEnvironmentClick,
            onSettingsClick = onSettingsClick,
            projectionMode = projectionMode.displayName,
            modifier = Modifier.fillMaxSize()
        )
    }
}
