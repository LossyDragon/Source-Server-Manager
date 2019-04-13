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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Handler
import android.util.Log

/**
 * This class implements a <tt>BroadcastReceiver</tt> listening for network events, and sends messages to the caller via
 * a <tt>Handler</tt> when events occur.
 */

/**
 * Construct a new instance of the NetworkEventReceiver class.
 *
 * This class implements a <tt>BroadcastReceiver</tt> listening for network events, and sends messages to the caller
 * via a <tt>Handler</tt> when events occur.
 *
 * @param context The context to use
 * @param handler The handler to use
 */

class NetworkEventReceiver(private val context: Context, private val handler: Handler) : Runnable {

    companion object {
        private const val TAG = "NetworkEventReceiver"
    }

    /**
     * Determines if the BroadcastReceiver is currently registered.
     *
     * @return A boolean value indicating whether or not the receiver is registered.
     */
    private var isRegistered: Boolean = false
    private var connected: Boolean = false
    private var lastNetworkType: Int = 0

    /**
     * Gets a copy of the receiver.
     *
     * @return The receiver as a BroadcastReceiver object
     */
    var receiver: BroadcastReceiver? = null
        private set
    private var filter: IntentFilter? = null
    private var event: Int = 0

    init {
        Log.d(TAG, "init")
        event = 0
    }

    override fun run() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)

        isRegistered = false
        connected = false
        lastNetworkType = 0
        filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)

        Log.i(TAG, "Starting network event receiver.")

        try {
            receiver = object : BroadcastReceiver() {
                override fun onReceive(x: Context, i: Intent) {
                    Log.i(TAG, "A network event has been received (event #$event).")

                    // Determine whether connectivity has been completely lost
                    if (i.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false)) {
                        Log.w(TAG, "Network connectivity has been lost.")
                        handler.sendEmptyMessage(-1)
                        connected = false
                    } else {
                        val c = x.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                        val n = c.activeNetworkInfo

                        if (n == null) {
                            Log.d(TAG, "ConnectivityManager.getActiveNetworkInfo() is null.")
                            Log.d(TAG, "No active network connections exist.")
                        } else {
                            val state: String

                            when (n.state) {
                                NetworkInfo.State.CONNECTING -> {
                                    state = "Connecting"
                                    connected = false
                                }
                                NetworkInfo.State.CONNECTED -> {
                                    state = "Connected"
                                    connected = true
                                }
                                NetworkInfo.State.DISCONNECTING -> {
                                    state = "Disconnecting"
                                    connected = false
                                }
                                NetworkInfo.State.DISCONNECTED -> {
                                    state = "Disconnected"
                                    connected = false
                                }
                                NetworkInfo.State.SUSPENDED -> {
                                    state = "Suspended"
                                    connected = false
                                }
                                NetworkInfo.State.UNKNOWN -> {
                                    state = "Unknown"
                                    connected = false
                                }
                                else -> {
                                    state = "Other"
                                    connected = false
                                }
                            }

                            val type = n.type
                            val typeName = n.typeName
                            val available = if (n.isAvailable) "true" else "false"

                            Log.i(TAG, "[receiver=" + receiver!!.hashCode() + "][event=" + event + "] TYPE: " + typeName + " (" + type + ")")
                            Log.i(TAG, "[receiver=" + receiver!!.hashCode() + "][event=" + event + "] STATE: " + state)
                            Log.i(TAG, "[receiver=" + receiver!!.hashCode() + "][event=" + event + "] AVAILABLE: " + available)

                            // The first event (0) will always be received just after the receiver
                            // is registered, so we'll ignore it and only notify the parent thread
                            // about events thereafter.

                            //TODO [[hack]] to start the ChatThread. Comment above is always 0
                            if(connected) {
                                handler.sendEmptyMessage(1)
                            }

                            if (event == 0) {
                                lastNetworkType = type
                            } else {
                                if (connected) {
                                    if (type != lastNetworkType) {
                                        lastNetworkType = type
                                        handler.sendEmptyMessage(1)
                                    } else {
                                        Log.d(TAG, "Ignoring event #$event (duplicate)")
                                    }
                                }
                            }

                        }
                    }

                    event++
                }
            }

            registerReceiver()
        } catch (e: Exception) {
            Log.w(TAG, "run(): Caught an exception:", e)
            unregisterReceiver()
            handler.sendEmptyMessage(-2)
            Log.i(TAG, "Shutting down network event receiver thread.")
            return
        }
    }

    /**
     * Shuts down the NetworkEventReceiver. This method simply calls the <tt>interrupt()<tt>
     * method on the NetworkEventReceiver object's thread.
     */
    fun shutDown() {
        Log.i(TAG, "Shutdown was requested; calling interrupt() on this thread.")
        Thread.currentThread().interrupt()
    }

    /**
     * Registers the BroadcastReceiver if it is not already registered.
     */
    private fun registerReceiver() {
        if (!isRegistered) {
            try {
                Log.i(TAG, "Registering broadcast receiver.")
                context.registerReceiver(receiver, filter)

                Log.i(TAG, "Resetting event counter.")
                event = 0

                isRegistered = true
            } catch (e: Exception) {
                Log.w(TAG, "registerReceiver(): Caught an exception:", e)
                Log.w(TAG, "Failed to register broadcast receiver.")
            }

        }
    }

    /**
     * Unregisters the BroadcastReceiver if it is currently registered.
     */
    fun unregisterReceiver() {
        if (isRegistered) {
            try {
                Log.i(TAG, "Unregistering broadcast receiver.")
                context.unregisterReceiver(receiver)
                isRegistered = false
            } catch (e: Exception) {
                Log.w(TAG, "unregisterReceiver(): Caught an exception:", e)
                Log.w(TAG, "Failed to unregister broadcast receiver.")
            }

        }
    }

}