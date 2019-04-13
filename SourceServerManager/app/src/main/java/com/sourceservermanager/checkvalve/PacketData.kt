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

import java.nio.ByteBuffer
import java.nio.ByteOrder
import android.util.Log

/*
 * Define the PacketData class
 */

/**
 * Construct a new instance of the PacketData class.
 * <br></br><br></br>
 * @param data The byte array containing the packet data
 */
class PacketData(data: ByteArray) {

    companion object {
        private const val TAG = "PacketData"
    }

    private val byteBuffer: ByteBuffer = ByteBuffer.wrap(data)

    /**
     * Get the current byte in the packet data and increase the position by 1.
     * <br></br><br></br>
     * @return The next byte in the packet data
     */
    val byte: Byte
        get() = byteBuffer.get()

    /**
     * Returns the int at the current position and increases the position by 4.
     * <br></br><br></br>
     * @return The next 4 bytes of packet data composed into an int
     */
    val int: Int
        get() = byteBuffer.int

    /**
     * Returns the float at the current position and increases the position by 4.
     * <br></br><br></br>
     * @return The next 4 bytes of packet data composed into a float
     */
    val float: Float
        get() = byteBuffer.float

    /**
     * Returns the String beginning at the current position using the default character set.
     * The position is increased to the first byte after the end of the String.
     * <br></br><br></br>
     * @return The String beginning at the current position
     */
    val string: String
        get() {
            var pos = byteBuffer.position()
            var len = 0
            while (byteBuffer.get(pos++) != 0x00.toByte()) len++
            val tmpArray = ByteArray(len)
            byteBuffer.get(tmpArray, 0, tmpArray.size)
            byteBuffer.get()

            return String(tmpArray)
        }

    /**
     * Returns the String beginning at the current position using the UTF-8 character set.
     * The position is increased to the first byte after the end of the String.
     * <br></br><br></br>
     * This method is equivalent to <tt>getString("UTF-8")</tt>.
     * <br></br><br></br>
     * @return The UTF-8 encoded String beginning at the current position, or <tt>null</tt> if an exception occurs
     */
    val utF8String: String?
        get() {

            var pos = byteBuffer.position()
            var len = 0

            while (byteBuffer.get(pos++) != 0x00.toByte()) len++

            val tmpArray = ByteArray(len)
            byteBuffer.get(tmpArray, 0, tmpArray.size)
            byteBuffer.get()

            return try {
                String(tmpArray)
            } catch (e: Exception) {
                Log.w(TAG, "getUTF8String(): Caught an exception:", e)
                null
            }
        }

    /**
     * Get the current position within the packet data.
     * <br></br><br></br>
     * @return The value of the buffer's current position
     */
    /**
     * Sets the position within the packet data.
     * <br></br><br></br>
     * @param newPosition The new position; must not be negative or greater than the length of the packet data
     */
    var position: Int
        get() = byteBuffer.position()
        set(newPosition) {
            byteBuffer.position(newPosition)
        }

    /**
     * Get the byte order currently used by the packet data buffer.
     * <br></br><br></br>
     * @return A <tt>ByteOrder</tt> object representing the byte order used by the buffer
     */
    init {
        this.byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
    }

    /**
     * Returns the String beginning at the current position using the specified character set.
     * The position is increased to the first byte after the end of the String.
     * <br></br><br></br>
     * @param characterSet The name of the character set to use
     * @return The String beginning at the current position, or <tt>null</tt> if an exception occurs
     */
    fun getString(characterSet: String): String? {
        var pos = byteBuffer.position()
        var len = 0
        while (byteBuffer.get(pos++) != 0x00.toByte()) len++
        val tmpArray = ByteArray(len)
        byteBuffer.get(tmpArray, 0, tmpArray.size)
        byteBuffer.get()

        return try {
            String(tmpArray)
            //String(tmpArray, Charset.forName("UTF-8"))
        } catch (e: Exception) {
            Log.w(TAG, "getString(): Caught an exception:", e)
            null
        }
    }

    /**
     * Determine whether the packet data buffer has data remaining.
     * <br></br><br></br>
     * @return <tt>true</tt> if there is data remaining, <tt>false</tt> if the current position is at the end of the data.
     */
    fun hasRemaining(): Boolean {
        return byteBuffer.hasRemaining()
    }

    /**
     * Skip the String beginning at the current position.  The position is increased to the first byte
     * after the end of the String.
     */
    fun skipString() {
        while (byteBuffer.hasRemaining() && byteBuffer.get() != 0x00.toByte()) {
        }
    }

    /**
     * Move the position in the buffer forward.  The position is increased by <tt>num</tt>.
     * <br></br><br></br>
     * @param num The number of bytes to skip.
     */
    fun skip(num: Int) {
        byteBuffer.position(byteBuffer.position() + num)
    }
}