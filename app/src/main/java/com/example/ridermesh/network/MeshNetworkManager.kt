package com.example.ridermesh.network

import android.content.Context
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest
import android.os.Looper

/**
 * Manages Wi-Fi Direct (P2P) connections, service discovery, and group negotiation.
 */
class MeshNetworkManager(
    private val context: Context,
    private val manager: WifiP2pManager,
    private val channel: WifiP2pManager.Channel
) {

    private val SERVICE_TYPE = "_ridermesh._tcp"

    /**
     * Registers a local service to be discovered by other peers.
     * This broadcasts the unique Room UUID (Session ID).
     */
    fun advertiseRoom(roomUuid: String) {
        val record = mapOf(
            "room_uuid" to roomUuid,
            "listen_port" to "8888", // UDP Port
            "buddy_name" to "Host" // Dynamic name
        )

        val serviceInfo = WifiP2pDnsSdServiceInfo.newInstance(
            "RiderMeshGroup",
            SERVICE_TYPE,
            record
        )

        manager.addLocalService(channel, serviceInfo, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                // Service registration successful
            }

            override fun onFailure(reason: Int) {
                // Failed to register service
            }
        })
    }

    /**
     * Initiates discovery for Wi-Fi Direct services (DNS-SD).
     * Filters specifically for "RiderMesh" services and matches the target UUID.
     */
    fun discoverRooms(
        targetUuid: String?,
        onRoomFound: (WifiP2pConfig) -> Unit
    ) {
        val serviceRequest = WifiP2pDnsSdServiceRequest.newInstance()

        manager.setDnsSdResponseListeners(channel,
            { instanceName, registrationType, srcDevice ->
                // Check if this is our service type
                if (registrationType.contains(SERVICE_TYPE)) {
                    // Log or process basic info
                }
            },
            { fullDomainName, txtRecordMap, srcDevice ->
                // Check TXT record for room_uuid
                val discoveredUuid = txtRecordMap["room_uuid"]
                if (discoveredUuid != null) {
                    if (targetUuid == null || discoveredUuid == targetUuid) {
                        // Found a matching room or any room if target is null
                        val config = WifiP2pConfig()
                        config.deviceAddress = srcDevice.deviceAddress
                        onRoomFound(config)
                    }
                }
            }
        )

        manager.addServiceRequest(channel, serviceRequest, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                manager.discoverServices(channel, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {}
                    override fun onFailure(reason: Int) {}
                })
            }
            override fun onFailure(reason: Int) {}
        })
    }

    /**
     * Connects to a specific peer (Group Owner or potential peer).
     */
    fun connectToPeer(config: WifiP2pConfig) {
        manager.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                // Connection negotiation started
            }

            override fun onFailure(reason: Int) {
                // Connection failed
            }
        })
    }

    /**
     * Stops discovery and clears service requests.
     * Does NOT remove the group or disconnect.
     */
    fun stopDiscovery() {
        manager.clearServiceRequests(channel, null)
        manager.stopPeerDiscovery(channel, null)
    }

    /**
     * Cleans up services and connections.
     */
    fun tearDown() {
        stopDiscovery()
        manager.removeGroup(channel, null)
        manager.clearLocalServices(channel, null)
    }
}
