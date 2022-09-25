package me.kickboot.rokucontrol

import android.app.Activity
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import me.kickboot.rokucontrol.databinding.ActivityMainBinding
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException

class MainActivity : Activity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        var thread = Thread(Runnable {
            run() {
                this.sendRokuMulticast()
            }
        })
        thread.start()
    }

    private fun sendRokuMulticast(): List<String> {
        val gatewayList = mutableListOf<String>()

        val mSearch = "M-SEARCH * HTTP/1.1\n" +
                "Host: 239.255.255.250:1900\n" +
                "Man: \"ssdp:discover\"\n" +
                "ST: roku:ecp"
        val sendData = mSearch.toByteArray()
        val sendPacket = DatagramPacket(sendData, sendData.size, InetAddress.getByName("239.255.255.250"), 1900)
        var socket: DatagramSocket

        try {
            socket = DatagramSocket()
        } catch (error: Exception) {
            Log.wtf("ROKU", "Failed to open socket")
            return gatewayList
        }

        try {
            socket.send(sendPacket)
        } catch (error: Exception) {
            Log.wtf("ROKU", "Failed to send multicast")
            return gatewayList
        }

        val receivedData = ByteArray(2048)
        val receivePacket = DatagramPacket(receivedData, receivedData.size)
        try {
            socket.receive(receivePacket)
            receivePacket.address?.hostName?.let {
                gatewayList.add(it)
                Log.i("ROKU", "Address received: " + it)
            }
        } catch (socketTimeout: SocketTimeoutException) {
            Log.wtf("ROKU", "Timeout waiting for response")
        } catch (error: Exception) {
            Log.wtf("ROKU", "Failed to receive response")
        } finally {
            socket.close()
        }
        return gatewayList
    }
}