package com.example.vrplayer

data class QualityOption(
    val id: String,
    val label: String
)

data class VRStreamServer(
    val host: String,
    val port: Int,
    val baseUrl: String
)

data class VRMedia(
    val mediaId: String,
    val name: String,
    val filename: String,

    val width: Int?,
    val height: Int?,
    val fps: Double?,
    val codec: String?,
    val audioCodec: String?,
    val durationSeconds: Double?,
    val bitrate: Long?,

    val isVr: Boolean?,
    val projection: String?,
    val stereo: String?,

    val thumbnailUrl: String?,

    val lastQuality: String?,
    val lastPosition: Double?
)

data class VRStreamInfo(
    val mediaId: String,
    val running: Boolean,
    val streamName: String?,
    val whepUrl: String?,
    val hlsUrl: String?,
    val dashUrl: String?,
    val quality: String?
)
