package com.sourceservermanager.rcon

/**
 * This file is part of the Source Dedicated Server Controller project.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 * Additional permission under GNU GPL version 3 section 7
 *
 * If you modify this Program, or any covered work, by linking or
 * combining it with srcds-controller (or a modified version of that library),
 * containing parts covered by the terms of GNU General Public License,
 * the licensors of this Program grant you additional permission to convey
 * the resulting work. {Corresponding Source for a non-source form of such a
 * combination shall include the source code for the parts of srcds-controller
 * used as well as that of the covered work.}
 *
 * For more information, please consult:
 *    <http://www.earthquake-clan.de/srcds/>
 *    <http://code.google.com/p/srcds-controller/>
 */

import com.sourceservermanager.rcon.exception.AuthenticationException
import com.sourceservermanager.rcon.exception.NotOnlineException
import com.sourceservermanager.rcon.exception.ResponseEmptyException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class SourceRconConnection(ip: String, port: Int, password: String) {

    companion object {
        private const val SERVERDATA_EXECCOMMAND = 2
        private const val SERVERDATA_AUTH = 3
        private const val SERVERDATA_AUTH_RESPONSE = 2
        private const val RESPONSE_TIMEOUT = 2000
        private const val MULTIPLE_PACKETS_TIMEOUT = 300
        private const val RESPONSE_ID = 1337
    }

    private var rconSocket: Socket = Socket()
    private var `in`: InputStream
    private var out: OutputStream

    init {
        try {
            rconSocket.connect(InetSocketAddress(ip, port), 1000)
            rconSocket.soTimeout = RESPONSE_TIMEOUT
            out = rconSocket.getOutputStream()
            `in` = rconSocket.getInputStream()
        } catch (e: IOException) {
            throw NotOnlineException(e.localizedMessage)
        }

        if (!authenticate(password)) {
            throw AuthenticationException("Authentication failed")
        }
    }

    @Throws(IOException::class)
    fun close() {
        out.close()
        `in`.close()
        rconSocket.close()
    }

    @Throws(SocketTimeoutException::class, AuthenticationException::class, ResponseEmptyException::class)
    fun send(command: String): String {

        var response: String? = null
        val resp = sendCommand(command)
        if (resp.isNotEmpty()) {
            response = assemblePackets(resp)
        }
        if (response == null || response.isEmpty()) {
            throw ResponseEmptyException("Response is empty")
        }
        return response
    }

    @Throws(SocketTimeoutException::class)
    private fun sendCommand(command: String): Array<ByteBuffer?> {

        val request = constructPacket(2, SERVERDATA_EXECCOMMAND, command)

        var resp = arrayOfNulls<ByteBuffer>(128)
        var i = 0
        try {
            out.write(request)
            resp[i] = receivePacket()
            try {
                // We don't know how many packets will return in response, so
                // we'll read() the socket until TimeoutException occurs.
                rconSocket.soTimeout = MULTIPLE_PACKETS_TIMEOUT
                while (true) {
                    resp[++i] = receivePacket()
                }
            } catch (e: SocketTimeoutException) {
                // No more packets in the response, go on
            }

        } catch (e: Exception) {
            resp = arrayOfNulls(0)
        }

        return resp
    }

    private fun constructPacket(id: Int, cmdtype: Int, s1: String): ByteArray {

        val p = ByteBuffer.allocate(s1.length + 16)
        p.order(ByteOrder.LITTLE_ENDIAN)

        // length of the packet
        p.putInt(s1.length + 12)
        // request id
        p.putInt(id)
        // type of command
        p.putInt(cmdtype)
        // the command itself
        p.put(s1.toByteArray())
        // two null bytes at the end
        p.put(0x00.toByte())
        p.put(0x00.toByte())
        // null string2 (see Source protocol)
        p.put(0x00.toByte())
        p.put(0x00.toByte())

        return p.array()
    }

    @Throws(IOException::class)
    private fun receivePacket(): ByteBuffer? {

        val p = ByteBuffer.allocate(4120)
        p.order(ByteOrder.LITTLE_ENDIAN)

        val length = ByteArray(4)
        var ret: ByteBuffer? = null
        if (`in`.read(length, 0, 4) == 4) {
            // Now we've the length of the packet, let's go read the bytes
            p.put(length)
            var i = 0
            while (i < p.getInt(0)) {
                p.put(`in`.read().toByte())
                i++
            }
            ret = p
        }
        return ret
    }

    private fun assemblePackets(packets: Array<ByteBuffer?>?): String {
        // Return the text from all the response packets together
        var response = ""

        if (packets != null) {
            for (i in packets.indices) {
                if (packets[i] != null) {
                    response += String(packets[i]!!.array(), 12, packets[i]!!.position() - 14)
                }
            }
        }
        return response
    }

    @Throws(AuthenticationException::class)
    private fun authenticate(password: String): Boolean {

        val authRequest = constructPacket(RESPONSE_ID, SERVERDATA_AUTH, password)
        var response: ByteBuffer?
        var ret = false
        try {
            out.write(authRequest)
            // junk response packet
            response = receivePacket()
            response = receivePacket()

            if (response!!.getInt(4) == RESPONSE_ID && response.getInt(8) == SERVERDATA_AUTH_RESPONSE) {
                ret = true
            }
        } catch (e: IOException) {
            throw AuthenticationException(e.localizedMessage, e)
        }

        return ret
    }

}