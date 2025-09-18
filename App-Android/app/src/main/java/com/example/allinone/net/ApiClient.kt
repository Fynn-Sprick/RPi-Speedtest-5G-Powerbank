package com.example.allinone.net

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.*

object ApiClient {
    private fun unsafeOkHttpClient(): OkHttpClient {
        val trustAll = arrayOf<TrustManager>(object: X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })
        val ctx = SSLContext.getInstance("SSL").apply { init(null, trustAll, SecureRandom()) }
        return OkHttpClient.Builder()
            .sslSocketFactory(ctx.socketFactory, trustAll[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .build()
    }
    private val client by lazy { unsafeOkHttpClient() }

    private const val ROUTER = "https://192.168.253.1"
    private const val SPEED_URL = "http://192.168.253.2:8080/api/v1/results/latest"
    private const val SPEED_TOKEN = "oQfVuUAreiMtpI2HKDGn3t3YhoaTEMNp0hRUHwt390319742"

    fun loginRouter(user: String, pass: String): String? {
        val body = JSONObject().put("username", user).put("password", pass).toString()
        val req = Request.Builder()
            .url("$ROUTER/api/login")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val txt = resp.body?.string() ?: return null
            val o = JSONObject(txt)
            if (!o.optBoolean("success", false)) return null
            return o.optJSONObject("data")?.optString("token")
        }
    }

    fun getEnabled(path: String, token: String): Boolean? {
        val req = Request.Builder().url("$ROUTER$path").get().addHeader("Authorization", "Bearer $token").build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val txt = resp.body?.string() ?: return null
            val data = JSONObject(txt).optJSONObject("data") ?: return null
            val v = data.opt("enabled")?.toString() ?: "0"
            return (v == "1" || v.equals("true", true))
        }
    }

    fun setEnabled(path: String, token: String, en: Boolean): Boolean {
        val payload = JSONObject().put("data", JSONObject().put("enabled", if (en) "1" else "0")).toString()
        val req = Request.Builder()
            .url("$ROUTER$path")
            .put(payload.toRequestBody("application/json".toMediaType()))
            .addHeader("Authorization", "Bearer $token")
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return false
            val txt = resp.body?.string() ?: return false
            return JSONObject(txt).optBoolean("success", false)
        }
    }

    fun fetchSpeedtest(): JSONObject? {
        val req = Request.Builder().url(SPEED_URL).get().addHeader("Authorization", "Bearer $SPEED_TOKEN").build()
        return try {
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val txt = resp.body?.string() ?: return null
                JSONObject(txt).optJSONObject("data")
            }
        } catch (_: Exception) { null }
    }
}