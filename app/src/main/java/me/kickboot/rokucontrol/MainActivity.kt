package me.kickboot.rokucontrol

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import me.kickboot.rokucontrol.databinding.ActivityMainBinding
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException

class MainActivity : Activity() {

    private lateinit var binding: ActivityMainBinding

    // static channel ids from roku
    private val channelIdNetflix = "12"

    // TODO: make this list dynamic
    private val showIdBadobi = "81154166"
    private val showIdPokemon = "81292753"
    private val showIdPawPatrol = "81277454"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        this.wireButtonToRokuCommand(R.id.btnPlayPause, "keypress/Play")
        this.wireButtonToRokuCommand(R.id.btnPower, "keypress/Power")
        this.wireButtonToRokuCommand(R.id.btnLaunchBadobi, this.getNetflixShow(showIdBadobi))
        this.wireButtonToRokuCommand(R.id.btnLaunchPokemon, this.getNetflixShow(showIdPokemon))
        this.wireButtonToRokuCommand(R.id.btnLaunchPawPatrol, this.getNetflixShow(showIdPawPatrol))
    }

    private fun getNetflixShow(showId: String): String {
        return "launch/$channelIdNetflix?contentID=$showId&mediaType=season"
    }

    private fun wireButtonToRokuCommand(viewId: Int, rokuCommand: String) {
        findViewById<Button>(viewId).setOnClickListener(this.handleSendCommandToRoku(rokuCommand))
    }

    private fun handleSendCommandToRoku(command: String): View.OnClickListener {
        return View.OnClickListener {
            val thread = Thread(Runnable {
                run() {
                    this.postCommandToRoku(command)
                }
            })
            thread.start()
        }
    }

    private fun postCommandToRoku(command: String) {
        val url = "http://192.168.2.18:8060/$command" // TODO: make the target IP dynamic
        val empty = RequestBody.create(null, "")
        val request = Request.Builder().url(url).method("POST", empty).build()
        val client =  OkHttpClient()
        try {
            val response = client.newCall(request).execute()
            if (response.code != 200) {
                Log.wtf("HTTP", "Request to Roku API returned status $response.code")
            }
        } catch (error: Exception) {
            Log.wtf("HTTP", "Failed to send request to Roku API", error)
        }
    }

    // TODO: fix UPNP discovery
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