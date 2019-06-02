package com.sourceservermanager.rcon

import java.net.DatagramPacket

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