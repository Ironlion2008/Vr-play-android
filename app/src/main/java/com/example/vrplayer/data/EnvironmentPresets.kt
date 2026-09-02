package com.example.vrplayer.data

import android.graphics.Color
import androidx.compose.ui.graphics.Color as ComposeColor

data class EnvironmentPreset(
    val id: String,
    val name: String,
    val description: String,
    val previewColor: ComposeColor,
    val gradientColors: List<ComposeColor>
)

object EnvironmentPresets {

    val defaultEnvironments = listOf(
        EnvironmentPreset(
            id = "default_dark",
            name = "Dark Studio",
            description = "Professional dark studio environment",
            previewColor = ComposeColor(0xFF0B0E14),
            gradientColors = listOf(
                ComposeColor(0xFF0B0E14),
                ComposeColor(0xFF16192A)
            )
        ),
        EnvironmentPreset(
            id = "cinema",
            name = "Virtual Cinema",
            description = "Theater-like cinema environment",
            previewColor = ComposeColor(0xFF1A1A1A),
            gradientColors = listOf(
                ComposeColor(0xFF1A1A1A),
                ComposeColor(0xFF4A0E0E)
            )
        ),
        EnvironmentPreset(
            id = "space_nebula",
            name = "Space Nebula",
            description = "Cosmic space environment",
            previewColor = ComposeColor(0xFF1A0033),
            gradientColors = listOf(
                ComposeColor(0xFF1A0033),
                ComposeColor(0xFF2A1A6A)
            )
        ),
        EnvironmentPreset(
            id = "cyberpunk",
            name = "Cyberpunk",
            description = "Neon cyberpunk aesthetic",
            previewColor = ComposeColor(0xFF0D1117),
            gradientColors = listOf(
                ComposeColor(0xFF0D1117),
                ComposeColor(0xFF1A3A3A)
            )
        ),
        EnvironmentPreset(
            id = "gradient_blue",
            name = "Neon Blue",
            description = "Blue gradient background",
            previewColor = ComposeColor(0xFF0A192F),
            gradientColors = listOf(
                ComposeColor(0xFF0A192F),
                ComposeColor(0xFF00E5FF)
            )
        ),
        EnvironmentPreset(
            id = "gradient_purple",
            name = "Neon Purple",
            description = "Purple gradient background",
            previewColor = ComposeColor(0xFF1A0A2E),
            gradientColors = listOf(
                ComposeColor(0xFF1A0A2E),
                ComposeColor(0xFF7C4DFF)
            )
        )
    )

    fun getPresetById(id: String): EnvironmentPreset? {
        return defaultEnvironments.find { it.id == id }
    }

    fun getAllPresets(): List<EnvironmentPreset> = defaultEnvironments
}
