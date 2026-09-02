package com.example.vrplayer.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "vr_player_prefs")

data class PlayerSettings(
    val activeEnvironment: String = "default_dark",
    val projectionMode: ProjectionMode = ProjectionMode.FLAT,
    val stereoMode: StereoMode = StereoMode.MONO,
    val defaultBitrate: Int = 0,
    val autoPlay: Boolean = true
)

enum class ProjectionMode(val displayName: String) {
    FLAT("2D Flat"),
    SPHERICAL_360("360° Spherical"),
    SPHERICAL_180("180° Spherical")
}

enum class StereoMode(val displayName: String) {
    MONO("Mono"),
    SBS("Side-by-Side 3D"),
    TAB("Top-and-Bottom 3D")
}

class PlayerPreferencesManager(private val context: Context) {

    companion object {
        val ACTIVE_ENVIRONMENT = stringPreferencesKey("active_environment")
        val PROJECTION_MODE = stringPreferencesKey("projection_mode")
        val STEREO_MODE = stringPreferencesKey("stereo_mode")
        val DEFAULT_BITRATE = stringPreferencesKey("default_bitrate")
        val AUTO_PLAY = stringPreferencesKey("auto_play")
    }

    val settings: Flow<PlayerSettings> = context.dataStore.data.map { prefs ->
        PlayerSettings(
            activeEnvironment = prefs[ACTIVE_ENVIRONMENT] ?: "default_dark",
            projectionMode = ProjectionMode.valueOf(prefs[PROJECTION_MODE] ?: ProjectionMode.FLAT.name),
            stereoMode = StereoMode.valueOf(prefs[STEREO_MODE] ?: StereoMode.MONO.name),
            defaultBitrate = prefs[DEFAULT_BITRATE]?.toIntOrNull() ?: 0,
            autoPlay = prefs[AUTO_PLAY]?.toBoolean() ?: true
        )
    }

    suspend fun setActiveEnvironment(environment: String) {
        context.dataStore.edit { prefs ->
            prefs[ACTIVE_ENVIRONMENT] = environment
        }
    }

    suspend fun setProjectionMode(mode: ProjectionMode) {
        context.dataStore.edit { prefs ->
            prefs[PROJECTION_MODE] = mode.name
        }
    }

    suspend fun setStereoMode(mode: StereoMode) {
        context.dataStore.edit { prefs ->
            prefs[STEREO_MODE] = mode.name
        }
    }

    suspend fun setDefaultBitrate(bitrate: Int) {
        context.dataStore.edit { prefs ->
            prefs[DEFAULT_BITRATE] = bitrate.toString()
        }
    }

    suspend fun setAutoPlay(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[AUTO_PLAY] = enabled.toString()
        }
    }
}
