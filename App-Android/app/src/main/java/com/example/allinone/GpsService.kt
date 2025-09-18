package com.example.allinone

import android.Manifest
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.allinone.net.ApiClient
import com.google.android.gms.location.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.timer

class GpsService : Service() {

    private lateinit var fused: FusedLocationProviderClient
    private lateinit var callback: LocationCallback
    private val http = OkHttpClient()
    private var speedTimer: Timer? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundNotification()

        fused = LocationServices.getFusedLocationProviderClient(this)
        val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000).build()
        callback = object: LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (loc in result.locations) sendToInflux(loc)
            }
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fused.requestLocationUpdates(req, callback, Looper.getMainLooper())
        }
        startSpeedtestPolling()
    }

    private fun startSpeedtestPolling() {
        speedTimer?.cancel()
        speedTimer = timer(initialDelay = 1500, period = 60_000) {
            val data = ApiClient.fetchSpeedtest() ?: return@timer
            val ping = data.optDouble("ping", 0.0)
            if (ping == 0.0) return@timer
            var timeStr = data.optString("updated_at", "N/A")
            if (timeStr != "N/A" && timeStr.contains(" ")) {
                try {
                    val src = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                    src.timeZone = TimeZone.getTimeZone("UTC")
                    val ts = src.parse(timeStr)
                    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                    cal.time = ts!!
                    cal.add(Calendar.HOUR_OF_DAY, 2)
                    val out = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    out.timeZone = TimeZone.getDefault()
                    timeStr = out.format(cal.time)
                } catch (_: Exception) {}
            }
            val intent = Intent(ACTION_SPEEDTEST)
                .putExtra("time", timeStr)
                .putExtra("ping", ping.toString())
                .putExtra("down", data.optString("download_bits_human", "N/A"))
                .putExtra("up", data.optString("upload_bits_human", "N/A"))
            sendBroadcast(intent)
        }
    }

    private fun sendToInflux(loc: Location) {
        val url = "http://192.168.10.22:8086/api/v2/write?org=influxdb&bucket=gps&precision=s"
        val body = "gps,device=android " +
            "lat=%s,lon=%s,alt=%s".format(
                Locale.US,
                String.format(Locale.US, "%.6f", loc.latitude),
                String.format(Locale.US, "%.6f", loc.longitude),
                String.format(Locale.US, "%.2f", loc.altitude)
            )
        Thread {
            try {
                val req = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Token 0Xvm1-nj1B6gxs1851f2wwlIlCbj3JRwBi56mp3q_lkTNexsrK1fHwpJqvqlVAngsePyPOJOf_p0JMOCMIAz-g==")
                    .post(RequestBody.create("text/plain".toMediaType(), body))
                    .build()
                http.newCall(req).execute().use { }
            } catch (_: Exception) {}
        }.start()
    }

    private fun startForegroundNotification() {
        val channelId = "gps_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "GPS Service", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val pi = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notif = NotificationCompat.Builder(this, channelId)
            .setContentTitle("AllInOne läuft")
            .setContentText("GPS + Speedtest im Hintergrund aktiv")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
        startForeground(1, notif)
    }

    override fun onDestroy() {
        super.onDestroy()
        fused.removeLocationUpdates(callback)
        speedTimer?.cancel()
    }

    override fun onBind(intent: Intent?) = null

    companion object { const val ACTION_SPEEDTEST = "com.example.allinone.SPEEDTEST" }
}