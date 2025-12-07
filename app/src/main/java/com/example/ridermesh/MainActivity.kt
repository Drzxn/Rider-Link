package com.example.ridermesh

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.ridermesh.network.MeshNetworkManager
import com.example.ridermesh.data.RoomRepository

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"
    private var meshManager: MeshNetworkManager? = null
    private lateinit var roomRepository: RoomRepository

    private val requiredPermissions = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.RECORD_AUDIO
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
    }.toTypedArray()

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted = permissions.entries.all { it.value }
            if (granted) {
                initializeMeshNetwork()
            } else {
                Toast.makeText(this, "Permissions required for P2P", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        roomRepository = RoomRepository(this)

        if (allPermissionsGranted()) {
            initializeMeshNetwork()
        } else {
            requestPermissionLauncher.launch(requiredPermissions)
        }
    }

    private fun allPermissionsGranted() = requiredPermissions.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun initializeMeshNetwork() {
        Log.d(TAG, "Initializing Mesh Network")
        val manager = getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
        val channel = manager?.initialize(this, mainLooper, null)

        if (manager != null && channel != null) {
            meshManager = MeshNetworkManager(this, manager, channel)

            // Example: Start advertising a room (Host mode)
            // In a real UI, this would be triggered by a button
            val roomId = "Ride-8821"
            meshManager?.advertiseRoom(roomId)
            roomRepository.saveCurrentRoomId(roomId)

            Toast.makeText(this, "Mesh Network Initialized. Room: $roomId", Toast.LENGTH_SHORT).show()
        } else {
            Log.e(TAG, "Cannot initialize WifiP2pManager")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        meshManager?.tearDown()
    }
}
