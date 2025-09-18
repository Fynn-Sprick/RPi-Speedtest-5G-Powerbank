package com.example.allinone.net

import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import java.io.ByteArrayOutputStream

object SshHelper {
    data class Host(val host: String, val user: String, val password: String, val port: Int = 22)

    fun exec(host: Host, command: String, timeoutMs: Int = 10000): String {
        val jsch = JSch()
        val session: Session = jsch.getSession(host.user, host.host, host.port)
        session.setPassword(host.password)
        val config = java.util.Properties()
        config["StrictHostKeyChecking"] = "no"
        session.setConfig(config)
        session.timeout = timeoutMs
        session.connect(timeoutMs)
        val ch = session.openChannel("exec") as ChannelExec
        ch.setCommand(command)
        val out = ByteArrayOutputStream()
        ch.outputStream = out
        ch.connect(timeoutMs)
        while (!ch.isClosed) Thread.sleep(100)
        ch.disconnect()
        session.disconnect()
        return out.toString().trim()
    }
}