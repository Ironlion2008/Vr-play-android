package com.example.vrplayer

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.dash.DefaultDashChunkSource
import androidx.media3.exoplayer.dash.manifest.DashManifestParser
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.util.EventLogger


@UnstableApi
class DashPlayerFactory(
    private val context: Context,
    private val onError: (String) -> Unit
) {

    companion object {
        private const val TAG = "VRPlayer.DASH"

        /*
         * Additional tag used for Media3 internal logging
         * toggled on in create(). Kept separate so the
         * playback-state log stream is not drowned out.
         */
        private const val TAG_INTERNAL = "VRPlayer.Media3"

        init {
            enableMedia3InternalLogging()
        }

        private fun enableMedia3InternalLogging() {
            androidx.media3.common.util.Log.setLogLevel(
                androidx.media3.common.util.Log.LOG_LEVEL_ALL
            )
            androidx.media3.common.util.Log.setLogger(
                object : androidx.media3.common.util.Log.Logger {
                    override fun d(
                        tag: String,
                        message: String,
                        tr: Throwable?
                    ) {
                        if (tr == null) {
                            Log.d(TAG_INTERNAL, "[$tag] $message")
                        } else {
                            Log.d(TAG_INTERNAL, "[$tag] $message", tr)
                        }
                    }

                    override fun i(
                        tag: String,
                        message: String,
                        tr: Throwable?
                    ) {
                        if (tr == null) {
                            Log.i(TAG_INTERNAL, "[$tag] $message")
                        } else {
                            Log.i(TAG_INTERNAL, "[$tag] $message", tr)
                        }
                    }

                    override fun w(
                        tag: String,
                        message: String,
                        tr: Throwable?
                    ) {
                        if (tr == null) {
                            Log.w(TAG_INTERNAL, "[$tag] $message")
                        } else {
                            Log.w(TAG_INTERNAL, "[$tag] $message", tr)
                        }
                    }

                    override fun e(
                        tag: String,
                        message: String,
                        tr: Throwable?
                    ) {
                        if (tr == null) {
                            Log.e(TAG_INTERNAL, "[$tag] $message")
                        } else {
                            Log.e(TAG_INTERNAL, "[$tag] $message", tr)
                        }
                    }
                }
            )
        }
    }

    fun create(
        manifestUrl: String
    ): ExoPlayer {

        Log.d(
            TAG,
            "Creating ExoPlayer for dynamic DASH: $manifestUrl"
        )

        val httpFactory =
            DefaultHttpDataSource.Factory()
                .setUserAgent("VRPlayer/1.0 (Media3-ExoPlayer-DASH)")
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(5_000)
                .setReadTimeoutMs(10_000)
                .setDefaultRequestProperties(
                    mapOf(
                        "Accept" to
                            "application/dash+xml," +
                                    "application/octet-stream,*/*"
                    )
                )

        val instrumentedHttpFactory =
            DataSource.Factory {
                val source = httpFactory.createDataSource()
                InstrumentedHttpDataSource(source)
            }

        val loadControl =
            DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    6_000,
                    20_000,
                    2_000,
                    2_500
                )
                .build()

        val mediaSourceFactory =
            DefaultMediaSourceFactory(context)
                .setDataSourceFactory(instrumentedHttpFactory)

        val player =
            ExoPlayer.Builder(context)
                .setMediaSourceFactory(mediaSourceFactory)
                .setLoadControl(loadControl)
                .build()

        /*
         * ---------------------------------------------------------
         * DefaultEventLogger — emits every load event,
         * manifest load, chunk load, init segment, media
         * segment, format, and playback start/end automatically.
         * ---------------------------------------------------------
         */
        player.addAnalyticsListener(
            EventLogger(
                "VRPlayer.EventLogger"
            )
        )

        player.addListener(
            object : Player.Listener {

                override fun onPlayerError(
                    error: PlaybackException
                ) {

                    val cause =
                        error.cause
                            ?.let { c ->
                                "${c.javaClass.simpleName}: ${c.message}"
                            }
                            ?: "no cause"

                    val message =
                        "ExoPlayer error " +
                                "code=${error.errorCode} " +
                                "name=${error.errorCodeName} " +
                                "msg=${error.message} " +
                                "cause=$cause"

                    Log.e(
                        TAG,
                        message
                    )

                    onError(
                        "Playback error " +
                                "${error.errorCodeName} " +
                                "(${error.errorCode}): " +
                                (error.message ?: "unknown")
                    )
                }

                override fun onPlaybackStateChanged(
                    state: Int
                ) {

                    val stateName =
                        when (state) {

                            Player.STATE_IDLE ->
                                "IDLE"

                            Player.STATE_BUFFERING ->
                                "BUFFERING"

                            Player.STATE_READY ->
                                "READY"

                            Player.STATE_ENDED ->
                                "ENDED"

                            else ->
                                "UNKNOWN($state)"
                        }

                    val isLoading = player.isLoading

                    Log.d(
                        TAG,
                        "Playback state: $stateName " +
                                "(isLoading=$isLoading, " +
                                "playWhenReady=${player.playWhenReady}, " +
                                "playbackState=$state)"
                    )
                }

                override fun onIsLoadingChanged(
                    isLoading: Boolean
                ) {
                    Log.d(
                        TAG,
                        "onIsLoadingChanged: isLoading=$isLoading " +
                                "(state=${stateName(player.playbackState)})"
                    )
                }

                override fun onTimelineChanged(
                    timeline: Timeline,
                    reason: Int
                ) {

                    val reasonName =
                        when (reason) {
                            Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED ->
                                "PLAYLIST_CHANGED"
                            Player.TIMELINE_CHANGE_REASON_SOURCE_UPDATE ->
                                "SOURCE_UPDATE"
                            else ->
                                "INIT($reason)"
                        }

                    Log.d(
                        TAG,
                        "onTimelineChanged reason=$reasonName " +
                                "windowCount=${timeline.windowCount} " +
                                "periodCount=${timeline.periodCount}"
                    )

                    val window = Timeline.Window()

                    var anyDynamic = false

                    for (i in 0 until timeline.windowCount) {

                        timeline.getWindow(
                            i,
                            window
                        )

                        if (window.isDynamic) {
                            anyDynamic = true
                        }

                        val posUs =
                            window.positionInFirstPeriodUs
                        val durUs =
                            window.durationUs
                        val startPosMs =
                            window.positionInFirstPeriodUs / 1_000
                        val durSec =
                            if (window.durationUs == androidx.media3.common.C.TIME_UNSET) {
                                "UNSET"
                            } else {
                                "${window.durationUs / 1_000_000.0}s"
                            }

                        val liveOffsetUs =
                            if (window
                                    .positionInFirstPeriodUs
                                > 0
                            ) {
                                "liveOffset=${window.positionInFirstPeriodUs / 1_000}ms"
                            } else {
                                "liveOffset=n/a"
                            }

                        Log.d(
                            TAG,
                            "  window[$i]: " +
                                    "isLive=${window.isLive()} " +
                                    "isDynamic=${window.isDynamic} " +
                                    "isSeekable=${window.isSeekable} " +
                                    "positionInFirstPeriodUs=$posUs " +
                                    "durationUs=$durUs " +
                                    "startPosMs=$startPosMs " +
                                    "durSec=$durSec " +
                                    "$liveOffsetUs"
                        )
                    }

                    Log.d(
                        TAG,
                        "  timeline.isDynamic (any window) =$anyDynamic"
                    )

                    val currentIdx =
                        player.currentMediaItemIndex

                    val cur = Timeline.Window()
                    timeline.getWindow(
                        currentIdx,
                        cur
                    )

                    Log.d(
                        TAG,
                        "  current media index=$currentIdx " +
                                "currentPosMs=${player.currentPosition} " +
                                "bufferedPosMs=${player.bufferedPosition} " +
                                "totalBufferedMs=${player.totalBufferedDuration}"
                    )
                }

                override fun onTracksChanged(
                    tracks: androidx.media3.common.Tracks
                ) {

                    val groupCount = tracks.groups.size
                    val trackCount =
                        tracks.groups.sumOf { it.length }

                    Log.d(
                        TAG,
                        "onTracksChanged groups=$groupCount " +
                                "tracks=$trackCount"
                    )

                    for (i in 0 until tracks.groups.size) {

                        val g = tracks.groups[i]
                        val tg = g.mediaTrackGroup
                        val firstFormat =
                            if (tg.length > 0)
                                tg.getFormat(0)
                            else
                                null

                        val mime =
                            firstFormat?.sampleMimeType
                                ?: "unknown"

                        val width =
                            firstFormat?.width
                            ?: 0
                        val height =
                            firstFormat?.height
                            ?: 0
                        val bitrate =
                            firstFormat?.bitrate
                            ?: 0

                        val selected =
                            (0 until g.length).count {
                                g.isTrackSelected(it)
                            }

                        Log.d(
                            TAG,
                            "  trackGroup[$i]: " +
                                    "mime=$mime " +
                                    "${width}x${height} " +
                                    "bitrate=$bitrate " +
                                    "selected=$selected/${g.length}"
                        )
                    }
                }

                override fun onRenderedFirstFrame() {
                    Log.d(
                        TAG,
                        "onRenderedFirstFrame " +
                                "(posMs=${player.currentPosition})"
                    )
                }

                override fun onVideoSizeChanged(
                    videoSize: androidx.media3.common.VideoSize
                ) {
                    Log.d(
                        TAG,
                        "onVideoSizeChanged " +
                                "${videoSize.width}x${videoSize.height} " +
                                "rotDeg=${videoSize.unappliedRotationDegrees} " +
                                "par=${videoSize.pixelWidthHeightRatio}"
                    )
                }

                override fun onPositionDiscontinuity(
                    oldPosition: Player.PositionInfo,
                    newPosition: Player.PositionInfo,
                    reason: Int
                ) {
                    val reasonName =
                        when (reason) {
                            Player.DISCONTINUITY_REASON_AUTO_TRANSITION ->
                                "AUTO_TRANSITION"
                            Player.DISCONTINUITY_REASON_SEEK ->
                                "SEEK"
                            Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT ->
                                "SEEK_ADJUSTMENT"
                            Player.DISCONTINUITY_REASON_REMOVE ->
                                "REMOVE"
                            Player.DISCONTINUITY_REASON_INTERNAL ->
                                "INTERNAL"
                            else ->
                                "UNKNOWN($reason)"
                        }
                    Log.d(
                        TAG,
                        "onPositionDiscontinuity reason=$reasonName " +
                                "oldPosMs=${oldPosition.positionMs} " +
                                "newPosMs=${newPosition.positionMs} " +
                                "oldMedia=${oldPosition.mediaItem?.mediaId} " +
                                "newMedia=${newPosition.mediaItem?.mediaId}"
                    )
                }
            }
        )

        val dashSource: MediaSource =
            DashMediaSource.Factory(
                DefaultDashChunkSource.Factory(
                    instrumentedHttpFactory
                ),
                instrumentedHttpFactory
            )
                .setManifestParser(
                    DashManifestParser()
                )
                .createMediaSource(
                    MediaItem.Builder()
                        .setUri(manifestUrl)
                        .setMimeType(MimeTypes.APPLICATION_MPD)
                        .setLiveConfiguration(
                            MediaItem.LiveConfiguration.Builder()
                                .build()
                        )
                        .build()
                )

        player.setMediaSource(dashSource)

        player.prepare()

        return player
    }

    private fun stateName(state: Int): String = when (state) {
        Player.STATE_IDLE -> "IDLE"
        Player.STATE_BUFFERING -> "BUFFERING"
        Player.STATE_READY -> "READY"
        Player.STATE_ENDED -> "ENDED"
        else -> "UNKNOWN($state)"
    }

    /*
     * ---------------------------------------------------------
     * InstrumentedHttpDataSource — wraps a DefaultHttpDataSource
     * and logs every open / read / close for diagnostic purposes
     * only. Behaviour is unchanged.
     * ---------------------------------------------------------
     */
    private class InstrumentedHttpDataSource(
        private val delegate: DefaultHttpDataSource
    ) : androidx.media3.datasource.HttpDataSource {

        private var lastUri: String? = null
        private var openStartMs: Long = 0L
        private var bytesRead: Long = 0L

        private fun contentType(): String? {
            val headers = delegate.responseHeaders
            for ((k, v) in headers) {
                if (k.equals("Content-Type", ignoreCase = true)) {
                    return v.firstOrNull()
                }
            }
            return null
        }

        private fun contentLength(): String? {
            val headers = delegate.responseHeaders
            for ((k, v) in headers) {
                if (k.equals("Content-Length", ignoreCase = true)) {
                    return v.firstOrNull()
                }
            }
            return null
        }

        override fun open(dataSpec: DataSpec): Long {
            val uri = dataSpec.uri.toString()
            lastUri = uri
            openStartMs = System.currentTimeMillis()
            bytesRead = 0L

            Log.d(
                TAG,
                "HTTP open BEGIN uri=$uri " +
                        "pos=${dataSpec.position} " +
                        "length=${dataSpec.length}"
            )

            return try {

                val remaining = delegate.open(dataSpec)

                Log.d(
                    TAG,
                    "HTTP open OK uri=$uri " +
                            "code=${delegate.responseCode} " +
                            "contentType=${contentType()} " +
                            "contentLength=${contentLength()} " +
                            "remaining=$remaining " +
                            "tookMs=${System.currentTimeMillis() - openStartMs}"
                )

                remaining

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "HTTP open FAILED uri=$uri " +
                            "code=${delegate.responseCode} " +
                            "contentType=${contentType()} " +
                            "err=${e.javaClass.simpleName}: ${e.message}",
                    e
                )

                throw e
            }
        }

        override fun read(
            buffer: ByteArray,
            offset: Int,
            length: Int
        ): Int {
            val n = try {
                delegate.read(buffer, offset, length)
            } catch (e: Exception) {
                Log.e(
                    TAG,
                    "HTTP read FAILED uri=${lastUri} " +
                            "err=${e.javaClass.simpleName}: ${e.message}",
                    e
                )
                throw e
            }
            if (n > 0) {
                bytesRead += n
            }
            return n
        }

        override fun getUri(): Uri? = delegate.uri

        override fun getResponseCode(): Int =
            delegate.responseCode

        override fun getResponseHeaders(): Map<String, List<String>> =
            delegate.responseHeaders

        override fun close() {
            try {
                delegate.close()
            } catch (e: Exception) {
                Log.w(
                    TAG,
                    "HTTP close error uri=${lastUri} " +
                            "err=${e.javaClass.simpleName}: ${e.message}"
                )
            } finally {

                val took = System.currentTimeMillis() - openStartMs

                Log.d(
                    TAG,
                    "HTTP close uri=${lastUri} " +
                            "bytesRead=$bytesRead " +
                            "tookMs=$took"
                )
            }
        }

        override fun setRequestProperty(
            name: String,
            value: String
        ) {
            delegate.setRequestProperty(name, value)
        }

        override fun clearRequestProperty(name: String) {
            delegate.clearRequestProperty(name)
        }

        override fun clearAllRequestProperties() {
            delegate.clearAllRequestProperties()
        }

        override fun addTransferListener(
            transferListener: androidx.media3.datasource.TransferListener
        ) {
            delegate.addTransferListener(transferListener)
        }
    }
}
