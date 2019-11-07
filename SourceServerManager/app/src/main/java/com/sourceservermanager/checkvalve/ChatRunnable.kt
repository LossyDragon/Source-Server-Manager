package com.sourceservermanager.checkvalve

/*
 * Copyright 2010-2016 by David A. Parker <parker.david.a@gmail.com>
 *
 * This file is part of CheckValve, an HLDS/SRCDS query app for Android.
 *
 * CheckValve is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation.
 *
 * CheckValve is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the CheckValve source code.  If not, see
 * <http://www.gnu.org/licenses/>.
 */

import android.os.Handler
import android.os.Message
import android.util.Log
import com.sourceservermanager.data.Chat
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * This class implements a CheckValve Chat Relay client.
 */

/**
 * Construct a new instance of the Chat class. This class implements a CheckValve Chat Relay client.
 *
 * @param crIP The URL or IP address of the Chat Relay
 * @param crPort The client listen port of the Chat Relay
 * @param crPassword The password for the Chat Relay
 * @param gsIP The URL or IP address of the game server from which you want chat messages
 * @param gsPort The listen port of the game server from which you want chat messages
 * @param h The handler to use
 * @throws UnknownHostException
 */

class ChatRunnable(crIP: String, crPort: String, crPassword: String, gsIP: String, gsPort: String, h: Handler) : Runnable {

    companion object {
        private const val TAG = "ChatRunnable"

        private var responseType: Byte = 0
        private var contentLength: Short = 0
        private var packetHeader: Int = 0
        private var passwordString: String? = null
        private var responseMessage: StringBuilder? = null
        private var s: Socket? = null
        private var msg: Message? = null
        private var `in`: InputStream? = null
        private var out: OutputStream? = null

        // Make sure the data and request buffers have backing arrays
        private val dataBytes = ByteArray(1024)
        private val requestBytes = ByteArray(256)
        private val dataBuffer = ByteBuffer.wrap(dataBytes)
        private val requestBuffer = ByteBuffer.wrap(requestBytes)

        // Fields set by the constructor
        private var chatRelayPort: Int = 0
        private lateinit var gameServerIP: String
        private lateinit var gameServerPort: String
        private lateinit var chatRelayPassword: String
        private lateinit var chatRelayIP: InetAddress
        private lateinit var handler: Handler

        private const val PACKET_HEADER = -0x1
        private const val PTYPE_IDENTITY_STRING = 0x00.toByte()
        private const val PTYPE_HEARTBEAT = 0x01.toByte()
        private const val PTYPE_CONNECTION_REQUEST = 0x02.toByte()
        private const val PTYPE_CONNECTION_FAILURE = 0x03.toByte()
        private const val PTYPE_CONNECTION_SUCCESS = 0x04.toByte()
        private const val PTYPE_MESSAGE_DATA = 0x05.toByte()
    }

    init {
        chatRelayIP = InetAddress.getByName(crIP)
        chatRelayPort = Integer.parseInt(crPort)
        chatRelayPassword = crPassword
        gameServerIP = gsIP
        gameServerPort = gsPort
        handler = h
    }

    override fun run() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)

        try {
            runChatRelayClient()
            handler.sendEmptyMessage(255)
        } catch (e: Exception) {
            val ste = e.stackTrace

            Log.i(TAG, "$this caught an exception: $e")
            Log.i(TAG, "Stack trace:")

            for (x in ste)
                Log.i(TAG, "    $x")

            handler.sendEmptyMessage(-2)
            return
        } finally {
            if (s != null) {
                if (!s!!.isClosed) {
                    try {
                        s!!.close()
                        Log.i(TAG, "The socket has been closed.")
                    } catch (ioe: IOException) {
                        Log.w(TAG, "Caught an exception while shutting down the socket:", ioe)
                    }

                }
            }

            Log.i(TAG, "Chat Relay client thread is shutting down.")
        }
    }

    /**
     * Establishes a client connection to the Chat Relay and then listens for incoming data.
     *
     *
     * **This method should not be explicitly called. It is started when the <tt>start()</tt> method is called on the
     * Chat object or its thread.**
     *
     * @throws Exception
     */
    @Throws(Exception::class)
    fun runChatRelayClient() {
        Log.d(TAG, "runChatRelayClient called")
        dataBuffer.order(ByteOrder.LITTLE_ENDIAN)
        dataBuffer.clear()

        requestBuffer.order(ByteOrder.LITTLE_ENDIAN)
        requestBuffer.clear()

        passwordString = "P "

        if (chatRelayPassword.isNotEmpty()) passwordString += chatRelayPassword

        // Build the connection request packet
        requestBuffer.putInt(PACKET_HEADER)
        requestBuffer.put(PTYPE_CONNECTION_REQUEST)
        requestBuffer.putShort((passwordString!!.length + gameServerIP.length + gameServerPort.length + 3).toShort())
        requestBuffer.put(passwordString!!.toByteArray(charset("UTF-8"))).put(0.toByte())
        requestBuffer.put(gameServerIP.toByteArray(charset("UTF-8"))).put(0.toByte())
        requestBuffer.put(gameServerPort.toByteArray(charset("UTF-8"))).put(0.toByte())
        requestBuffer.flip()

        try {
            s = Socket()
            s!!.soTimeout = 2000
            s!!.sendBufferSize = 1024
            s!!.receiveBufferSize = 1024
            s!!.connect(InetSocketAddress(chatRelayIP, chatRelayPort), 2000)
            `in` = s!!.getInputStream()
            out = s!!.getOutputStream()

            dataBuffer.clear()

            Log.d(TAG, "Waiting for server identity string.")

            // Get the first 5 bytes of the packet data (header and packet type)
            if (`in`!!.read(dataBytes, 0, 5) == -1) return

            Log.d(TAG, "Received a packet.")

            // Make sure the header is valid

            do {
                packetHeader = dataBuffer.int
                Log.w(
                        TAG,
                        "Rejecting packet: invalid header 0x" + String.format("%s", Integer.toHexString(packetHeader)) + "."
                )
                handler.sendEmptyMessage(-1)


            } while (packetHeader != PACKET_HEADER)


            // Get the packet type
            responseType = dataBuffer.get()

            if (responseType == PTYPE_IDENTITY_STRING) {
                Log.d(TAG, "Received server identity string.")

                responseMessage = StringBuilder()

                // Get the content length
                if (`in`!!.read(dataBytes, dataBuffer.position(), 2) == -1) return

                contentLength = dataBuffer.short
                Log.d(TAG, "Content length is $contentLength bytes.")

                // Make sure the content length is valid
                if (contentLength < 1 || contentLength > 1024) {
                    Log.w(TAG, "Packet contained an invalid content length ($contentLength)")
                    handler.sendEmptyMessage(-1)
                    return
                }

                // Read the rest of the packet data
                if (`in`!!.read(dataBytes, dataBuffer.position(), contentLength.toInt()) == -1) return

                dataBuffer.limit(dataBuffer.position() + contentLength)

                while (dataBuffer.hasRemaining())
                    responseMessage!!.append(dataBuffer.get().toChar())

                Log.i(TAG, "Server identity string is " + responseMessage!!.toString().trim { it <= ' ' })

                out!!.write(requestBuffer.array(), requestBuffer.position(), requestBuffer.limit())
                out!!.flush()

                s!!.soTimeout = 60000
            } else {
                Log.w(
                        TAG,
                        "Unexpected packet type 0x" + String.format("%s", responseType.toString()) + "."
                )
                Log.d(TAG, "runChatRelayClient(): Sending -1 to handler.")
                handler.sendEmptyMessage(-1)
                return
            }
        } catch (e: Exception) {
            Log.w(TAG, "Caught an exception:", e)
            handler.sendEmptyMessage(-1)
            return
        }

        if (!s!!.isConnected) {
            Log.d(TAG, "runChatRelayClient(): Socket is not connected; socket=" + s!!.toString())
            handler.sendEmptyMessage(-1)
            return
        }

        while (true) {
            dataBuffer.clear()

            try {
                Log.d(TAG, "Waiting for the next packet.")

                // Get the first 5 bytes of the packet data (header and packet type)
                if (`in`!!.read(dataBytes, 0, 5) == -1) return

                Log.d(TAG, "Received a packet.")
            } catch (ste: SocketTimeoutException) {
                Log.w(TAG, "Timed out while waiting for next packet.")
                continue
            } catch (se: SocketException) {
                Log.d(TAG, "Caught a socket exception.")
                return
            }

            // Make sure the header is valid
            do {
                packetHeader = dataBuffer.int

                Log.w(
                        TAG,
                        "Rejecting packet: invalid header 0x" + String.format("%s", Integer.toHexString(packetHeader)) + "."
                )

            } while (packetHeader != PACKET_HEADER)

            // Get the packet type
            responseType = dataBuffer.get()

            Log.d(TAG, "Packet type is 0x" + String.format("%s", responseType.toString()) + ".")

            // No need to do anything if this is a heartbeat
            if (responseType == PTYPE_HEARTBEAT) {
                //handler.sendEmptyMessage(1);
                continue
            }

            responseMessage = StringBuilder()

            try {
                // Get the content length
                if (`in`!!.read(dataBytes, dataBuffer.position(), 2) == -1) return

                contentLength = dataBuffer.short
                Log.d(TAG, "Content length is $contentLength bytes.")
            } catch (ste: SocketTimeoutException) {
                Log.w(TAG, "Timed out while reading content length.")
                continue
            }

            // Make sure the content length is valid
            if (contentLength < 1 || contentLength > 1024) continue

            try {
                // Read the rest of the packet data
                if (`in`!!.read(dataBytes, dataBuffer.position(), contentLength.toInt()) == -1) return
            } catch (ste: SocketTimeoutException) {
                Log.w(TAG, "Timed out while reading packet data.")
                continue
            }

            dataBuffer.limit(dataBuffer.position() + contentLength)

            when (responseType) {
                PTYPE_CONNECTION_SUCCESS -> {
                    Log.i(TAG, "Connected to " + chatRelayIP.hostAddress + ":" + chatRelayPort.toString() + ".")
                    handler.sendEmptyMessage(4)
                }

                PTYPE_CONNECTION_FAILURE -> {
                    while (dataBuffer.hasRemaining())
                        responseMessage!!.append(dataBuffer.get().toChar())

                    val error = responseMessage!!.substring(2).trim { it <= ' ' }
                    msg = Message.obtain(handler, 3, error)
                    handler.sendMessage(msg!!)

                    Log.i(TAG, "Connection refused by Chat Relay server: $error")
                }

                PTYPE_MESSAGE_DATA -> {
                    val tmp = ByteArray(dataBuffer.remaining())
                    dataBuffer.get(tmp, 0, dataBuffer.remaining())

                    val pd = PacketData(tmp)

                    val chat = Chat(
                            pd.byte,            // Protocol version
                            pd.byte,            // Epoch timestamp from the Chat Relay
                            pd.int,             // say_team flag
                            pd.utF8String!!,    // Game server IP
                            pd.utF8String!!,    // Game server port
                            pd.utF8String!!,    // Timestamp from the original message
                            pd.utF8String!!,    // Player name
                            pd.utF8String!!,    // Player team
                            pd.utF8String!!     // Chat message
                    )

                    msg = Message.obtain(handler, 5, chat)
                    handler.sendMessage(msg!!)
                }

                else -> {
                    Log.w(TAG, "Unknown packet type, re-sending request.")
                    out!!.write(requestBuffer.array(), 0, requestBuffer.position())
                    out!!.flush()
                }
            }
        }
    }

    /**
     * Shuts down the Chat Relay client.
     *
     * This method closes the network socket and then calls <tt>interrupt()<tt> on
     * the Chat object's thread.
     */
    fun shutDown() {
        try {
            Log.d(TAG, "$this: Shutdown was requested.")

            if (s != null) {
                if (!s!!.isClosed) {
                    Log.d(TAG, this.toString() + ": Closing socket " + s!!.toString() + ".")
                    s!!.close()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Caught an exception while closing socket:", e)
            handler.sendEmptyMessage(-2)
        }

        Log.d(TAG, this.toString() + ": Calling interrupt() on " + Thread.currentThread().toString())
        Thread.currentThread().interrupt()
    }
}