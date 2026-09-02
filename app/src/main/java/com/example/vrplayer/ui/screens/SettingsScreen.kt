package com.example.vrplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.vrplayer.ui.components.EnvironmentCard
import com.example.vrplayer.ui.theme.*

@Composable
fun EnvironmentScreen(
    selectedEnvironment: String,
    onEnvironmentSelect: (String) -> Unit,
    onCustomImageSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val presets = com.example.vrplayer.data.EnvironmentPresets.getAllPresets()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Player Environment",
                style = MaterialTheme.typography.headlineSmall,
                color = VRTextPrimary
            )

            Button(
                onClick = onCustomImageSelect,
                colors = ButtonDefaults.buttonColors(containerColor = VRSecondaryDark)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Custom PNG")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Choose a background environment for the player",
            style = MaterialTheme.typography.bodyMedium,
            color = VRTextSecondary
        )

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(presets.chunked(2)) { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowItems.forEach { preset ->
                        EnvironmentCard(
                            preset = preset,
                            isSelected = preset.id == selectedEnvironment,
                            onSelect = { onEnvironmentSelect(preset.id) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    projectionMode: String,
    stereoMode: String,
    onProjectionModeChange: (String) -> Unit,
    onStereoModeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Player Settings",
            style = MaterialTheme.typography.headlineSmall,
            color = VRTextPrimary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Projection Mode Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = VRCardBackground)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Projection Mode",
                    style = MaterialTheme.typography.titleMedium,
                    color = VRTextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Choose how video content is projected",
                    style = MaterialTheme.typography.bodySmall,
                    color = VRTextSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProjectionModeButton(
                        label = "2D Flat",
                        isSelected = projectionMode == "FLAT",
                        onClick = { onProjectionModeChange("FLAT") },
                        modifier = Modifier.weight(1f)
                    )
                    ProjectionModeButton(
                        label = "180°",
                        isSelected = projectionMode == "SPHERICAL_180",
                        onClick = { onProjectionModeChange("SPHERICAL_180") },
                        modifier = Modifier.weight(1f)
                    )
                    ProjectionModeButton(
                        label = "360°",
                        isSelected = projectionMode == "SPHERICAL_360",
                        onClick = { onProjectionModeChange("SPHERICAL_360") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Stereo Mode Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = VRCardBackground)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Stereo Mode",
                    style = MaterialTheme.typography.titleMedium,
                    color = VRTextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Select viewing mode for 3D content",
                    style = MaterialTheme.typography.bodySmall,
                    color = VRTextSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))

                StereoModeOption(
                    label = "Mono",
                    description = "Standard 2D video",
                    isSelected = stereoMode == "MONO",
                    onClick = { onStereoModeChange("MONO") }
                )

                Spacer(modifier = Modifier.height(8.dp))

                StereoModeOption(
                    label = "Side-by-Side 3D",
                    description = "For SBS 3D content",
                    isSelected = stereoMode == "SBS",
                    onClick = { onStereoModeChange("SBS") }
                )

                Spacer(modifier = Modifier.height(8.dp))

                StereoModeOption(
                    label = "Top-and-Bottom 3D",
                    description = "For Over-Under 3D content",
                    isSelected = stereoMode == "TAB",
                    onClick = { onStereoModeChange("TAB") }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // App Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = VRCardBackground)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "About",
                    style = MaterialTheme.typography.titleMedium,
                    color = VRTextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "VR Player v1.0",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VRTextSecondary
                )
                Text(
                    text = "Stream VR content from local network devices",
                    style = MaterialTheme.typography.bodySmall,
                    color = VRTextSecondary
                )
            }
        }
    }
}

@Composable
private fun ProjectionModeButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) VRPrimaryDark else VRSurfaceDark,
            contentColor = if (isSelected) VRBackgroundDark else VRTextPrimary
        )
    ) {
        Text(label)
    }
}

@Composable
private fun StereoModeOption(
    label: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) VRPrimaryDark.copy(alpha = 0.1f) else androidx.compose.ui.graphics.Color.Transparent
            )
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = VRTextPrimary
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = VRTextSecondary
            )
        }

        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = VRPrimaryDark,
                unselectedColor = VRTextSecondary
            )
        )
    }
}
