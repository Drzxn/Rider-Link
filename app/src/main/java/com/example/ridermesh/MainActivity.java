package com.example.ridermesh;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.ridermesh.data.RoomRepository;
import com.example.ridermesh.network.MeshNetworkManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private MeshNetworkManager meshManager;
    private RoomRepository roomRepository;

    private String[] requiredPermissions;

    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), new ActivityResultCallback<Map<String, Boolean>>() {
                @Override
                public void onActivityResult(Map<String, Boolean> permissions) {
                    boolean granted = true;
                    for (Boolean b : permissions.values()) {
                        if (!b) {
                            granted = false;
                            break;
                        }
                    }
                    if (granted) {
                        initializeMeshNetwork();
                    } else {
                        Toast.makeText(MainActivity.this, "Permissions required for P2P", Toast.LENGTH_LONG).show();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        roomRepository = new RoomRepository(this);

        List<String> perms = new ArrayList<>();
        perms.add(Manifest.permission.ACCESS_FINE_LOCATION);
        perms.add(Manifest.permission.RECORD_AUDIO);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.NEARBY_WIFI_DEVICES);
        }
        requiredPermissions = perms.toArray(new String[0]);

        if (allPermissionsGranted()) {
            initializeMeshNetwork();
        } else {
            requestPermissionLauncher.launch(requiredPermissions);
        }
    }

    private boolean allPermissionsGranted() {
        for (String perm : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(getBaseContext(), perm) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void initializeMeshNetwork() {
        Log.d(TAG, "Initializing Mesh Network");
        WifiP2pManager manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        WifiP2pManager.Channel channel = null;
        if (manager != null) {
            channel = manager.initialize(this, getMainLooper(), null);
        }

        if (manager != null && channel != null) {
            meshManager = new MeshNetworkManager(this, manager, channel);

            // Example: Start advertising a room (Host mode)
            // In a real UI, this would be triggered by a button
            String roomId = "Ride-8821";
            meshManager.advertiseRoom(roomId);
            roomRepository.saveCurrentRoomId(roomId);

            Toast.makeText(this, "Mesh Network Initialized. Room: " + roomId, Toast.LENGTH_SHORT).show();
        } else {
            Log.e(TAG, "Cannot initialize WifiP2pManager");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (meshManager != null) {
            meshManager.tearDown();
        }
    }
}
