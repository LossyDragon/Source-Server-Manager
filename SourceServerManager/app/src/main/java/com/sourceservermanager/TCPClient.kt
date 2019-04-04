package com.sourceservermanager

/**
 * Created by Matthew on 2/15/2016.
 */
import android.util.Log
import java.io.*
import java.net.InetAddress
import java.net.Socket

/**
 * Constructor of the class. OnMessagedReceived listens for the messages received from server
 */
class TCPClient(listener: OnMessageReceived) {

    private var serverMessage: String? = null
    private var mMessageListener: OnMessageReceived? = null
    private var mRun = false
    private var mSocket: Socket? = null

    private var out: PrintWriter? = null
    private lateinit var `in`: BufferedReader

    init {
        mMessageListener = listener
    }

    /**
     * Sends the message entered by client to the server
     * @param message text entered by client
     */
    private fun sendMessage(message: String) {
        if (out != null && !out!!.checkError()) {
            out!!.println(message)
            out!!.flush()
        }
    }

    fun stopClient() {
        mRun = false
        try {
            mSocket!!.close()
            mSocket = null
        } catch (e: Exception) {
            Log.e("TCP", "Stopping: Error", e)
        }
    }

    fun run(messageOnConnect: String) {

        mRun = true

        try {
            //here you must put your computer's IP address.
            val serverAddr = InetAddress.getByName(SERVER_IP)

            Log.e("TCP Client", "C: Connecting...")

            //create a socket to make the connection with the server
            mSocket = Socket(serverAddr, SERVER_PORT)
            mSocket!!.soTimeout = 10000

            try {
                //send the message to the server
                out = PrintWriter(BufferedWriter(OutputStreamWriter(mSocket!!.getOutputStream())), true)

                // Send connect message
                sendMessage(messageOnConnect)

                Log.e("TCP Client", "C: Sent.")
                Log.e("TCP Client", "C: Done.")

                //receive the message which the server sends back
                `in` = BufferedReader(InputStreamReader(mSocket!!.getInputStream()))

                //in this while the client listens for the messages sent by the server
                while (mRun) {
                    try {
                        serverMessage = `in`.readLine()
                    } catch (e: Exception) {
                        Log.e("TCP", "readLine: Error", e)
                    }

                    if (serverMessage != null && mMessageListener != null) {
                        //call the method messageReceived from MyActivity class
                        mMessageListener!!.messageReceived(serverMessage!!)
                    }
                    serverMessage = null
                }

                Log.e("RESPONSE FROM SERVER", "S: Received Message: '$serverMessage'")

            } catch (e: Exception) {
                Log.e("TCP", "S: Error", e)
            } finally {
                //the socket must be closed. It is not possible to reconnect to this socket
                // after it is closed, which means a new socket instance has to be created.
                if (mSocket != null) {
                    mSocket!!.close()
                }
            }

        } catch (e: Exception) {
            Log.e("TCP", "C: Error", e)
        }
    }

    //Declare the interface. The method messageReceived(String message) will must be implemented in the MyActivity
    //class at on asynckTask doInBackground
    interface OnMessageReceived {
        fun messageReceived(message: String)
    }

    companion object {
        //public static final String SERVER_IP = "104.236.78.109"; // IHTOAYA-Mini
        const val SERVER_IP = "173.18.137.244"
        const val SERVER_PORT = 27100
    }
}
