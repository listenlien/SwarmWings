package com.example.androidndkapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pGroup
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi


class WifiDirectBroadcastReceiver(
    private val manager: WifiP2pManager,
    private val channel: WifiP2pManager.Channel,
    activity: MainActivity
): BroadcastReceiver() {
    private val activity: MainActivity = activity

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onReceive(context: Context, intent: Intent) {
//        Log.d(MainActivity.TAG, "WifiDirectBroadcastReceiver.onReceive")
        when (intent.action) {
            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                // Determine if Wi-Fi Direct mode is enabled or not, alert the Activity.
                val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, 0)
                Log.d(MainActivity.TAG, "WIFI_P2P_STATE_CHANGED_ACTION:" + state)
                if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                    // Wifi Direct mode is enabled
                    activity.setIsWifiP2pEnabled(true)
                    activity.updateStatusTextView("Wi-Fi Driect is enabled.")

                } else {
                    // Wifi Direct mode is disabled
                    activity.setIsWifiP2pEnabled(false)
                    activity.updateStatusTextView("Wi-Fi Driect is disabled.")
//                    activity.resetData();
                }

            }

            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                // Unable to receive this intent in SDK_VERSION 34, if user doesn't touch the APP.
                Log.d(MainActivity.TAG, "WIFI_P2P_PEERS_CHANGED_ACTION")
                manager?.requestPeers(channel, MainActivity.deviceListListener)
            }

            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                // Unable to receive this intent in SDK_VERSION 34, if user doesn't touch the APP.
                Log.d(MainActivity.TAG, "WIFI_P2P_CONNECTION_CHANGED_ACTION")

                if (activity.isWifiP2pEnabled()) {
//                    manager.requestConnectionInfo(
//                        channel,
//                        object : WifiP2pManager.ConnectionInfoListener {
//                            override fun onConnectionInfoAvailable(p2pInfo: WifiP2pInfo?) {
//                                Log.d(MainActivity.TAG, "onConnectionInfoAvailable - info:\n" + p2pInfo.toString())
//                                if (p2pInfo?.groupFormed == true) {
//                                    manager.requestGroupInfo(
//                                        channel,
//                                        object : WifiP2pManager.GroupInfoListener {
//                                            override fun onGroupInfoAvailable(group: WifiP2pGroup?) {
//                                                Log.d(MainActivity.TAG, "onGroupInfoAvailable - info:\n" + group?.passphrase + "\n" + group.toString())
//                                                if (p2pInfo.isGroupOwner) {
//                                                    activity.updateDisplayTextView(
//                                                        "Network:"+group?.networkName+"\n"
//                                                                +"GO: this\n"
//                                                                +"Owner address:"+p2pInfo.groupOwnerAddress)
//                                                } else {
//                                                    activity.updateDisplayTextView(
//                                                        "Network: "+group?.networkName+"\n"
//                                                                +"Group Owner: "+group?.owner?.deviceName+"\n"
//                                                                +"Owner address: "+p2pInfo.groupOwnerAddress)
//                                                }
//                                            }
//                                        })
//                                } else {
////                                    activity.updateDisplayTextView("No connection")
//                                    //                           createGroup() // As a AP
//                                }
//                            }
//                        })
                }

            }

            WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION -> {
                Log.d(MainActivity.TAG, "WIFI_P2P_DISCOVERY_CHANGED_ACTION")
            }

            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                // Unable to receive this intent in SDK_VERSION 34, if user doesn't touch the APP.
                Log.d(MainActivity.TAG, "WIFI_P2P_THIS_DEVICE_CHANGED_ACTION")

                if (activity.isWifiP2pEnabled()) {
                    // Get this device information
                    val thisDevice = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(
                            WifiP2pManager.EXTRA_WIFI_P2P_DEVICE,
                            WifiP2pDevice::class.java
                        )
                    } else {
                        intent.getParcelableExtra<WifiP2pDevice>(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE)
                    }

                    if (thisDevice!= null)
                        activity.setMyP2pName(thisDevice?.deviceName.toString())
                }

            }
        }
    }
}