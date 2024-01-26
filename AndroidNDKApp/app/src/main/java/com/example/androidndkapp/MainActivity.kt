package com.example.androidndkapp

import android.Manifest
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.ActionListener
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.provider.Settings
import android.provider.SyncStateContract.Constants
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.androidndkapp.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.lang.ref.WeakReference
import java.lang.reflect.InvocationTargetException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.minutes


//public class FibonacciResult {}
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val intentFilter = IntentFilter()
    private val scope = MainScope()

    private var myP2pName:String = ""
    private lateinit var channel: WifiP2pManager.Channel
    private lateinit var manager: WifiP2pManager
    private lateinit var receiver:BroadcastReceiver
    //    private val peers = mutableListOf<WifiP2pDevice>()
    private var isWifiP2pEnabled = false
    private var isP2PDeviceReady = false

    private var udpSocket: UdpSocket? = null
    private lateinit var handler: Handler
    init {
        val outerClass = WeakReference(this)
        handler = MyHandler(outerClass)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val android_device_id = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        Log.d(TAG, "Android Device ID="+android_device_id)
        Log.d(TAG, "SDK Version="+android.os.Build.VERSION.SDK_INT)
        myP2pName = "AIWINGS_"+android_device_id.takeLast(5)

        // Indicates a change in the Wi-Fi Direct status.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        // Indecates a change in the list of available peers.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        // Indecates the state of Wi-Fi Direct connectivity has changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        // Indicates this device's details have changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)

//        manager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
//        channel = manager.initialize(this, mainLooper, null)
        if (!initP2p()) {
            Toast.makeText(this, "Wi-Fi Driect is not enabled.",
                Toast.LENGTH_SHORT).show();
            Log.d(TAG, "initP2P failed")
            finish();
        }

        setP2pDeviceName()


        udpSocket = UdpSocket(handler)
        udpSocket?.startUDPSocket()


//        val timerIntent = TimerUserCase(scope)
//        timerIntent.toggleTime(20)
        scope.launch {
            var stop = false
            delay(100)
            while (!stop) {

                Log.d(TAG, "---scope.launch---\n isWifiP2pEnabled:"+isWifiP2pEnabled)

                if (isWifiP2pEnabled) {
                    // Scan peers
                    discoverPeers()
                }
                delay(2.minutes)   // 2 minutes
            }
        }

        // Example of a call to a native method
        /*
        val fibonacciIndex = 9
        val res = computeFibonacciNative(fibonacciIndex)
        binding.displayText.text = res.toString()
        */
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    /** register the BroadcastReceiver with the intent values to be matched **/
    public override fun onResume() {
        super.onResume()
        var receiver = WifiDirectBroadcastReceiver(manager, channel, this)
        registerReceiver(receiver, intentFilter)
    }
    public override fun onPause() {
        Log.d(TAG, "---onPause---")
        super.onPause()
        unregisterReceiver(receiver)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id: Int = item.itemId
        if(id == R.id.disconnection) {
            manager.removeGroup(channel, object:WifiP2pManager.ActionListener{
                override fun onSuccess() {
                    Log.d(TAG, "onOptionsItemSelected removeGroup onSuccess")
                }
                override fun onFailure(reason: Int) {
                    Log.d(TAG, "onOptionsItemSelected removeGroup onFailure")
                }

            })
//            createGroup()
        } else if(id == R.id.sendmessage) {
            Log.d(TAG, "connectedGroupAddress:" + connectedGroupAddress)
            if (connectedGroupAddress!=null) {
                var addr = getBroadcastAddress()
                Log.d(TAG, "broadcast address:" + addr)
                var msg = "Hi, it's "+ LocalDateTime.now() + ",\n from " + myP2pName + "."
                udpSocket!!.sendMessage(addr, msg)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    // Declare the Handler as a static class.
    class MyHandler(private val outerClass: WeakReference<MainActivity>) : Handler() {
        override fun handleMessage(msg: Message) {
//            outerClass.get()?.tv_receive?.append(msg?.obj.toString()+"\n")
            Log.d(TAG, "MyHandle - handleMessage:"+msg)
            outerClass.get()?.updateMessageTextView(msg.obj.toString())

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        udpSocket?.stopUDPSocket()
    }

    /////////////////////////////////////////////

    fun setMyP2pName(name:String) {
        this.myP2pName = name
        updateDeviceTextView(myP2pName)
    }
    fun setIsWifiP2pEnabled(isWifiP2pEnabled: Boolean) {
        Log.d(TAG, "setIsWifiP2pEnabled "+isWifiP2pEnabled)
        this.isWifiP2pEnabled = isWifiP2pEnabled
    }
    fun isWifiP2pEnabled(): Boolean {
        return this.isWifiP2pEnabled
    }

    private fun initP2p(): Boolean {
        Log.d(TAG, "---initP2p start---")
        // Device capability definition check
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT)) {
            Log.e(TAG, "Wi-Fi Direct is not supported by this device.")
            return false
        }
        // Hardware capability check
        val wifiManager = getSystemService(WIFI_SERVICE) as WifiManager
        if (wifiManager == null) {
            Log.e(TAG, "Cannot get Wi-Fi system service.")
            return false
        }
        if (!wifiManager.isP2pSupported) {
            Log.e(TAG, "Wi-Fi Direct is not supported by the hardware or Wi-Fi is off.")
            return false
        }

        // Check permission of NEARBY_WIFI_DEVICES, which is necessary to Android 13+
//            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.GINGERBREAD) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.NEARBY_WIFI_DEVICES
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.d(TAG, "ACCESS_FINE_LOCATION or NEARBY_WIFI_DEVICES permission denied")
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return false
            }
        }

        // Implement the WifiP2pManager object
        manager = getSystemService(WIFI_P2P_SERVICE) as WifiP2pManager
        if (manager == null) {
            Log.e(TAG, "Cannot get Wi-Fi Direct system service.")
            return false
        }
        // Initialize the Wi-Fi Direct
        channel = manager.initialize(this, mainLooper, null)
        if (channel == null) {
            Log.e(TAG, "Cannot initialize Wi-Fi Direct.")
            return false
        }

        // Initialize deviceListListener
        deviceListListener.initialize(manager, channel, this)



        Log.d(TAG, "---initP2p end---")
        return true
    }

//    fun getRandomString(length: Int) : String {
//        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
//        return (1..length)
//            .map { allowedChars.random() }
//            .joinToString("")
//    }

    fun setP2pDeviceName() {
        try {
            val paramTypes: Array<Class<*>?> = arrayOfNulls(3)
            paramTypes[0] = WifiP2pManager.Channel::class.java
            paramTypes[1] = String::class.java
            paramTypes[2] = WifiP2pManager.ActionListener::class.java
            val setDeviceName = manager.javaClass.getMethod(
                "setDeviceName", *paramTypes
            )
            val argList = arrayOfNulls<Any>(3)
            argList[0] = channel
            argList[1] = myP2pName
            argList[2] = object : ActionListener {
                override fun onSuccess() {
                    Log.d(TAG,"setP2pDeviceName onSuccess")

                    updateDeviceTextView(myP2pName)
                }
                override fun onFailure(reason: Int) {
                    Log.d(TAG,"setP2pDeviceName onFailure:"+reason+" - "+myP2pName)
                    updateDeviceTextView("unknown ("+myP2pName+")")
                }
            }
            setDeviceName.isAccessible = true
            setDeviceName.invoke(manager, *argList)
        } catch (e: NoSuchMethodException) {
            Log.d(TAG,"setP2pDeviceName NoSuchMethodException")
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            Log.d(TAG,"setP2pDeviceName IllegalAccessException")
            e.printStackTrace()
        } catch (e: IllegalArgumentException) {
            Log.d(TAG,"setP2pDeviceName IllegalArgumentException")
            e.printStackTrace()
        } catch (e: InvocationTargetException) {
            Log.d(TAG,"setP2pDeviceName InvocationTargetException")
            e.printStackTrace()
        }
    }

    fun discoverPeers() {
        Toast.makeText(this@MainActivity, "discovering peers",
            Toast.LENGTH_SHORT).show();

        manager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "discoverPeers - onSuccess")
                manager?.requestPeers(channel, MainActivity.deviceListListener)

            }
            override fun onFailure(reasonCode: Int) {
                Log.d(TAG, "discoverPeers - onFailure:"+reasonCode)
            }
        })
    }

    fun updateStatusTextView(t: String?) {
        runOnUiThread {
            binding.statusText.text =t
        }
    }
    fun updateDisplayTextView(t: String?) {
        runOnUiThread {
            binding.displayText.text =t
        }
    }
    fun updateDeviceTextView(t: String?) {
        runOnUiThread {
            binding.deviceText.text =t
        }
    }
    fun updateMessageTextView(t: String?) {
        runOnUiThread {
            binding.messageText.text =t
        }
    }

//    fun sendBroadcast(messageStr: String) {
//        val policy = ThreadPolicy.Builder().permitAll().build()
//        StrictMode.setThreadPolicy(policy)
//        try {
//            val socket = DatagramSocket()
//            socket.broadcast = true
//            val sendData = messageStr.toByteArray()
//            val sendPacket =
//                DatagramPacket(sendData, sendData.size, getBroadcastAddress(), Constants.PORT)
//            socket.send(sendPacket)
//            println(javaClass.name + "Broadcast packet sent to: " + getBroadcastAddress().hostAddress)
//        } catch (e: IOException) {
//            Log.e(TAG, "IOException: " + e.message)
//        }
//    }

    @Throws(IOException::class)
    fun getBroadcastAddress(): InetAddress {
        /*
        val wifi = baseContext.getSystemService(WIFI_SERVICE) as WifiManager
        val dhcp = wifi.dhcpInfo

        val broadcast = dhcp.ipAddress and dhcp.netmask or dhcp.netmask.inv()
        val quads = ByteArray(4)
        for (k in 0..3) quads[k] = (broadcast shr k * 8 and 0xFF).toByte()

         */
        var address = connectedGroupAddress.toString()
        Log.d(TAG, "getBroadcastAddress - connectedGroupAddress="+ address)
        address = address.substring(0,address.lastIndexOf('.')+1)
        address = address.substring(1,address.lastIndexOf('.')+1)+"255"

        return InetAddress.getByName(address)
    }

    /**
     * A native method that is implemented by the 'androidndkapp' native library,
     * which is packaged with this application.
     */
//    external fun stringFromJNI(): String
//
    suspend fun computeFibonacci(argument: Int): Int {
        return withContext(Dispatchers.Default) {
            computeFibonacciNative(argument)
        }
    }
    private external fun computeFibonacciNative(argument: Int): Int

    companion object {
        val TAG: String = "wifidirect"
//        var thisDeviceReady = false
//        var isConnected = false
//        var serverCreated = false
        var deviceListListener = DeviceListListener()
//        var peers = mutableListOf<WifiP2pDevice>()
        var connectedList = ArrayList<String>()
        var connectedGroupAddress: InetAddress? = null
        // Used to load the 'androidndkapp' library on application startup.
        init {
            System.loadLibrary("androidndkapp")
        }


        //    lateinit var deviceNameArray: Array<String?>
//        lateinit var deviceArray: Array<WifiP2pDevice?>
//        var peerCount = 0
    }

}