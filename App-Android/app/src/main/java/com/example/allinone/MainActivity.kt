package com.example.allinone

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate

import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.allinone.net.ApiClient
import com.example.allinone.net.SshHelper

class MainActivity : AppCompatActivity() {

    private lateinit var btnGpsStart: Button
    private lateinit var btnGpsStop: Button
    private lateinit var txtGpsStatus: TextView

    private lateinit var btnRefreshAll: Button
    private lateinit var dotWg: View
    private lateinit var dotDts: View
    private lateinit var btnWgOn: Button
    private lateinit var btnWgOff: Button
    private lateinit var btnDtsOn: Button
    private lateinit var btnDtsOff: Button

    private lateinit var txtSpeedTime: TextView
    private lateinit var txtSpeedPing: TextView
    private lateinit var txtSpeedDown: TextView
    private lateinit var txtSpeedUp: TextView

    private lateinit var dotDocker: View
    private lateinit var txtDockerStatus: TextView
    private lateinit var btnDockerStart: Button
    private lateinit var btnDockerStop: Button
    private lateinit var btnDockerRestart: Button

    private lateinit var dotScript: View
    private lateinit var txtScriptStatus: TextView
    private lateinit var btnScriptStart: Button
    private lateinit var btnScriptStop: Button
    private lateinit var btnScriptRestart: Button

    private lateinit var txtPiUptime: TextView
    private lateinit var btnShutdownPi: Button

    private lateinit var txtLog: TextView
    private lateinit var scrollLog: ScrollView

    private val routerUser = "admin"
    private val routerPass = "*****"
    private val WG_PATH = "/api/wireguard/config/Hetzner"
    private val DTS_PATH = "/api/data_to_server/collections/config/1"

    private val pi = SshHelper.Host("192.168.253.2", "root", "*****")
    private val server = SshHelper.Host("192.168.10.22", "root", "*****")

    private val speedReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == GpsService.ACTION_SPEEDTEST) {
                txtSpeedTime.text = "Zeit: " + (intent.getStringExtra("time") ?: "-")
                txtSpeedPing.text = "Ping: " + (intent.getStringExtra("ping") ?: "-") + " ms"
                txtSpeedDown.text = "Down: " + (intent.getStringExtra("down") ?: "-")
                txtSpeedUp.text = "Up: " + (intent.getStringExtra("up") ?: "-")
                fetchPiUptime()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        setContentView(R.layout.activity_main)
        bindViews()
        wireEvents()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 1001)
        }
    }

    override fun onStart() {
        super.onStart()
        registerReceiver(speedReceiver, IntentFilter(GpsService.ACTION_SPEEDTEST))
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(speedReceiver)
    }

    private fun bindViews() {
        btnGpsStart = findViewById(R.id.btnGpsStart)
        btnGpsStop = findViewById(R.id.btnGpsStop)
        txtGpsStatus = findViewById(R.id.txtGpsStatus)

        btnRefreshAll = findViewById(R.id.btnRefreshAll)
        dotWg = findViewById(R.id.dotWg)
        dotDts = findViewById(R.id.dotDts)
        btnWgOn = findViewById(R.id.btnWgOn)
        btnWgOff = findViewById(R.id.btnWgOff)
        btnDtsOn = findViewById(R.id.btnDtsOn)
        btnDtsOff = findViewById(R.id.btnDtsOff)

        txtSpeedTime = findViewById(R.id.txtSpeedTime)
        txtSpeedPing = findViewById(R.id.txtSpeedPing)
        txtSpeedDown = findViewById(R.id.txtSpeedDown)
        txtSpeedUp = findViewById(R.id.txtSpeedUp)

        dotDocker = findViewById(R.id.dotDocker)
        txtDockerStatus = findViewById(R.id.txtDockerStatus)
        btnDockerStart = findViewById(R.id.btnDockerStart)
        btnDockerStop = findViewById(R.id.btnDockerStop)
        btnDockerRestart = findViewById(R.id.btnDockerRestart)

        dotScript = findViewById(R.id.dotScript)
        txtScriptStatus = findViewById(R.id.txtScriptStatus)
        btnScriptStart = findViewById(R.id.btnScriptStart)
        btnScriptStop = findViewById(R.id.btnScriptStop)
        btnScriptRestart = findViewById(R.id.btnScriptRestart)

        txtPiUptime = findViewById(R.id.txtPiUptime)
        btnShutdownPi = findViewById(R.id.btnShutdownPi)

        txtLog = findViewById(R.id.txtLog)
        scrollLog = findViewById(R.id.scrollLog)
    }

    private fun wireEvents() {
        btnGpsStart.setOnClickListener {
            androidx.core.content.ContextCompat.startForegroundService(this, Intent(this, GpsService::class.java))
            txtGpsStatus.text = "GPS: läuft"
        }
        btnGpsStop.setOnClickListener {
            stopService(Intent(this, GpsService::class.java))
            txtGpsStatus.text = "GPS: gestoppt"
        }

        btnRefreshAll.setOnClickListener { refreshAll() }

        btnWgOn.setOnClickListener { setRouterEnabled(WG_PATH, true) }
        btnWgOff.setOnClickListener { setRouterEnabled(WG_PATH, false) }
        btnDtsOn.setOnClickListener { setRouterEnabled(DTS_PATH, true) }
        btnDtsOff.setOnClickListener { setRouterEnabled(DTS_PATH, false) }

        btnDockerStart.setOnClickListener { dockerCmd("start") }
        btnDockerStop.setOnClickListener { dockerCmd("stop") }
        btnDockerRestart.setOnClickListener { dockerCmd("restart") }

        btnScriptStart.setOnClickListener { scriptCmd("start") }
        btnScriptStop.setOnClickListener { scriptCmd("stop") }
        btnScriptRestart.setOnClickListener { scriptCmd("restart") }

        btnShutdownPi.setOnClickListener {
            appendLog("Pi wird heruntergefahren …")
            Thread {
                val out = com.example.allinone.net.SshHelper.exec(pi, "shutdown -h now")
                runOnUiThread { appendLog("Pi Shutdown: $out") }
            }.start()
        }
    }

    private fun refreshAll() {
        appendLog("Aktualisiere: Router, Docker, Script, Speedtest, Uptime …")
        refreshRouterStatus()
        dockerCmd("status")
        scriptCmd("status")
        refreshSpeedtestOnce()
        fetchPiUptime()
    }

    private fun refreshRouterStatus() {
        Thread {
            val token = com.example.allinone.net.ApiClient.loginRouter(routerUser, routerPass)
            if (token == null) {
                runOnUiThread { appendLog("Router: Login fehlgeschlagen") }
                return@Thread
            }
            val wg = com.example.allinone.net.ApiClient.getEnabled(WG_PATH, token)
            val dts = com.example.allinone.net.ApiClient.getEnabled(DTS_PATH, token)
            runOnUiThread {
                setDot(dotWg, wg == true); setDot(dotDts, dts == true)
                appendLog("Router: WireGuard=${stateText(wg)}, Data-to-Server=${stateText(dts)}")
            }
        }.start()
    }

    private fun setRouterEnabled(path: String, enabled: Boolean) {
        appendLog("Router: setze ${if (path==WG_PATH) "WireGuard" else "Data-to-Server"} -> ${if (enabled) "AN" else "AUS"}")
        Thread {
            val token = com.example.allinone.net.ApiClient.loginRouter(routerUser, routerPass) ?: run {
                runOnUiThread { appendLog("Router: Login fehlgeschlagen") }
                return@Thread
            }
            val ok = com.example.allinone.net.ApiClient.setEnabled(path, token, enabled)
            runOnUiThread { appendLog(if (ok) "Router: OK" else "Router: Fehler"); refreshRouterStatus() }
        }.start()
    }

    private fun dockerCmd(action: String) {
        val cmd = when(action) {
            "start" -> "docker start speedtest-tracker"
            "stop" -> "docker stop speedtest-tracker"
            "restart" -> "docker restart speedtest-tracker"
            else -> "docker inspect -f '{{.State.Running}}' speedtest-tracker || echo false"
        }
        Thread {
            val out = com.example.allinone.net.SshHelper.exec(pi, cmd)
            val status = com.example.allinone.net.SshHelper.exec(pi, "docker inspect -f '{{.State.Running}}' speedtest-tracker || echo false")
            runOnUiThread {
                val running = status.contains("true")
                setDot(dotDocker, running)
                txtDockerStatus.text = if (running) "Status: läuft" else "Status: gestoppt"
                appendLog("Pi Docker ($action): $out")
            }
        }.start()
    }

    private fun scriptCmd(action: String) {
        val statusCmd = "screen -ls | grep data_to_server_web || true"
        val startCmd = "screen -dmS data_to_server_web python3 /root/data_to_server_web.py"
        val stopCmd = "screen -S data_to_server_web -X quit || true"
        val cmd = when(action) {
            "start" -> startCmd
            "stop" -> stopCmd
            "restart" -> "$stopCmd; sleep 1; $startCmd"
            else -> statusCmd
        }
        Thread {
            val out = com.example.allinone.net.SshHelper.exec(server, cmd)
            val status = com.example.allinone.net.SshHelper.exec(server, statusCmd)
            runOnUiThread {
                val running = status.contains("data_to_server_web")
                setDot(dotScript, running)
                txtScriptStatus.text = if (running) "Status: läuft" else "Status: gestoppt"
                appendLog("Server Script ($action): $out")
            }
        }.start()
    }

    private fun refreshSpeedtestOnce() {
        Thread {
            val data = com.example.allinone.net.ApiClient.fetchSpeedtest() ?: run {
                runOnUiThread { appendLog("Speedtest: keine Daten") }
                return@Thread
            }
            val ping = data.optDouble("ping", 0.0)
            if (ping == 0.0) { runOnUiThread { appendLog("Speedtest: ping==0, übersprungen") }; return@Thread }
            var time = data.optString("updated_at", "N/A")
            if (time != "N/A" && time.contains(" ")) {
                try {
                    val src = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
                    src.timeZone = java.util.TimeZone.getTimeZone("UTC")
                    val ts = src.parse(time)
                    val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Europe/Berlin"))
                    cal.time = ts!!; cal.add(java.util.Calendar.HOUR_OF_DAY, 2)
                    val out = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                    out.timeZone = java.util.TimeZone.getDefault()
                    time = out.format(cal.time)
                } catch (_: Exception) {}
            }
            val down = data.optString("download_bits_human", "N/A")
            val up = data.optString("upload_bits_human", "N/A")
            runOnUiThread {
                txtSpeedTime.text = "Zeit: $time"
                txtSpeedPing.text = "Ping: $ping ms"
                txtSpeedDown.text = "Down: $down"
                txtSpeedUp.text = "Up: $up"
                appendLog("Speedtest aktualisiert")
            }
        }.start()
    }

    private fun fetchPiUptime() {
        Thread {
            val out = com.example.allinone.net.SshHelper.exec(pi, "uptime -p || true")
            runOnUiThread { txtPiUptime.text = "Uptime: $out"; appendLog("Pi Uptime: $out") }
        }.start()
    }

    private fun setDot(v: View, on: Boolean) {
        v.background = androidx.core.content.ContextCompat.getDrawable(this, if (on) R.drawable.dot_green else R.drawable.dot_red)
    }
    private fun stateText(b: Boolean?): String = when(b){ true->"AN"; false->"AUS"; else->"??" }

    private fun appendLog(msg: String) {
        txtLog.append(if (txtLog.text.isEmpty()) msg else "\n$msg")
        scrollLog.post { scrollLog.fullScroll(View.FOCUS_DOWN) }
    }
}