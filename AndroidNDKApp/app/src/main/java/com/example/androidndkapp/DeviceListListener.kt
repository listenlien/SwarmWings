package com.example.androidndkapp

import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pGroup
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.util.Log
import android.widget.Toast

class DeviceListListener : WifiP2pManager.PeerListListener {
//    var peers = mutableListOf<WifiP2pDevice>()


    var manager: WifiP2pManager? = null
    var channel: WifiP2pManager.Channel? = null
    lateinit var activity: MainActivity
//    List<WifiP2pDevice> = ArrayList()
//    private val device: WifiP2pDevice? = null


    fun initialize(m: WifiP2pManager, c: WifiP2pManager.Channel, a:MainActivity) {
        manager = m
        channel = c
        activity = a
    }

    override fun onPeersAvailable(peerList: WifiP2pDeviceList) {
        Log.d(MainActivity.TAG, "DeviceList onPeersAvailable")
//        peers.clear()
//        peers.addAll(peerList.deviceList)
        val deviceList = peerList.getDeviceList()
        if (deviceList.size == 0) {
            activity.updateDisplayTextView("No peers")
            Log.d(MainActivity.TAG, "No devices found")
            MainActivity.connectedList.clear()
            return
        }
//        MainActivity.peers.clear()
        for(peer in deviceList) {
            Log.d(MainActivity.TAG, "DeviceList onPeersAvailable - "+peer.deviceName+" "+getDeviceStatus(peer.status)+" "+peer.deviceAddress)
            if (peer.deviceName.contains("aiwings", ignoreCase = true)) {
                when (peer.status) {

                    WifiP2pDevice.AVAILABLE -> {
                        if(!MainActivity.connectedList.contains(peer.deviceAddress)){

                            val config = WifiP2pConfig().apply {
                                deviceAddress = peer.deviceAddress
                                wps.setup = WpsInfo.PBC
                            }
                            manager?.connect(channel, config, object : WifiP2pManager.ActionListener {
                                override fun onSuccess() {
                                    Log.d(MainActivity.TAG, "connect - onSuccess")

                                    // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
//                        success = true
                                }

                                override fun onFailure(reason: Int) {
                                    Log.d(MainActivity.TAG, "connect - onFailure:"+reason)
                                    Toast.makeText(
                                        activity,
                                        "Connect failed. Retry.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            })
                        }
                    }
                    WifiP2pDevice.CONNECTED -> {
                        MainActivity.connectedList.add(peer.deviceAddress)

                        manager?.requestConnectionInfo(
                            channel,
                            object : WifiP2pManager.ConnectionInfoListener {
                                override fun onConnectionInfoAvailable(p2pInfo: WifiP2pInfo?) {
                                    Log.d(MainActivity.TAG, "onConnectionInfoAvailable - info:\n" + p2pInfo.toString())
                                    if (p2pInfo?.groupFormed == true) {
                                        MainActivity.connectedGroupAddress = p2pInfo.groupOwnerAddress

                                        manager!!.requestGroupInfo(
                                            channel,
                                            object : WifiP2pManager.GroupInfoListener {
                                                override fun onGroupInfoAvailable(group: WifiP2pGroup?) {
                                                    Log.d(MainActivity.TAG, "onGroupInfoAvailable - info:\n" + group?.passphrase + "\n" + group.toString())
                                                    if (p2pInfo.isGroupOwner) {
                                                        activity.updateDisplayTextView(
                                                            "Network:"+group?.networkName+"\n"
                                                                    +"Group Owner: me\n"
                                                                    +"Owner address:"+p2pInfo.groupOwnerAddress)
                                                    } else {
                                                        activity.updateDisplayTextView(
                                                            "Network: "+group?.networkName+"\n"
                                                                    +"Group Owner: "+group?.owner?.deviceName+"\n"
                                                                    +"Owner address: "+p2pInfo.groupOwnerAddress)
                                                    }
                                                }
                                            })
                                    } else {
//                                    activity.updateDisplayTextView("No connection")
                                        //                           createGroup() // As a AP
                                    }
                                }
                            })
                    }

//                    WifiP2pDevice.INVITED -> "Invited"
//                    WifiP2pDevice.FAILED -> "Failed"
//                    WifiP2pDevice.UNAVAILABLE -> "Unavailable"
                }
            }
        }
    }


    //    fun getDevice(): WifiP2pDevice? {
//        return device
//    }
    private fun getDeviceStatus(deviceStatus: Int): String? {
//        Log.d(MainActivity.TAG, "Peer status :$deviceStatus")
        return when (deviceStatus) {
            WifiP2pDevice.AVAILABLE -> "Available"
            WifiP2pDevice.INVITED -> "Invited"
            WifiP2pDevice.CONNECTED -> "Connected"
            WifiP2pDevice.FAILED -> "Failed"
            WifiP2pDevice.UNAVAILABLE -> "Unavailable"
            else -> "Unknown"
        }
    }
}