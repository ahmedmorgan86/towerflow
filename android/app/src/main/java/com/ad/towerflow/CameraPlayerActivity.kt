package com.ad.towerflow

import android.app.Activity
import android.os.Bundle
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.Button
import android.widget.TextView
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.ui.PlayerView
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class CameraPlayerActivity : Activity() {
    private var player: ExoPlayer? = null
    private val worker = Executors.newSingleThreadExecutor()
    private val stopped = AtomicBoolean(false)
    private var session = ""
    private lateinit var origin: String
    private lateinit var token: String
    private lateinit var camera: String
    private lateinit var org: String
    private fun request(action: String, data: JSONObject): JSONObject {
        val connection = URL("$origin/api/cameras/$camera/$action?org=$org").openConnection() as HttpURLConnection
        try { connection.instanceFollowRedirects = false; connection.requestMethod = "POST"; connection.connectTimeout = 10000; connection.readTimeout = 25000; connection.setRequestProperty("Authorization", "Bearer $token"); connection.setRequestProperty("X-TowerFlow-Client", "android"); connection.setRequestProperty("Content-Type", "application/json"); connection.doOutput = true; connection.outputStream.use { it.write(data.toString().toByteArray()) }; if (connection.responseCode !in 200..299) throw IllegalStateException("Camera unavailable or viewing permission expired"); return JSONObject(connection.inputStream.bufferedReader().use { it.readText() }) } finally { connection.disconnect() }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        origin = intent.getStringExtra("origin") ?: ""; token = intent.getStringExtra("token") ?: ""; camera = intent.getStringExtra("camera") ?: ""; org = intent.getStringExtra("org") ?: ""
        intent.removeExtra("token")
        val uri = try { URI(origin) } catch (_: Exception) { finish(); return }
        if (uri.scheme != "https" || uri.host.isNullOrEmpty() || uri.userInfo != null || uri.query != null || uri.fragment != null || (!uri.path.isNullOrEmpty() && uri.path != "/") || !Regex("[a-f0-9-]{36}").matches(camera) || !Regex("[a-f0-9-]{36}").matches(org) || !Regex("[a-f0-9]{64}").matches(token)) { finish(); return }
        origin = origin.trimEnd('/')
        val arabic = intent.getBooleanExtra("arabic", true)
        val status = TextView(this).apply { text = if (arabic) "جارٍ تجهيز البث الآمن…" else "Preparing secure stream…"; textSize = 17f; setPadding(20,20,20,20) }
        val video = PlayerView(this)
        val stop = Button(this).apply { text = if (arabic) "إيقاف المشاهدة والعودة" else "Stop viewing and return"; setOnClickListener { finish() } }
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; addView(status); addView(video, LinearLayout.LayoutParams(-1,0,1f)); addView(stop) }; setContentView(layout)
        worker.execute { try { val response = request("live", JSONObject()); session = response.getString("id"); if (stopped.get()) { request("close", JSONObject().put("session_id", session)); return@execute }; val path = response.getString("url"); require(path.startsWith("/api/cameras/$camera/media/") && !path.startsWith("//")); runOnUiThread { if (!stopped.get()) {
            val factory = DefaultHttpDataSource.Factory().setDefaultRequestProperties(mapOf("Authorization" to "Bearer $token", "X-TowerFlow-Client" to "android")).setAllowCrossProtocolRedirects(false)
            val item = MediaItem.Builder().setUri(origin + path).setMimeType(MimeTypes.APPLICATION_M3U8).build()
            player = ExoPlayer.Builder(this).build().also { p -> video.player = p; p.addListener(object: Player.Listener { override fun onPlayerError(error: PlaybackException) { status.text = if (arabic) "انقطع البث أو انتهت صلاحية المشاهدة. أعد فتح الكاميرا." else "Stream interrupted or access expired. Reopen the camera." } }); p.setMediaSource(HlsMediaSource.Factory(factory).createMediaSource(item)); p.prepare(); p.playWhenReady = true; status.text = if (arabic) "جلسة مشاهدة مؤقتة — خمس دقائق" else "Temporary viewing session — five minutes" }
        } } } catch (_: Exception) { runOnUiThread { status.text = if (arabic) "تعذر تشغيل الكاميرا. تحقق من الاتصال والصلاحية والبوابة." else "Cannot start camera. Check connection, access and gateway." } } }
    }
    override fun onStop() { stopped.set(true); player?.release(); player = null; if (::origin.isInitialized && session.isNotEmpty()) worker.execute { try { request("close", JSONObject().put("session_id", session)) } catch (_: Exception) {} }; super.onStop() }
    override fun onDestroy() { worker.shutdown(); super.onDestroy() }
}
