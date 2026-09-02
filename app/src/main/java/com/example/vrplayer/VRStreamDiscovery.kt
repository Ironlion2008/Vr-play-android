package com.example.vrplayer

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit

class VRStreamDiscovery {

    companion object {

        private const val TAG = "VRPlayer"

        private const val VRSTREAM_PORT = 6969

        /*
         * TCP probe timeout.
         *
         * This is intentionally short because we may test
         * many devices on the LAN.
         */
        private const val TCP_TIMEOUT_MS = 300

        /*
         * HTTP verification timeout.
         */
        private const val HTTP_CONNECT_TIMEOUT_MS = 700L
        private const val HTTP_READ_TIMEOUT_MS = 1500L

        /*
         * Maximum simultaneous TCP probes.
         *
         * 32 gives us a good balance between discovery speed
         * and not hammering the Android network stack/router.
         */
        private const val MAX_CONCURRENT_PROBES = 32
    }

    private val httpClient =
        OkHttpClient.Builder()
            .connectTimeout(
                HTTP_CONNECT_TIMEOUT_MS,
                TimeUnit.MILLISECONDS
            )
            .readTimeout(
                HTTP_READ_TIMEOUT_MS,
                TimeUnit.MILLISECONDS
            )
            .writeTimeout(
                HTTP_READ_TIMEOUT_MS,
                TimeUnit.MILLISECONDS
            )
            .build()

    /*
     * --------------------------------------------------
     * Public discovery API
     * --------------------------------------------------
     *
     * The existing MainActivity expects one server, so
     * discover() remains compatible with it.
     *
     * We return the first verified VRStream server.
     *
     * Later, when we build the server-selection UI,
     * discoverAll() will expose every server found.
     */
    suspend fun discover(
        localIp: String
    ): VRStreamServer? {

        return withContext(Dispatchers.IO) {

            val servers =
                discoverAllInternal(localIp)

            servers.firstOrNull()
        }
    }

    /*
     * --------------------------------------------------
     * Discover every VRStream server
     * --------------------------------------------------
     *
     * This is the API we will use when the UI is ready
     * to display multiple servers.
     */
    suspend fun discoverAll(
        localIp: String
    ): List<VRStreamServer> {

        return withContext(Dispatchers.IO) {

            discoverAllInternal(localIp)
        }
    }

    /*
     * --------------------------------------------------
     * Actual LAN scan
     * --------------------------------------------------
     */

    private suspend fun discoverAllInternal(
        localIp: String
    ): List<VRStreamServer> {

        val subnet =
            subnetPrefix(localIp)

        if (subnet == null) {

            Log.e(
                TAG,
                "Unable to determine IPv4 subnet from: $localIp"
            )

            return emptyList()
        }

        Log.d(
            TAG,
            "LAN discovery starting"
        )

        Log.d(
            TAG,
            "Local IP: $localIp"
        )

        Log.d(
            TAG,
            "Scanning subnet: $subnet.0/24"
        )

        Log.d(
            TAG,
            "Checking TCP port $VRSTREAM_PORT"
        )

        /*
         * --------------------------------------------------
         * Phase 1:
         * Fast concurrent TCP port scan.
         * --------------------------------------------------
         */

        val reachableHosts =
            mutableListOf<String>()

        /*
         * Use batches instead of launching all 254 sockets
         * simultaneously.
         */
        for (
        batchStart in
        1..254 step MAX_CONCURRENT_PROBES
        ) {

            val batchEnd =
                minOf(
                    batchStart +
                            MAX_CONCURRENT_PROBES -
                            1,
                    254
                )

            val batchHosts =
                (batchStart..batchEnd)
                    .map { lastOctet ->
                        "$subnet.$lastOctet"
                    }
                    .filter { host ->
                        host != localIp
                    }

            val results =
                coroutineScope {

                    batchHosts.map { host ->

                        async(Dispatchers.IO) {

                            if (
                                isPortOpen(
                                    host,
                                    VRSTREAM_PORT
                                )
                            ) {
                                host
                            } else {
                                null
                            }
                        }
                    }.awaitAll()
                }

            results
                .filterNotNull()
                .forEach { host ->

                    reachableHosts.add(host)

                    Log.d(
                        TAG,
                        "Port $VRSTREAM_PORT OPEN: $host"
                    )
                }
        }

        Log.d(
            TAG,
            "TCP scan complete. " +
                    "Open port $VRSTREAM_PORT hosts: " +
                    reachableHosts.size
        )

        /*
         * --------------------------------------------------
         * Phase 2:
         * Verify that the open port actually belongs
         * to VRStream.
         * --------------------------------------------------
         */

        val verifiedServers =
            reachableHosts
                .mapNotNull { host ->

                    probeVrStream(host)
                }

        Log.d(
            TAG,
            "VRStream discovery complete. " +
                    "Servers found: " +
                    verifiedServers.size
        )

        verifiedServers.forEach { server ->

            Log.d(
                TAG,
                "VRStream SERVER: " +
                        server.baseUrl
            )
        }

        return verifiedServers
    }

    /*
     * --------------------------------------------------
     * TCP port probe
     * --------------------------------------------------
     */

    private fun isPortOpen(
        host: String,
        port: Int
    ): Boolean {

        return try {

            Socket().use { socket ->

                socket.connect(
                    InetSocketAddress(
                        host,
                        port
                    ),
                    TCP_TIMEOUT_MS
                )

                true
            }

        } catch (
            _: Exception
        ) {

            false
        }
    }

    /*
     * --------------------------------------------------
     * VRStream API verification
     * --------------------------------------------------
     *
     * A device having port 6969 open is not enough.
     *
     * We verify:
     *
     *   GET /api/media
     *
     * before showing it as a VRStream server.
     */
    private fun probeVrStream(
        host: String
    ): VRStreamServer? {

        return try {

            val url =
                "http://$host:$VRSTREAM_PORT/api/media"

            val request =
                Request.Builder()
                    .url(url)
                    .get()
                    .header(
                        "Accept",
                        "application/json"
                    )
                    .build()

            httpClient
                .newCall(request)
                .execute()
                .use { response ->

                    Log.d(
                        TAG,
                        "VRStream verification " +
                                "$host:$VRSTREAM_PORT -> " +
                                "HTTP ${response.code}"
                    )

                    if (
                        !response.isSuccessful
                    ) {
                        return null
                    }

                    VRStreamServer(
                        host = host,
                        port = VRSTREAM_PORT,
                        baseUrl =
                            "http://$host:$VRSTREAM_PORT"
                    )
                }

        } catch (
            e: Exception
        ) {

            Log.d(
                TAG,
                "VRStream verification failed " +
                        "for $host:$VRSTREAM_PORT: " +
                        "${e.javaClass.simpleName}: " +
                        "${e.message}"
            )

            null
        }
    }

    /*
     * --------------------------------------------------
     * IPv4 subnet calculation
     * --------------------------------------------------
     *
     * Current target is normal /24 home LANs such as:
     *
     *   192.168.1.x
     *   192.168.137.x
     *
     * We intentionally keep this simple for this first
     * implementation.
     */
    private fun subnetPrefix(
        localIp: String
    ): String? {

        val parts =
            localIp.split(".")

        if (parts.size != 4) {
            return null
        }

        if (
            parts.any { part ->
                part.toIntOrNull()
                    ?.let { it !in 0..255 }
                    ?: true
            }
        ) {
            return null
        }

        return parts
            .take(3)
            .joinToString(".")
    }
}