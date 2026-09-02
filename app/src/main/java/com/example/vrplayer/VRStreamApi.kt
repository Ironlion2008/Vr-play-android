package com.example.vrplayer

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class PlaybackResponse(
    val ok: Boolean,
    val mediaId: String,
    val name: String?,
    val quality: String?,
    val position: Double,
    val transport: String?,
    val playerUrl: String?
)

class VRStreamApi(
    private val baseUrl: String
) {

    companion object {
        private const val TAG = "VRPlayer"
    }

    private val client =
        OkHttpClient.Builder()
            .connectTimeout(
                5,
                TimeUnit.SECONDS
            )
            .readTimeout(
                10,
                TimeUnit.SECONDS
            )
            .build()

    /*
     * --------------------------------------------------
     * GET /api/media
     * --------------------------------------------------
     */

    suspend fun getMedia(): List<VRMedia> =
        withContext(Dispatchers.IO) {

            val url =
                "$baseUrl/api/media"

            Log.d(
                TAG,
                "GET $url"
            )

            val request =
                Request.Builder()
                    .url(url)
                    .get()
                    .header(
                        "Accept",
                        "application/json"
                    )
                    .build()

            client
                .newCall(request)
                .execute()
                .use { response ->

                    if (!response.isSuccessful) {

                        throw IllegalStateException(
                            "Media API HTTP ${response.code}"
                        )
                    }

                    val body =
                        response.body?.string()
                            ?: throw IllegalStateException(
                                "Empty media API response"
                            )

                    Log.d(
                        TAG,
                        "Media API response received"
                    )

                    parseMediaList(body)
                }
        }

    /*
     * --------------------------------------------------
     * POST /api/media/{id}/play
     * --------------------------------------------------
     *
     * Requests VRStream to start/switch playback for
     * the selected media.
     */

    suspend fun playMedia(
        mediaId: String,
        quality: String? = null,
        position: Double = 0.0,
        transport: String = "dash"
    ): PlaybackResponse =
        withContext(Dispatchers.IO) {

            val url =
                "$baseUrl/api/media/$mediaId/play"

            Log.d(
                TAG,
                "POST $url"
            )

            val json =
                JSONObject().apply {

                    if (quality != null) {
                        put(
                            "quality",
                            quality
                        )
                    }

                    put(
                        "position",
                        position
                    )

                    put(
                        "transport",
                        transport
                    )
                }

            val body =
                json.toString()
                    .toRequestBody(
                        "application/json".toMediaType()
                    )

            val request =
                Request.Builder()
                    .url(url)
                    .post(body)
                    .header(
                        "Accept",
                        "application/json"
                    )
                    .build()

            client
                .newCall(request)
                .execute()
                .use { response ->

                    if (!response.isSuccessful) {

                        val errorBody =
                            response.body?.string()

                        throw IllegalStateException(
                            "Play API HTTP ${response.code}: " +
                                    (errorBody ?: "unknown error")
                        )
                    }

                    val responseBody =
                        response.body?.string()
                            ?: throw IllegalStateException(
                                "Empty play response"
                            )

                    Log.d(
                        TAG,
                        "Play response: $responseBody"
                    )

                    parsePlaybackResponse(
                        responseBody
                    )
                }
        }

    /*
     * --------------------------------------------------
     * GET /api/media/{id}/stream
     * --------------------------------------------------
     */

    suspend fun getStreamInfo(
        mediaId: String
    ): VRStreamInfo =
        withContext(Dispatchers.IO) {

            val url =
                "$baseUrl/api/media/$mediaId/stream"

            Log.d(
                TAG,
                "GET $url"
            )

            val request =
                Request.Builder()
                    .url(url)
                    .get()
                    .header(
                        "Accept",
                        "application/json"
                    )
                    .build()

            client
                .newCall(request)
                .execute()
                .use { response ->

                    if (!response.isSuccessful) {

                        throw IllegalStateException(
                            "Stream API HTTP ${response.code}"
                        )
                    }

                    val body =
                        response.body?.string()
                            ?: throw IllegalStateException(
                                "Empty stream response"
                            )

                    parseStreamInfo(body)
                }
        }

    /*
     * --------------------------------------------------
     * DASH manifest URL helper
     * --------------------------------------------------
     */

    fun getDashManifestUrl(): String {
        return "$baseUrl/dash/manifest.mpd"
    }

    suspend fun getQualityProfiles(mediaId: String): List<QualityOption> =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("$baseUrl/api/media/$mediaId/quality-profiles")
                .get()
                .header("Accept", "application/json")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("Quality API HTTP ${response.code}")
                }
                val root = JSONObject(response.body?.string() ?: "{}")
                val array = root.optJSONArray("profiles") ?: return@withContext emptyList()
                buildList {
                    for (i in 0 until array.length()) {
                        val item = array.optJSONObject(i) ?: continue
                        val id = item.optString("id")
                        if (id.isNotBlank()) {
                            add(QualityOption(id, item.optString("label", id)))
                        }
                    }
                }
            }
        }

    suspend fun setQuality(
        mediaId: String,
        quality: String,
        position: Double
    ): PlaybackResponse = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("quality", quality)
            put("position", position)
        }.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$baseUrl/api/media/$mediaId/quality")
            .post(body)
            .header("Accept", "application/json")
            .build()
        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string() ?: "{}"
            if (!response.isSuccessful) {
                throw IllegalStateException("Quality API HTTP ${response.code}: $responseBody")
            }
            parsePlaybackResponse(responseBody)
        }
    }

    /*
     * --------------------------------------------------
     * Parse media library
     * --------------------------------------------------
     */

    private fun parseMediaList(
        body: String
    ): List<VRMedia> {

        val root =
            JSONObject(body)

        val array =
            root.optJSONArray("media")
                ?: return emptyList()

        val result =
            mutableListOf<VRMedia>()

        for (i in 0 until array.length()) {

            val item =
                array.optJSONObject(i)
                    ?: continue

            val metadata =
                item.optJSONObject(
                    "metadata"
                )

            val vr =
                item.optJSONObject(
                    "vr"
                )

            val playback =
                item.optJSONObject(
                    "playback"
                )

            val thumbnails =
                item.optJSONObject(
                    "thumbnails"
                )

            val mediaId =
                item.optString("id")

            if (mediaId.isBlank()) {
                continue
            }

            result += VRMedia(

                mediaId =
                    mediaId,

                name =
                    item.optString(
                        "name",
                        mediaId
                    ),

                filename =
                    item.optString(
                        "filename",
                        ""
                    ),

                width =
                    metadata?.optIntOrNull(
                        "width"
                    ),

                height =
                    metadata?.optIntOrNull(
                        "height"
                    ),

                fps =
                    metadata?.optDoubleOrNull(
                        "fps"
                    ),

                codec =
                    metadata?.optStringOrNull(
                        "video_codec"
                    ),

                audioCodec =
                    metadata?.optStringOrNull(
                        "audio_codec"
                    ),

                durationSeconds =
                    metadata?.optDoubleOrNull(
                        "duration_seconds"
                    ),

                bitrate =
                    metadata?.optLongOrNull(
                        "bitrate"
                    ),

                isVr =
                    vr?.optBooleanOrNull(
                        "is_vr"
                    ),

                projection =
                    vr?.optStringOrNull(
                        "projection"
                    ),

                stereo =
                    vr?.optStringOrNull(
                        "stereo"
                    ),

                thumbnailUrl =
                    thumbnails?.optStringOrNull(
                        "android"
                    ),

                lastQuality =
                    playback?.optStringOrNull(
                        "last_quality"
                    ),

                lastPosition =
                    playback?.optDoubleOrNull(
                        "last_position"
                    )
            )
        }

        return result
    }

    /*
     * --------------------------------------------------
     * Parse stream information
     * --------------------------------------------------
     */

    private fun parseStreamInfo(
        body: String
    ): VRStreamInfo {

        val root =
            JSONObject(body)

        return VRStreamInfo(

            mediaId =
                root.optString(
                    "media_id"
                ),

            running =
                root.optBoolean(
                    "running",
                    false
                ),

            streamName =
                root.optStringOrNull(
                    "stream_name"
                ),

            whepUrl =
                root.optStringOrNull(
                    "whep_url"
                ),

            hlsUrl =
                root.optStringOrNull(
                    "hls_url"
                ),

            dashUrl =
                root.optStringOrNull(
                    "dash_url"
                ),

            quality =
                root.optStringOrNull(
                    "quality"
                )
        )
    }

    /*
     * --------------------------------------------------
     * Parse playback response
     * --------------------------------------------------
     */

    private fun parsePlaybackResponse(
        body: String
    ): PlaybackResponse {

        val root =
            JSONObject(body)

        return PlaybackResponse(

            ok =
                root.optBoolean(
                    "ok",
                    false
                ),

            mediaId =
                root.optString(
                    "media_id"
                ),

            name =
                root.optStringOrNull(
                    "name"
                ),

            quality =
                root.optStringOrNull(
                    "quality"
                ),

            position =
                root.optDouble(
                    "position",
                    0.0
                ),

            transport =
                root.optStringOrNull(
                    "transport"
                ),

            playerUrl =
                root.optStringOrNull(
                    "player_url"
                )
        )
    }

    /*
     * --------------------------------------------------
     * JSON helpers
     * --------------------------------------------------
     */

    private fun JSONObject.optStringOrNull(
        name: String
    ): String? {

        if (
            !has(name) ||
            isNull(name)
        ) {
            return null
        }

        return optString(name)
            .takeIf {
                it.isNotBlank()
            }
    }

    private fun JSONObject.optIntOrNull(
        name: String
    ): Int? {

        if (
            !has(name) ||
            isNull(name)
        ) {
            return null
        }

        return optInt(name)
    }

    private fun JSONObject.optLongOrNull(
        name: String
    ): Long? {

        if (
            !has(name) ||
            isNull(name)
        ) {
            return null
        }

        return optLong(name)
    }

    private fun JSONObject.optDoubleOrNull(
        name: String
    ): Double? {

        if (
            !has(name) ||
            isNull(name)
        ) {
            return null
        }

        return optDouble(name)
    }

    private fun JSONObject.optBooleanOrNull(
        name: String
    ): Boolean? {

        if (
            !has(name) ||
            isNull(name)
        ) {
            return null
        }

        return optBoolean(name)
    }
}