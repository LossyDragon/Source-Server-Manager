package com.sourceservermanager.rcon

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.nio.ByteBuffer
import java.nio.ByteOrder

import com.sourceservermanager.rcon.exception.BadRcon
import com.sourceservermanager.rcon.exception.ResponseEmpty

/**
 * User: oscahie (aka PiTaGoRaS)<br></br>
 * Date: 03-jan-2005<br></br>
 * Time: 19:11:40<br></br>
 * version: 0.4<br></br>
 * Rcon library for Source Engine based games<br></br>
 */
object SourceRcon {

    private const val SERVERDATA_EXECCOMMAND = 2
    private const val SERVERDATA_AUTH = 3
    private const val SERVERDATA_RESPONSE_VALUE = 0
    private const val SERVERDATA_AUTH_RESPONSE = 2

    private const val RESPONSE_TIMEOUT = 2000
    private const val MULTIPLE_PACKETS_TIMEOUT = 300

    private var rconSocket: Socket? = null
    private var `in`: InputStream? = null
    private var out: OutputStream? = null

    private var listenerSocket: Socket? = null
    private var listenerIn: InputStream? = null
    private var listenerOut: OutputStream? = null

    // Get IP regardless of WiFi or 3G
    // Just going to return null anyways
    private val localIpAddress: String?
        get() {
            try {
                val en = NetworkInterface.getNetworkInterfaces()
                while (en.hasMoreElements()) {
                    val intf = en.nextElement()
                    val enumIpAddr = intf.inetAddresses
                    while (enumIpAddr.hasMoreElements()) {
                        val inetAddress = enumIpAddr.nextElement()
                        if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                            return inetAddress.getHostAddress().toString()
                        }
                    }
                }
            } catch (ex: SocketException) {
            }

            return null
        }

    /*HttpClient httpclient = new DefaultHttpClient();
            HttpGet httpget = new HttpGet("http://api.externalip.net/ip");
            // HttpGet httpget = new HttpGet("http://whatismyip.com.au/");
            // HttpGet httpget = new HttpGet("http://www.whatismyip.org/");
            //HttpResponse response;

            //response = httpclient.execute(httpget);

            HttpResponse execute = httpclient.execute(httpget);
            InputStream content = execute.getEntity().getContent();

            BufferedReader buffer = new BufferedReader(
                    new InputStreamReader(content));
            String s = "";
            while ((s = buffer.readLine()) != null) {
            	extIP += s;
            }*///Log.i("externalip",response.getStatusLine().toString());
    //Log.d("SSM", "EXT IP:" + extIP);
    val externalIpAddress: String
        get() {
            var extIP = ""
            try {

            } catch (e: Exception) {
                extIP = "error: " + e.message
            }

            return extIP
        }


    /**
     * Send the RCON command to the game server (must have been previously authed with the correct rcon_password)
     *
     * @param ipStr     The IP (as a String) of the machine where the RCON command will go.
     * @param port      The port of the machine where the RCON command will go.
     * @param password  The RCON password.
     * @param command   The RCON command (without the rcon prefix).
     * @return The response text from the server after trying the RCON command.
     * @throws SocketTimeoutException when there is any problem communicating with the server.
     */
    @Throws(SocketTimeoutException::class, BadRcon::class, ResponseEmpty::class)
    fun send(ipStr: String, port: Int, password: String, command: String, timeout: String): String? {
        return send(ipStr, port, password, command, 0, timeout)
    }

    /**
     * Send the RCON command to the game server (must have been previously authed with the correct rcon_password)
     *
     * @param ipStr     The IP (as a String) of the machine where the RCON command will go.
     * @param port      The port of the machine where the RCON command will go.
     * @param password  The RCON password.
     * @param command   The RCON command (without the rcon prefix).
     * @param localPort The port of the local machine to use for sending out the RCON request.
     * @return The response text from the server after trying the RCON command.
     * @throws SocketTimeoutException when there is any problem communicating with the server.
     */
    @Throws(SocketTimeoutException::class, BadRcon::class, ResponseEmpty::class)
    fun send(ipStr: String, port: Int, password: String, command: String, localPort: Int, rconTimeout: String): String? {
        var response = ""

        try {
            rconSocket = Socket()

            // getLocalIpAddress() will get IP regardless of WiFi or 3G
            val addr = InetAddress.getByName(localIpAddress)
            val ipAddr = addr.address
            val inetLocal = InetAddress.getByAddress(ipAddr)

            //rconSocket.setReuseAddress(true);
            rconSocket!!.bind(InetSocketAddress(inetLocal, localPort))
            rconSocket!!.connect(InetSocketAddress(ipStr, port), Integer.parseInt(rconTimeout) * 1000)

            out = rconSocket!!.getOutputStream()
            `in` = rconSocket!!.getInputStream()

            rconSocket!!.soTimeout = Integer.parseInt(rconTimeout) * 1000

            if (rconAuth(password)) {

                // We are now authed
                val resp: Array<ByteBuffer?>? = sendCommand(command)
                // Close socket handlers, we don't need them more
                out!!.close()
                `in`!!.close()
                rconSocket!!.close()
                if (resp != null) {
                    response = assemblePackets(resp)
                    if (response.isEmpty()) {
                        //throw new ResponseEmpty();
                    }
                }
                /*
            	byte[] request = constructPacket(2, SERVERDATA_EXECCOMMAND, command);
            	out.write(request);


            	while (true)
		        {
            		ByteBuffer[] resp = getData();
		        	response = assemblePackets(resp);
		        	if (response.length() > 0)
		        	{
		        		System.out.println(response);
		        		//Toast.makeText(currContext, response, Toast.LENGTH_SHORT).show();
		        	}
	            }
	            */
            } else {
                throw BadRcon()
            }
        } catch (timeout: SocketTimeoutException) {
            throw timeout
        } catch (e: UnknownHostException) {
            return "UnknownHostException: " + e.cause
        } catch (e: IOException) {
            return "Couldn't get I/O for the connection: " + e.cause
        } catch (e: Exception) {
            //e.printStackTrace();
        }

        return response
    }

    @Throws(Exception::class)
    fun rconListener(ipStr: String, port: Int, password: String, filterList: Array<String>?): String {
        var socket: DatagramSocket? = null
        /*Socket socket = null;
    	DataOutputStream dataOutputStream = null;
    	DataInputStream dataInputStream = null;*/
        try {

            /*
    		socket = new Socket(ipStr, port);
			dataOutputStream = new DataOutputStream(socket.getOutputStream());
			dataInputStream = new DataInputStream(socket.getInputStream());
			dataOutputStream.writeUTF("hello");
			*/


            /*
    		InetAddress serverAddr = InetAddress.getByName(ipStr);
            DatagramChannel channel = DatagramChannel.open();
            socket = channel.socket();

            //socket = new DatagramSocket();
            socket.setReuseAddress(true);

            //InetSocketAddress ia = new InetSocketAddress("localhost", SERVERPORT);
            InetSocketAddress sa = new InetSocketAddress(8080);
            socket.bind(sa);
            DatagramPacket holepunh = new DatagramPacket(new byte[]{0,1,2,3},4, serverAddr, port);
            socket.send(holepunh);

            // create a buffer to copy packet contents into
            byte[] buf = new byte[1500];
            // create a packet to receive

            DatagramPacket packet = new DatagramPacket(buf, buf.length);

            //Log.d("UDP", "***Waiting on packet!");
            socket.setSoTimeout(10000);
            // wait to receive the packet
            socket.receive(packet);
            */

            /*
             * START MEGA IGNORE
             */


            // Retrieve the ServerName
            //InetAddress serverAddr = InetAddress.getByName(ipStr);

            //Log.d("UDP", "S: Connecting...");
            // Create new UDP-Socket
            //DatagramSocket socket = new DatagramSocket(port, serverAddr);
            socket = DatagramSocket(port)

            // By magic we know, how much data will be waiting for us
            val message = ByteArray(1500)
            // Prepare a UDP-Packet that can
            // contain the data we want to receive
            val packet = DatagramPacket(message, message.size)
            //Log.d("UDP", "S: Receiving...");

            // Receive the UDP-Packet
            // Wait 10 seconds before timing out
            socket.soTimeout = 10000
            socket.receive(packet)


            /*
             * END MEGA IGNORE
             */

            //Log.d("UDP", "S: Length: " + packet.getLength());
            //Log.d("UDP", "S: Offset: " + packet.getOffset());
            var tempResp = String(packet.data)
            //String tempResp = dataInputStream.readLine();
            //tempResp = stripGarbage(tempResp);
            //tempResp = tempResp.substring(0, packet.getLength());
            // Remove timestamp
            tempResp = tempResp.substring(tempResp.indexOf(":") + 8)

            //Log.d("UDP", "S: Received: '" + tempResp + "'");
            //Log.d("UDP", "S: Done.");
            socket.close()

            if (filterList != null) {
                for (filter in filterList) {
                    val sayIndex = tempResp.indexOf(filter)
                    if (sayIndex > 0) {
                        val userName = tempResp.substring(1, tempResp.indexOf("<"))
                        val msg = tempResp.substring(tempResp.indexOf("\"", sayIndex + 1) + 1, tempResp.lastIndexOf("\""))

                        return if (filter.contains("say_team")) {
                            "$userName<T>: $msg"
                        } else {
                            "$userName: $msg"
                        }
                    }
                }
            } else {
                return tempResp
            }
        } catch (e: Exception) {
            socket?.close()
            //Log.e("UDP", "S: Error", e);
        }

        return ""
    }

    fun stripGarbage(s: String): String {
        val sb = StringBuilder(s.length)
        for (i in 0 until s.length) {
            val ch = s[i]
            if (ch in ' '..'?' ||
                    ch in 'A'..'Z' ||
                    ch in 'a'..'z' ||
                    ch in '0'..'9') {
                sb.append(ch)
            }
        }
        var cleanString = sb.toString()
        // Exclude timestamp
        // ex: 19:59:56: rcon from 127.0.0.1 ...
        cleanString = cleanString.substring(cleanString.indexOf(":") + 8)

        return cleanString
    }


    @Throws(SocketTimeoutException::class)
    private fun sendCommand(command: String): Array<ByteBuffer?> {

        val request = constructPacket(2, SERVERDATA_EXECCOMMAND, command)

        val resp = arrayOfNulls<ByteBuffer>(128)
        var i = 0
        try {
            out!!.write(request)
            resp[i] = receivePacket(`in`!!)  // First and maybe the unique response packet
            try {
                // We don't know how many packets will return in response, so we'll
                // read() the socket until TimeoutException occurs.
                rconSocket!!.soTimeout = MULTIPLE_PACKETS_TIMEOUT
                while (true) {
                    resp[++i] = receivePacket(`in`!!)
                }
            } catch (e: SocketTimeoutException) {
                // No more packets in the response, go on
                return resp
            }

        } catch (timeout: SocketTimeoutException) {
            // Timeout while connecting to the server
            throw timeout
        } catch (e2: Exception) {
            //System.err.println("I/O error on socket\n");
        }

        return emptyArray()
    }

    /*
    private static ByteBuffer[] getData() throws SocketTimeoutException {
        ByteBuffer[] resp = new ByteBuffer[128];
        int i = 0;
        try {
            resp[i] = receivePacket(in);  // First and maybe the unique response packet
            try {
                // We don't know how many packets will return in response, so we'll
                // read() the socket until TimeoutException occurs.
                rconSocket.setSoTimeout(MULTIPLE_PACKETS_TIMEOUT);
                while (true) {
                    resp[++i] = receivePacket(in);
                }
            } catch (SocketTimeoutException e) {
                // No more packets in the response, go on
                return resp;
            }

        } catch (SocketTimeoutException timeout) {
            // Timeout while connecting to the server
            //throw timeout;
        	return resp;
        } catch (Exception e2) {
            //System.err.println("I/O error on socket\n");
        }
        return null;
    }
     */

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

    @Throws(Exception::class)
    private fun receivePacket(inStream: InputStream): ByteBuffer? {

        val p = ByteBuffer.allocate(4120)
        p.order(ByteOrder.LITTLE_ENDIAN)

        val length = ByteArray(4)

        if (inStream.read(length, 0, 4) == 4) {
            // Now we've the length of the packet, let's go read the bytes
            p.put(length)
            var i = 0
            while (i < p.getInt(0)) {
                p.put(inStream.read().toByte())
                i++
            }
            return p
        } else {
            return null
        }
    }


    private fun assemblePackets(packets: Array<ByteBuffer?>?): String {
        // Return the text from all the response packets together

        var response = ""

        if (packets != null) {
            for (i in packets.indices) {
                if (packets[i] != null) {
                    //String resp = new String(packets[i].array(), 12, packets[i].position()-14);
                    //if (resp != null)
                    //{
                    response += String(packets[i]!!.array(), 12, packets[i]!!.position() - 14)
                    //} else
                    //{
                    //response = response.concat("NULL");
                    //}
                }
            }
        }
        return response
    }


    @Throws(SocketTimeoutException::class)
    private fun rconAuth(rcon_password: String): Boolean {

        val authRequest = constructPacket(1337, SERVERDATA_AUTH, rcon_password)

        var response: ByteBuffer?
        try {
            out!!.write(authRequest)
            response = receivePacket(`in`!!) // junk response packet
            response = receivePacket(`in`!!)

            // Lets see if the received request_id is leet enough ;)
            if (response!!.getInt(4) == 1337 && response.getInt(8) == SERVERDATA_AUTH_RESPONSE) {
                return true
            }
        } catch (timeout: SocketTimeoutException) {
            throw timeout
        } catch (e: Exception) {
            System.err.println("I/O error on socket\n")
        }

        return false
    }

}