package com.sourceservermanager.rcon

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.Socket
import java.net.SocketTimeoutException

import com.sourceservermanager.rcon.exception.BadRcon
import com.sourceservermanager.rcon.exception.ResponseEmpty

/**
 * Rcon is a simple Java library for issuing RCON commands to game servers.
 *
 * This has currently only been used with HalfLife based servers.
 *
 * Example:
 * response = Rcon.send(27778, "127.0.0.1", 27015, rconPassword, "log on");
 *
 * PiTaGoRas - 21/12/2004<br></br>
 * Now also supports responses divided into multiple packets, bad rcon password
 * detection and other minor fixes/improvements.
 *
 * @author DeadEd
 * @version 1.1
 */
object Rcon {

    private val RESPONSE_TIMEOUT = 2000
    private val MULTIPLE_PACKETS_TIMEOUT = 300

    internal var rconSocket: Socket? = null
    internal var `in`: InputStream? = null
    internal var out: OutputStream? = null

    /**
     * Send the RCON request.  Sends the command to the game server.  A port
     * (localPort must be opened to send the command through.
     *
     * @param localPort The port on the local machine where the RCON request can be made from.
     * @param ipStr     The IP (as a String) of the machine where the RCON command will go.
     * @param port      The port of the machine where the RCON command will go.
     * @param password  The RCON password.
     * @param command   The RCON command (without the rcon prefix).
     * @return The reponse text from the server after trying the RCON command.
     * @throws SocketTimeoutException when there is any problem communicating with the server.
     */
    @Throws(SocketTimeoutException::class, BadRcon::class, ResponseEmpty::class)
    fun send(localPort: Int, ipStr: String, port: Int, password: String, command: String, rconTimeout: String): String {

        val requested = sendRequest(localPort, ipStr, port, password, command, rconTimeout)

        val response = assemblePacket(requested)

        if (response.matches("Bad rcon_password.\n".toRegex())) {
            throw BadRcon()
        }
        if (response.isEmpty()) {
            throw ResponseEmpty()
        }

        return response
    }

    private fun getDatagramPacket(request: String, inet: InetAddress, port: Int): DatagramPacket {
        val first: Byte = -1
        val last: Byte = 0
        val buffer = request.toByteArray()
        val commandBytes = ByteArray(buffer.size + 5)
        commandBytes[0] = first
        commandBytes[1] = first
        commandBytes[2] = first
        commandBytes[3] = first
        for (i in buffer.indices) {
            commandBytes[i + 4] = buffer[i]
        }
        commandBytes[buffer.size + 4] = last

        return DatagramPacket(commandBytes, commandBytes.size, inet, port)
    }

    @Throws(SocketTimeoutException::class)
    private fun sendRequest(localPort: Int, ipStr: String, port: Int, password: String,
                            command: String, rconTimeout: String): Array<RconPacket?> {

        var socket: DatagramSocket? = null
        val resp = arrayOfNulls<RconPacket>(128)

        try {
            socket = DatagramSocket(localPort)
            val packetSize = 1400

            val address = InetAddress.getByName(ipStr)
            //InetAddress address = InetAddress.getByName(getLocalIpAddress());
            val ip = address.address
            val inet = InetAddress.getByAddress(ip)
            val msg = "challenge rcon\n"

            val out = getDatagramPacket(msg, inet, port)
            socket.send(out)

            // get the challenge
            val data = ByteArray(packetSize)
            val inPacket = DatagramPacket(data, packetSize)

            socket.soTimeout = Integer.parseInt(rconTimeout) * 1000
            socket.receive(inPacket)

            // compose the final command and send to the server
            val challenge = parseResponse(inPacket.data)
            val challengeNumber = challenge.substring(challenge.indexOf("rcon") + 5).trim { it <= ' ' }
            val commandStr = "rcon $challengeNumber \"$password\" $command"
            val out2 = getDatagramPacket(commandStr, inet, port)
            socket.send(out2)

            // get the response
            val data2 = ByteArray(packetSize)
            val inPacket2 = DatagramPacket(data2, packetSize)
            socket.soTimeout = Integer.parseInt(rconTimeout) * 1000
            socket.receive(inPacket2)

            resp[0] = RconPacket(inPacket2)
            try {
                // Wait for a possible multiple packets response
                socket.soTimeout = MULTIPLE_PACKETS_TIMEOUT
                var i = 1
                while (true) {
                    socket.receive(inPacket2)
                    resp[i++] = RconPacket(inPacket2)
                }
            } catch (sex: SocketTimeoutException) {
                // Server didn't send more packets
            }

        } catch (sex: SocketTimeoutException) {
            throw sex
        } catch (ex: IOException) {
            ex.printStackTrace()
        } finally {
            socket?.close()
        }

        return resp
    }

    private fun parseResponse(buf: ByteArray): String {

        return if (buf[0].toInt() != -1 || buf[1].toInt() != -1 || buf[2].toInt() != -1 || buf[3].toInt() != -1) {
            "ERROR"
        } else {
            var off = 5
            val challenge = StringBuffer(20)
            while (buf[off].toInt() != 0) {
                challenge.append((buf[off++].toInt() and 255).toChar())
            }
            challenge.toString()
        }
    }

    private fun assemblePacket(respPacket: Array<RconPacket?>): String {

        var resp = ""

        // TODO: inspect the headers to decide the correct order

        for (i in respPacket.indices) {
            if (respPacket[i] != null) {
                resp += respPacket[i]!!.data
            }
        }
        return resp
    }
}

/**
 * Represents a rcon response packet from the game server. A response may be split
 * into multiple packets, so an array of RconPackets should be used.
 *
 * @param packet One DatagramPacket returned by the server
 */
internal class RconPacket(packet: DatagramPacket) {

    /**
     * ASCII representation of the full packet received (header included)
     */
    var ascii = ""

    /**
     * The data included in the packet, header removed
     */
    var data = ""

    /**
     * The full packet received (header included) in bytes
     */
    var bytes = ByteArray(1400)

    /**
     * Length of the packet
     */
    var length = 0

    init {

        this.ascii = String(packet.data, 0, packet.length)
        this.bytes = ascii.toByteArray()
        this.length = packet.length

        // Now we remove the headers from the packet to have just the text
        if (bytes[0].toInt() == -2) {
            // this response comes divided into two packets
            if (bytes[13].toInt() == 108) {
                this.data = String(packet.data, 14, packet.length - 16)
            } else {
                this.data = String(packet.data, 11, packet.length - 13)
            }
        } else {
            // Single packet
            this.data = String(packet.data, 5, packet.length - 7)
        }
    }
}