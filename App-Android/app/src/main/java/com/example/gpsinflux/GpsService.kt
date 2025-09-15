package com.example.gpsinflux

import android.Manifest
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.util.Locale

class GpsService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val client = OkHttpClient()

    override fun onCreate() {
        super.onCreate()
        startForegroundNotification()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 2000 // alle 2s
        ).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (loc in result.locations) {
                    Log.d("GpsService", "GPS: ${loc.latitude}, ${loc.longitude}, ${loc.altitude}")
                    sendToInflux(loc)
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    private fun sendToInflux(loc: Location) {
        val url = "http://192.168.10.22:8086/api/v2/write?org=influxdb&bucket=gps&precision=s"
        val body = "gps,device=android " +
            "lat=${String.format(Locale.US, "%.6f", loc.latitude)}," +
            "lon=${String.format(Locale.US, "%.6f", loc.longitude)}," +
            "alt=${String.format(Locale.US, "%.2f", loc.altitude)}"

        Thread {
            try {
                Log.d("GpsService", "Sende an Influx: $body -> $url")
                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Token 0Xvm1-nj1B6gxs1851f2wwlIlCbj3JRwBi56mp3q_lkTNexsrK1fHwpJqvqlVAngsePyPOJOf_p0JMOCMIAz-g==")
                    .post(RequestBody.create("text/plain".toMediaType(), body))
                    .build()
                val response = client.newCall(request).execute()
                Log.d("GpsService", "Antwort von Influx: ${response.code}")
                response.close()
            } catch (e: Exception) {
                Log.e("GpsService", "Fehler beim Senden", e)
            }
        }.start()
    }

    private fun startForegroundNotification() {
        val channelId = "gps_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "GPS Service",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("GPS Influx aktiv")
            .setContentText("Standort wird im Hintergrund an Influx gesendet")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onBind(intent: android.content.Intent?): IBinder? = null
}
