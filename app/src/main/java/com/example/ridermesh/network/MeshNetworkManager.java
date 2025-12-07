package com.example.ridermesh.network;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages Wi-Fi Direct (P2P) connections, service discovery, and group negotiation.
 */
public class MeshNetworkManager {

    private final Context context;
    private final WifiP2pManager manager;
    private final WifiP2pManager.Channel channel;
    private static final String SERVICE_TYPE = "_ridermesh._tcp";

    public interface OnRoomFoundListener {
        void onRoomFound(WifiP2pConfig config);
    }

    public MeshNetworkManager(Context context, WifiP2pManager manager, WifiP2pManager.Channel channel) {
        this.context = context;
        this.manager = manager;
        this.channel = channel;
    }

    /**
     * Registers a local service to be discovered by other peers.
     * This broadcasts the unique Room UUID (Session ID).
     */
    @SuppressLint("MissingPermission")
    public void advertiseRoom(String roomUuid) {
        Map<String, String>record = new HashMap<>();
        record.put("room_uuid", roomUuid);
        record.put("listen_port", "8888"); // UDP Port
        record.put("buddy_name", "Host"); // Dynamic name

        WifiP2pDnsSdServiceInfo serviceInfo = WifiP2pDnsSdServiceInfo.newInstance(
            "RiderMeshGroup",
            SERVICE_TYPE,
            record
        );

        manager.addLocalService(channel, serviceInfo, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                // Service registration successful
            }

            @Override
            public void onFailure(int reason) {
                // Failed to register service
            }
        });
    }

    /**
     * Initiates discovery for Wi-Fi Direct services (DNS-SD).
     * Filters specifically for "RiderMesh" services and matches the target UUID.
     */
    @SuppressLint("MissingPermission")
    public void discoverRooms(String targetUuid, OnRoomFoundListener onRoomFound) {
        WifiP2pDnsSdServiceRequest serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();

        manager.setDnsSdResponseListeners(channel,
            new WifiP2pManager.DnsSdServiceResponseListener() {
                @Override
                public void onDnsSdServiceAvailable(String instanceName, String registrationType, WifiP2pDevice srcDevice) {
                    // Check if this is our service type
                    if (registrationType.contains(SERVICE_TYPE)) {
                        // Log or process basic info
                    }
                }
            },
            new WifiP2pManager.DnsSdTxtRecordListener() {
                @Override
                public void onDnsSdTxtRecordAvailable(String fullDomainName, Map<String, String> txtRecordMap, WifiP2pDevice srcDevice) {
                    // Check TXT record for room_uuid
                    String discoveredUuid = txtRecordMap.get("room_uuid");
                    if (discoveredUuid != null) {
                        if (targetUuid == null || discoveredUuid.equals(targetUuid)) {
                            // Found a matching room or any room if target is null
                            WifiP2pConfig config = new WifiP2pConfig();
                            config.deviceAddress = srcDevice.deviceAddress;
                            onRoomFound.onRoomFound(config);
                        }
                    }
                }
            }
        );

        manager.addServiceRequest(channel, serviceRequest, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                manager.discoverServices(channel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {}
                    @Override
                    public void onFailure(int reason) {}
                });
            }
            @Override
            public void onFailure(int reason) {}
        });
    }

    /**
     * Connects to a specific peer (Group Owner or potential peer).
     */
    @SuppressLint("MissingPermission")
    public void connectToPeer(WifiP2pConfig config) {
        manager.connect(channel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                // Connection negotiation started
            }

            @Override
            public void onFailure(int reason) {
                // Connection failed
            }
        });
    }

    /**
     * Stops discovery and clears service requests.
     * Does NOT remove the group or disconnect.
     */
    @SuppressLint("MissingPermission")
    public void stopDiscovery() {
        manager.clearServiceRequests(channel, null);
        manager.stopPeerDiscovery(channel, null);
    }

    /**
     * Cleans up services and connections.
     */
    @SuppressLint("MissingPermission")
    public void tearDown() {
        stopDiscovery();
        manager.removeGroup(channel, null);
        manager.clearLocalServices(channel, null);
    }
}
