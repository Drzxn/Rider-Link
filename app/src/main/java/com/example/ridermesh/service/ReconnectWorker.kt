package com.example.ridermesh.service

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pConfig
import android.os.Looper
import android.util.Log
import com.example.ridermesh.data.RoomRepository
import com.example.ridermesh.network.MeshNetworkManager
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * ReconnectWorker handles the logic of scanning for the lost Group_UUID
 * and re-initiating the connection.
 *
 * This worker should be enqueued when a disconnect event is detected
 * and a "Sticky Room" ID exists in the repository.
 */
class ReconnectWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    private val TAG = "ReconnectWorker"
    private val roomRepo = RoomRepository(context)

    // In a real dependency injection scenario, these would be injected.
    // Here we instantiate them assuming standard system service availability.
    private val wifiP2pManager = context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    private val channel = wifiP2pManager?.initialize(context, Looper.getMainLooper(), null)

    private var meshNetworkManager: MeshNetworkManager? = null

    init {
        if (wifiP2pManager != null && channel != null) {
            meshNetworkManager = MeshNetworkManager(context, wifiP2pManager, channel)
        }
    }

    override fun doWork(): Result {
        val targetUuid = roomRepo.getLastRoomId()
        if (targetUuid == null) {
            Log.d(TAG, "No sticky room found. Aborting reconnection.")
            return Result.failure()
        }

        if (meshNetworkManager == null) {
             Log.e(TAG, "WifiP2pManager not available.")
             return Result.failure()
        }

        Log.i(TAG, "Attempting to reconnect to Room: $targetUuid")

        // Synchronization aid to wait for discovery results
        val latch = CountDownLatch(1)
        var connectionInitiated = false

        try {
            // Start scanning for the specific UUID
            meshNetworkManager?.discoverRooms(targetUuid) { config ->
                Log.i(TAG, "Found target room! Initiating connection to ${config.deviceAddress}")

                // We found the room, now connect.
                // Note: In a real worker, we might need to handle the connection result more robustly.
                meshNetworkManager?.connectToPeer(config)
                connectionInitiated = true
                latch.countDown()
            }

            // Wait for a result or timeout (e.g., 30 seconds scan window)
            val success = latch.await(30, TimeUnit.SECONDS)
            if (!success) {
                Log.d(TAG, "Timeout: Target room $targetUuid not found within window.")
                // Return retry to reschedule this worker with backoff
                return Result.retry()
            }
        } catch (e: InterruptedException) {
            Log.e(TAG, "Worker interrupted", e)
            return Result.failure()
        } finally {
            // Important: Stop discovery to save battery and clean up resources
            meshNetworkManager?.stopDiscovery()
        }

        return if (connectionInitiated) {
             Log.i(TAG, "Reconnection sequence initiated successfully.")
             Result.success()
        } else {
             Result.retry()
        }
    }
}
