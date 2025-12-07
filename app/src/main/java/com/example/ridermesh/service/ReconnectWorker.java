package com.example.ridermesh.service;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.ridermesh.data.RoomRepository;
import com.example.ridermesh.network.MeshNetworkManager;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * ReconnectWorker handles the logic of scanning for the lost Group_UUID
 * and re-initiating the connection.
 *
 * This worker should be enqueued when a disconnect event is detected
 * and a "Sticky Room" ID exists in the repository.
 */
public class ReconnectWorker extends Worker {

    private static final String TAG = "ReconnectWorker";
    private final RoomRepository roomRepo;

    // In a real dependency injection scenario, these would be injected.
    // Here we instantiate them assuming standard system service availability.
    private WifiP2pManager wifiP2pManager;
    private WifiP2pManager.Channel channel;

    private MeshNetworkManager meshNetworkManager;

    public ReconnectWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        roomRepo = new RoomRepository(context);

        wifiP2pManager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        if (wifiP2pManager != null) {
            channel = wifiP2pManager.initialize(context, Looper.getMainLooper(), null);
            if (channel != null) {
                meshNetworkManager = new MeshNetworkManager(context, wifiP2pManager, channel);
            }
        }
    }

    @NonNull
    @Override
    public Result doWork() {
        String targetUuid = roomRepo.getLastRoomId();
        if (targetUuid == null) {
            Log.d(TAG, "No sticky room found. Aborting reconnection.");
            return Result.failure();
        }

        if (meshNetworkManager == null) {
             Log.e(TAG, "WifiP2pManager not available.");
             return Result.failure();
        }

        Log.i(TAG, "Attempting to reconnect to Room: " + targetUuid);

        // Synchronization aid to wait for discovery results
        final CountDownLatch latch = new CountDownLatch(1);
        // We use a final array to hold the result since variables in lambda must be final
        final boolean[] connectionInitiated = {false};

        try {
            // Start scanning for the specific UUID
            meshNetworkManager.discoverRooms(targetUuid, new MeshNetworkManager.OnRoomFoundListener() {
                @Override
                public void onRoomFound(WifiP2pConfig config) {
                    Log.i(TAG, "Found target room! Initiating connection to " + config.deviceAddress);

                    // We found the room, now connect.
                    // Note: In a real worker, we might need to handle the connection result more robustly.
                    meshNetworkManager.connectToPeer(config);
                    connectionInitiated[0] = true;
                    latch.countDown();
                }
            });

            // Wait for a result or timeout (e.g., 30 seconds scan window)
            boolean success = latch.await(30, TimeUnit.SECONDS);
            if (!success) {
                Log.d(TAG, "Timeout: Target room " + targetUuid + " not found within window.");
                // Return retry to reschedule this worker with backoff
                return Result.retry();
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "Worker interrupted", e);
            return Result.failure();
        } finally {
            // Important: Stop discovery to save battery and clean up resources
            if (meshNetworkManager != null) {
                meshNetworkManager.stopDiscovery();
            }
        }

        if (connectionInitiated[0]) {
             Log.i(TAG, "Reconnection sequence initiated successfully.");
             return Result.success();
        } else {
             return Result.retry();
        }
    }
}
