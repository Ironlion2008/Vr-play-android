package com.example.vrplayer

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.vrplayer.ui.theme.VRPlayerTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request

class MainActivity : ComponentActivity() {

    private val discovery = VRStreamDiscovery()
    private var api: VRStreamApi? = null
    private var exoPlayer: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            VRPlayerTheme {
                VRPlayerApp()
            }
        }
    }

    /*
     * =========================================================
     * MAIN APP
     * =========================================================
     */

    @Composable
    private fun VRPlayerApp() {

        var screen by remember { mutableStateOf(AppScreen.HOME) }
        var status by remember { mutableStateOf("Ready") }
        var server by remember { mutableStateOf<VRStreamServer?>(null) }
        var mediaList by remember { mutableStateOf<List<VRMedia>>(emptyList()) }
        var selectedMedia by remember { mutableStateOf<VRMedia?>(null) }
        var isDiscovering by remember { mutableStateOf(false) }
        var player by remember { mutableStateOf<ExoPlayer?>(null) }

        BackHandler(enabled = screen != AppScreen.HOME) {
            when (screen) {
                AppScreen.VRSTREAM -> screen = AppScreen.HOME
                AppScreen.MEDIA_LIBRARY -> screen = AppScreen.VRSTREAM
                AppScreen.PLAYER ->
                    stopPlayback(
                        onStopped = {
                            screen = AppScreen.MEDIA_LIBRARY
                            selectedMedia = null
                        }
                    )
                AppScreen.HOME -> Unit
            }
        }

        when (screen) {
            AppScreen.HOME -> {
                HomeScreen(
                    onVrStream = { screen = AppScreen.VRSTREAM },
                    onWebHash = { status = "WebHash Server is coming next." },
                    onLocalFiles = { status = "Local Files is coming next." },
                    status = status
                )
            }

            AppScreen.VRSTREAM -> {
                VrStreamServerScreen(
                    server = server,
                    isDiscovering = isDiscovering,
                    status = status,
                    onBack = { screen = AppScreen.HOME },
                    onScan = {
                        isDiscovering = true
                        status = "Searching local network..."

                        CoroutineScope(Dispatchers.Main).launch {
                            try {
                                val localIp = getLocalIpv4Address()
                                if (localIp == null) {
                                    status = "Could not determine phone IP"
                                    return@launch
                                }

                                Log.d("VRPlayer", "Phone LAN IP: $localIp")
                                val discovered = discovery.discover(localIp)

                                if (discovered == null) {
                                    server = null
                                    mediaList = emptyList()
                                    status = "No VRStream server found"
                                    return@launch
                                }

                                server = discovered
                                Log.d("VRPlayer", "VRStream found at ${discovered.baseUrl}")
                                api = VRStreamApi(discovered.baseUrl)
                                status = "Loading media..."
                                mediaList = api!!.getMedia()
                                Log.d("VRPlayer", "Media count: ${mediaList.size}")
                                status = if (mediaList.isEmpty()) {
                                    "Server found — no media"
                                } else {
                                    "${mediaList.size} videos available"
                                }
                            } catch (e: Exception) {
                                Log.e("VRPlayer", "VRStream discovery failed", e)
                                server = null
                                mediaList = emptyList()
                                status = "Connection failed: ${e.message ?: "unknown error"}"
                            } finally {
                                isDiscovering = false
                            }
                        }
                    },
                    onOpenLibrary = {
                        if (server != null && mediaList.isNotEmpty()) {
                            screen = AppScreen.MEDIA_LIBRARY
                        }
                    }
                )
            }

            AppScreen.MEDIA_LIBRARY -> {
                MediaLibraryScreen(
                    server = server,
                    mediaList = mediaList,
                    onBack = { screen = AppScreen.VRSTREAM },
                    onRefresh = {
                        val currentServer = server ?: return@MediaLibraryScreen
                        CoroutineScope(Dispatchers.Main).launch {
                            try {
                                status = "Refreshing media..."
                                val refreshed = VRStreamApi(currentServer.baseUrl)
                                api = refreshed
                                mediaList = refreshed.getMedia()
                                status = "${mediaList.size} videos available"
                            } catch (e: Exception) {
                                Log.e("VRPlayer", "Media refresh failed", e)
                                status = "Refresh failed"
                            }
                        }
                    },
                    onMediaSelected = { media ->
                        selectedMedia = media
                        screen = AppScreen.PLAYER
                        playSelectedMedia(
                            media = media,
                            onStatus = { status = it },
                            onPlayerReady = { newPlayer -> player = newPlayer }
                        )
                    }
                )
            }

            AppScreen.PLAYER -> {
                PlayerScreen(
                    media = selectedMedia,
                    status = status,
                    player = player,
                    onQualityChanged = { quality ->
                        val currentMedia = selectedMedia
                        val currentPlayer = player
                        if (currentMedia == null || currentPlayer == null) {
                            status = "Player is not ready"
                        } else {
                            val currentPosition = currentPlayer.currentPosition / 1000.0
                            CoroutineScope(Dispatchers.Main).launch {
                                try {
                                    status = "Switching to $quality..."
                                    api?.setQuality(currentMedia.mediaId, quality, currentPosition)
                                    // The server creates a new isolated DASH run. Re-open its
                                    // manifest at the preserved position after it becomes ready.
                                    val deadline = System.currentTimeMillis() + 15_000L
                                    while (System.currentTimeMillis() < deadline) {
                                        if (api?.getStreamInfo(currentMedia.mediaId)?.running == true) break
                                        delay(250)
                                    }
                                    currentPlayer.stop()
                                    currentPlayer.clearMediaItems()
                                    currentPlayer.setMediaItem(
                                        androidx.media3.common.MediaItem.Builder()
                                            .setUri(api?.getDashManifestUrl() ?: return@launch)
                                            .setMimeType(androidx.media3.common.MimeTypes.APPLICATION_MPD)
                                            .build(),
                                        (currentPosition * 1000).toLong()
                                    )
                                    currentPlayer.prepare()
                                    currentPlayer.playWhenReady = true
                                    status = "Playing • $quality"
                                } catch (e: Exception) {
                                    Log.e("VRPlayer", "Quality switch failed", e)
                                    status = "Quality switch failed: ${e.message ?: "unknown error"}"
                                }
                            }
                        }
                    },
                    onBack = {
                        stopPlayback(
                            onStopped = {
                                screen = AppScreen.MEDIA_LIBRARY
                                selectedMedia = null
                                player = null
                            }
                        )
                    }
                )
            }
        }
    }

    /*
     * =========================================================
     * HOME
     * =========================================================
     */

    @Composable
    private fun HomeScreen(
        onVrStream: () -> Unit,
        onWebHash: () -> Unit,
        onLocalFiles: () -> Unit,
        status: String
    ) {
        Surface(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "VR PLAYER",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Choose a playback source",
                    modifier = Modifier.padding(top = 8.dp, bottom = 32.dp),
                    style = MaterialTheme.typography.bodyLarge
                )

                SourceCard(
                    title = "VRStream Server",
                    description = "Stream VR media from a VRStream server",
                    onClick = onVrStream
                )

                Spacer(modifier = Modifier.height(16.dp))

                SourceCard(
                    title = "WebHash Server",
                    description = "Browse and play web-based content",
                    onClick = onWebHash
                )

                Spacer(modifier = Modifier.height(16.dp))

                SourceCard(
                    title = "Local Files",
                    description = "Play videos stored on this device",
                    onClick = onLocalFiles
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    /*
     * =========================================================
     * VRSTREAM SERVER SCREEN
     * =========================================================
     */

    @Composable
    private fun VrStreamServerScreen(
        server: VRStreamServer?,
        isDiscovering: Boolean,
        status: String,
        onBack: () -> Unit,
        onScan: () -> Unit,
        onOpenLibrary: () -> Unit
    ) {
        Surface(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                ScreenHeader(
                    title = "VRStream Server",
                    onBack = onBack
                )

                Text(
                    text = "Find a VRStream server on your local network.",
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (isDiscovering) {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Searching local network...",
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = status,
                                    modifier = Modifier.padding(top = 4.dp),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                } else if (server != null) {
                    ServerCard(
                        server = server,
                        status = status,
                        onClick = onOpenLibrary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Tap the server to browse its media.",
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Text(
                                text = "No server connected",
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = status,
                                modifier = Modifier.padding(top = 6.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onScan,
                    enabled = !isDiscovering
                ) {
                    Text(
                        if (server == null) "Scan for VRStream" else "Scan Again"
                    )
                }
            }
        }
    }

    /*
     * =========================================================
     * MEDIA LIBRARY
     * =========================================================
     */

    @Composable
    private fun MediaLibraryScreen(
        server: VRStreamServer?,
        mediaList: List<VRMedia>,
        onBack: () -> Unit,
        onRefresh: () -> Unit,
        onMediaSelected: (VRMedia) -> Unit
    ) {
        Surface(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                ScreenHeader(
                    title = "VRStream Library",
                    onBack = onBack
                )

                if (server != null) {
                    Text(
                        text = server.baseUrl,
                        modifier = Modifier.padding(top = 4.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${mediaList.size} videos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    OutlinedButton(
                        onClick = onRefresh
                    ) {
                        Text("Refresh")
                    }
                }

                if (mediaList.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No media available"
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = mediaList,
                            key = { it.mediaId }
                        ) { media ->
                            MediaLibraryCard(
                                media = media,
                                onClick = { onMediaSelected(media) }
                            )
                        }
                    }
                }
            }
        }
    }

    /*
     * =========================================================
     * PLAYER
     * =========================================================
     */

    @Composable
    private fun PlayerScreen(
        media: VRMedia?,
        status: String,
        player: ExoPlayer?,
        onQualityChanged: (String) -> Unit,
        onBack: () -> Unit
    ) {
        var qualityMenuExpanded by remember { mutableStateOf(false) }
        var qualityOptions by remember { mutableStateOf<List<QualityOption>>(emptyList()) }

        // Load quality options when media changes
        androidx.compose.runtime.LaunchedEffect(media?.mediaId) {
            val id = media?.mediaId ?: return@LaunchedEffect
            try {
                qualityOptions = api?.getQualityProfiles(id) ?: emptyList()
            } catch (e: Exception) {
                Log.w("VRPlayer", "Could not load quality profiles", e)
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onBack
                    ) {
                        Text("Back")
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = media?.name ?: "Player",
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Box {
                        OutlinedButton(onClick = { qualityMenuExpanded = true }) {
                            Text("Quality")
                        }
                        DropdownMenu(
                            expanded = qualityMenuExpanded,
                            onDismissRequest = { qualityMenuExpanded = false }
                        ) {
                            qualityOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label) },
                                    onClick = {
                                        qualityMenuExpanded = false
                                        onQualityChanged(option.id)
                                    }
                                )
                            }
                        }
                    }
                }

                if (media != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = buildString {
                                media.width?.let { append(it) }
                                if (media.width != null && media.height != null) append("×")
                                media.height?.let { append(it) }
                                media.fps?.let { append(" • ${it.toInt()} FPS") }
                            }.ifBlank { "VR video" },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = status,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (player != null) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { context ->
                                PlayerView(context).apply {
                                    this.player = player
                                    useController = true
                                    controllerAutoShow = true
                                    controllerHideOnTouch = true
                                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING) // ✅ Fixed: Using setter method
                                    keepScreenOn = true
                                }
                            },
                            update = { view ->
                                view.player = player
                            }
                        )
                    } else {
                        CircularProgressIndicator()
                    }

                    if (status.isNotBlank()) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp),
                            shape = RoundedCornerShape(12.dp),
                            tonalElevation = 6.dp
                        ) {
                            Text(
                                text = status,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }

    /*
     * =========================================================
     * SOURCE CARD
     * =========================================================
     */

    @Composable
    private fun SourceCard(
        title: String,
        description: String,
        onClick: () -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() },
            shape = RoundedCornerShape(18.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(22.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    modifier = Modifier.padding(top = 6.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }

    /*
     * =========================================================
     * SERVER CARD
     * =========================================================
     */

    @Composable
    private fun ServerCard(
        server: VRStreamServer,
        status: String,
        onClick: () -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() },
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "VRStream",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${server.host}:${server.port}",
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = status,
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    /*
     * =========================================================
     * MEDIA CARD
     * =========================================================
     */

    @Composable
    private fun MediaLibraryCard(
        media: VRMedia,
        onClick: () -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() },
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = media.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                val resolution = if (media.width != null && media.height != null) {
                    "${media.width} × ${media.height}"
                } else {
                    "Unknown resolution"
                }

                val fps = media.fps?.let { "${it} FPS" } ?: "Unknown FPS"
                val codec = media.codec?.uppercase() ?: "Unknown codec"

                Text(
                    text = "$resolution • $fps • $codec",
                    modifier = Modifier.padding(top = 5.dp),
                    style = MaterialTheme.typography.bodySmall
                )

                if (media.isVr == true) {
                    Text(
                        text = buildString {
                            append("VR")
                            media.projection?.let {
                                append(" • ")
                                append(it)
                            }
                            media.stereo?.let {
                                append(" • ")
                                append(it)
                            }
                        },
                        modifier = Modifier.padding(top = 4.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }

    /*
     * =========================================================
     * HEADER
     * =========================================================
     */

    @Composable
    private fun ScreenHeader(
        title: String,
        onBack: () -> Unit
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onBack
            ) {
                Text("Back")
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }

    /*
     * =========================================================
     * PLAY SELECTED MEDIA
     * =========================================================
     */

    private fun playSelectedMedia(
        media: VRMedia,
        onStatus: (String) -> Unit,
        onPlayerReady: (ExoPlayer) -> Unit
    ) {
        val vrApi = api ?: run {
            onStatus("VRStream API not initialized")
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            try {
                onStatus("Requesting DASH playback...")

                val playback = vrApi.playMedia(
                    mediaId = media.mediaId,
                    quality = media.lastQuality,
                    position = media.lastPosition ?: 0.0,
                    transport = "dash"
                )

                Log.d("VRPlayer", "DASH playback requested: ${playback.mediaId} quality=${playback.quality}")

                onStatus("Waiting for DASH stream...")

                val deadline = System.currentTimeMillis() + 15_000L
                var ready = false

                while (System.currentTimeMillis() < deadline) {
                    val info = vrApi.getStreamInfo(media.mediaId)
                    Log.d("VRPlayer", "DASH stream running=${info.running}")

                    if (info.running) {
                        ready = true
                        break
                    }
                    delay(300)
                }

                if (!ready) {
                    onStatus("DASH stream did not become ready")
                    return@launch
                }

                val streamInfo = vrApi.getStreamInfo(media.mediaId)
                val manifestUrl = streamInfo.dashUrl ?: vrApi.getDashManifestUrl()

                Log.d("VRPlayer", "DASH manifest: $manifestUrl")

                onStatus("Preparing video...")

                exoPlayer?.release()

                val factory = DashPlayerFactory(
                    context = this@MainActivity,
                    onError = { message -> onStatus(message) }
                )

                val newPlayer = factory.create(manifestUrl)
                newPlayer.playWhenReady = true
                exoPlayer = newPlayer
                onPlayerReady(newPlayer)
                onStatus("Playing")

            } catch (e: Exception) {
                Log.e("VRPlayer", "DASH playback failed", e)
                onStatus("Playback failed: ${e.message ?: "unknown error"}")
            }
        }
    }

    /*
     * =========================================================
     * ONE-SHOT MPD PROBE (DIAGNOSTIC ONLY)
     * =========================================================
     *
     * Issues a single GET against the manifest URL at the
     * exact moment playback starts, prints the response
     * status, content-type, content-length and the first
     * 4 KiB of the body to logcat. Not used as the playback
     * source.
     */

    private fun probeManifestOnce(manifestUrl: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = Request.Builder()
                    .url(manifestUrl)
                    .get()
                    .header("Accept", "application/dash+xml,application/xml;q=0.9,*/*;q=0.8")
                    .build()

                val client = OkHttpClient.Builder()
                    .connectTimeout(5, TimeUnit.SECONDS)
                    .readTimeout(5, TimeUnit.SECONDS)
                    .build()

                client.newCall(request).execute().use { response ->
                    val code = response.code
                    val ct = response.header("Content-Type")
                    val cl = response.header("Content-Length")
                    val body = response.body

                    Log.d("VRPlayer.MPD", "PROBE GET $manifestUrl -> code=$code contentType=$ct contentLength=$cl")

                    if (!response.isSuccessful || body == null) {
                        Log.w("VRPlayer.MPD", "PROBE unsuccessful or empty body")
                        return@launch
                    }

                    val src = body.byteStream()
                    val head = ByteArray(4 * 1024)
                    val read = src.read(head)
                    val text = String(head, 0, maxOf(read, 0), Charsets.UTF_8)

                    Log.d("VRPlayer.MPD", "PROBE body (first 4 KiB, read=$read bytes):\n$text")
                }
            } catch (e: Exception) {
                Log.e("VRPlayer.MPD", "PROBE failed: ${e.javaClass.simpleName}: ${e.message ?: "unknown"}", e)
            }
        }
    }

    /*
     * =========================================================
     * STOP PLAYBACK
     * =========================================================
     */

    private fun stopPlayback(onStopped: () -> Unit) {
        exoPlayer?.release()
        exoPlayer = null
        onStopped()
    }

    /*
     * =========================================================
     * CLEANUP
     * =========================================================
     */

    override fun onDestroy() {
        Log.d("VRPlayer", "MainActivity destroying")
        exoPlayer?.release()
        exoPlayer = null
        api = null
        super.onDestroy()
    }

    /*
     * =========================================================
     * LOCAL IP
     * =========================================================
     */

    private fun getLocalIpv4Address(): String? {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (!networkInterface.isUp || networkInterface.isLoopback) {
                    continue
                }

                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (address is Inet4Address && !address.isLoopbackAddress) {
                        return address.hostAddress
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.e("VRPlayer", "Failed to get local IP", e)
            null
        }
    }

    /*
     * =========================================================
     * SCREEN STATE
     * =========================================================
     */

    private enum class AppScreen {
        HOME,
        VRSTREAM,
        MEDIA_LIBRARY,
        PLAYER
    }
}

