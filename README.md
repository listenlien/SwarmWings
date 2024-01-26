# Swarm Wings
## Wi-Fi Direct
- References
    - Wi-Fi Direct Specification, Version 1.9, 2021
    - [Advantages and Disadvantages of Wi-Fi Direct](https://www.profolus.com/topics/advantages-and-disadvantages-of-wi-fi-direct/)
    - [Android Wi-Fi Direct Architecture: From Protocol Implementation to Formal Specification](https://www.ijert.org/android-wi-fi-direct-architecture-from-protocol-implementation-to-formal-specification)

### Android App
- Implementation
    * [Set up application permissions](https://developer.android.com/develop/connectivity/wifi/wifi-direct#permissions)
    * [Set up a broadcast receiver and peer-to-peer manager](https://developer.android.com/develop/connectivity/wifi/wifi-direct#receiver)
        * In SDK version 34, if without clicking a button by the user, only *WIFI_P2P_STATE_CHANGED_ACTION* will be received.
    * Wi-Fi Direct Initialization ([initP2P()](https://android.googlesource.com/platform/development/+/refs/heads/main/samples/WiFiDirectDemo/src/com/example/android/wifidirect/WiFiDirectActivity.java))
    * Set Wi-Fi p2p device name
        * The default Wi-Fi Direct device name is *Android-xxxxx*. We try to change the name to include "AIWINGS" to identify it as a partner.
        * Setting using kotlin reflection fails in SDK version 34, but works in SDK version 28. Renaming can set manually from *設定->網際網路->網路偏好設定->Wi-Fi Direct*.
    * Create a timer using kotlin coroutine to discover peers every 2 minutes.
        * The *discoverPeers* may trigger intent *WIFI_P2P_PEERS_CHANGED_ACTION*. We will *requestPeers* when *discoverPeers* succeeds and *WIFI_P2P_PEERS_CHANGED_ACTION* be received.
    * Create the class *deviceListListener* for the callback when the peer list is availabled after *requestPeers*.
        * Iterate the peer list, filter the device name that contain "aiwings", and *connect* to the device if its status is **AVAILABLE**, or *requestConnectionInfo* if its status is **CONNECTED**.
        * Intent *WIFI_P2P_CONNECTION_CHANGED_ACTION* may be trigger when connection is changed.
        * *WifiP2pInfo*, that contains *groupFormed*, *isGroupOwner* and *groupOwnerAddress* (e.g., "groupFormed: true isGroupOwner: false groupOwnerAddress: /192.168.49.1"), will be received in *onConnectionInfoAvailable* in *ConnectionInfoListener* when connection is availabled after *requestConnectionInfo*.
        * After the *onConnectionInfoAvailable*, call *requestGroupInfo* to get the *WifiP2pGroup* in *onGroupInfoAvailable* that contains the group information, e.q., "network: DIRECT-Aw-AIWINGS_MENzl isGO: false GO: Device: AIWINGS_a1356 deviceAddress: 76:f6:1c:a8:a3:b0 primary type: 10-0050F204-5 secondary type: null wps: 392 grpcapab: 0 devcapab: 37 status: 4 wfdInfo: WFD enabled: trueWFD DeviceInfo: 16 WFD CtrlPort: 7236 WFD MaxThroughput: 50 WFD R2 DeviceInfo: -1 vendorElements: null interface: p2p0 networkId: 2 frequency: 5180"
    * Create a *sendmessage* button in menu for sending messages.
        * Create a class *UdpSocket* to send and receive message through the UDP socket.
        * The function *getBroadcastAddress* is to generate the UDP broadcast address by modify *connectedGroupAddress* that is from *WifiP2pInfo.groupOwnerAddress*, e.q., if the *connectedGroupAddress* is "/192.168.49.1", the broadcast address will be "192.168.49.255".
        * When click *sendmessage* button, send a message that contains current time and device name to the UDP broadcast address. Due to the tendency of UDP to loss packets, we execute *send* command 5 times. (May still loss the message.)
        * Create a handler to handle the message received from UDP socket to display in the TextView.
    * Create a *disconnection* button in the menu for disconnection by calling *removeGroup*.

- References
    - Wi-Fi Direct
        - [Create P2P connections with Wi-Fi Direct](https://developer.android.com/develop/connectivity/wifi/wifi-direct)
        - [WifiP2pManager](https://developer.android.com/reference/android/net/wifi/p2p/WifiP2pManager)
        - [Request permission to access nearby Wi-Fi devices](https://developer.android.com/develop/connectivity/wifi/wifi-permissions)
        - [HelpApp](https://github.com/y-delta/wifidirectchat/tree/master)
        - [Samples-WiFiDirectDemo](https://android.googlesource.com/platform/development/+/refs/heads/main/samples/WiFiDirectDemo/src/com/example/android/wifidirect)
        - [Rename device name](https://stackoverflow.com/questions/27315198/android-rename-devices-name-for-wifi-direct)
    - Kotlin coroutine
        - [Getting Started with Kotlin Coroutines in Android](https://nickand.medium.com/getting-started-with-kotlin-coroutines-in-android-bfa8283fcf60)
        - [Kotlin Coroutines 那一兩件事情](https://medium.com/jastzeonic/kotlin-coroutine-%E9%82%A3%E4%B8%80%E5%85%A9%E4%BB%B6%E4%BA%8B%E6%83%85-685e02761ae0)
    - UDP Socket
        - [[Android|Kotlin]UDP Socket](https://medium.com/@hongminlai/android-kotlin-udp-socket-fba4474ea0b1)
    - NDK (preparing for future use)
        - [Get started with the NDK](https://developer.android.com/ndk/guides)
        - [Add C and C++ code to your project](https://developer.android.com/studio/projects/add-native-code)
        - [JNI tips](https://developer.android.com/training/articles/perf-jni)


### Monitoring Wi-Fi (802.11)
#### 列出網路介面卡資訊
- Windows [netsh](https://learn.microsoft.com/zh-tw/windows-server/networking/technologies/netsh/netsh-contexts)
    ```console
    netsh wlan show all
    ```
- Linux [iw](https://wireless.wiki.kernel.org/en/users/documentation/iw) [iwlist](https://linux.die.net/man/8/iwlist)
    ```console
    ifconfig
    sudo iw dev [interface] info #網路介面卡資訊
    sudo iw dev [interface] link #網路介面卡連線訊息
    sudo iw dev [interface] scan #網路介面卡掃描結果
    iwlist [interface] channel #網路介面卡可用frequency/channel
    ```

#### Linux: 設定網路介面卡
- Set mode=monitor and channel=1/6/11(Social Channel) or others

- Using airmon-ng ([askubuntu](https://askubuntu.com/questions/603477/error-for-wireless-request-set-frequency-8b04), [netbeez](https://netbeez.net/blog/linux-how-to-configure-monitoring-mode-wifi-interface/))
    ```console
    sudo airmon-ng check kill
    #sudo airmon-ng stop [interface]
    sudo iw [interface] del
    sudo airmon-ng # Found phy1 with no interfaces assigned, would you like to assign ont to it? [y]
    sudo airmon-ng start [interface] [channel]
    iw wlan0mon info
    ```

- Using iwconfig ([Wireless Sniffing](https://cyberlab.pacific.edu/resources/lab-network-wireless-sniffing))
    ```console
    iw [interface] info
    ifconfig [interface] down
    sudo iwconfig [interface] mode monitor
    sudo iwconfig [interface] mode monitor channel 1 # May cause 8B04 error, so use airmon-ng instead.
    iw [interface] info
    sudo ifconfig [interface] up
    ```

#### Wireshark capture
- [WLAN Capture](https://wiki.wireshark.org/CaptureSetup/WLAN)
    ```console
    sudo wireshark
    ```
- filter with source=MAC-address: 
    ```filter
    wlan.sa==[MAC address]
    ```
- [CWAP 802.11- Probe Request/Response](https://mrncciew.com/2014/10/27/cwap-802-11-probe-requestresponse/)

### Observation
* In Android app, when trigger "WifiP2pManager.discoverPeers", app keeps sending *Probe Request* to broadcast address (ff:ff:ff:ff:ff:ff) with SSID="DIRECT-" about 2 minutes in channel 1, 6 and 11 (social channels), and 1, 2 or 3 packets in other channels. In Android phone(SDK ver.=34), when operate *設定->網際網路->網路偏好設定->Wi-Fi Direct*, *Probe Request* will be keeps sending about 2.5 minutes in social channels and few in others.
* MAC address in Android with SDK version equals 34 will be **different every time** in *Probe Request* packets (locally administered, see [stackoverflow](https://stackoverflow.com/questions/10968951/wi-fi-direct-and-normal-wi-fi-different-mac)). But in SDK version equals 28, the MAC address is always the same.

## TODO - Hardware
- Type: Drone?
- MCU/DevBoard: Raspberry Pi?
- Device purpose: Target searching?
## TODO - Android
- OS: Lineage?
- SDK or NDK to implement Wi-Fi Direct?
## TODO - Measuring distance
- Wi-Fi RSSI: Not provided by Android SDK. May use the wpa_supplicant?
- Sensor: GPS, mmWave, camera?
## TODO - Swarm algorithm
- According [SwarmLab](https://ieeexplore.ieee.org/document/9340854) ([github](https://github.com/lis-epfl/swarmlab))
    1. Olfati-Saber
        - [DISTRIBUTED COOPERATIVE CONTROL OF MULTIPLE VEHICLE FORMATIONS USING STRUCTURAL POTENTIAL FUNCTIONS](https://www.sciencedirect.com/science/article/pii/S1474667015386651)
        - Graph theory
    2. Vásárhelyi
        - [Outdoor flocking and formation flight with autonomous aerial robots](https://arxiv.org/abs/1402.3588)
        - [Decentralized traffic management of autonomous drones](https://arxiv.org/abs/2312.11207)
    - Implement the 2 alrorithms in C/C++ or using Simulink to import the SwarmLab's Matlab code for Android?
- ***Would/How to use deep learning (RL/GAN) to train a model for the control of the individual device to swarm?***
