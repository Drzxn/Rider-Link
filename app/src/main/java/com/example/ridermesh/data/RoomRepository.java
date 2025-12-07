package com.example.ridermesh.data;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Repository for managing persistent room data.
 * Ensures the "Sticky Room" ID is saved across app restarts or connection drops.
 */
public class RoomRepository {

    private static final String PREFS_NAME = "ridermesh_prefs";
    private static final String KEY_LAST_ROOM_UUID = "last_room_uuid";

    private final SharedPreferences prefs;

    public RoomRepository(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Saves the current connected Room UUID.
     * Call this when a connection is successfully established.
     */
    public void saveCurrentRoomId(String uuid) {
        prefs.edit().putString(KEY_LAST_ROOM_UUID, uuid).apply();
    }

    /**
     * Retrieves the last known Room UUID.
     * Used by ReconnectionService to find the lost group.
     */
    public String getLastRoomId() {
        return prefs.getString(KEY_LAST_ROOM_UUID, null);
    }

    /**
     * Clears the stored Room UUID.
     * Call this when the user explicitly leaves a group.
     */
    public void clearRoomId() {
        prefs.edit().remove(KEY_LAST_ROOM_UUID).apply();
    }
}
