package com.example.ridermesh.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Repository for managing persistent room data.
 * Ensures the "Sticky Room" ID is saved across app restarts or connection drops.
 */
class RoomRepository(context: Context) {

    private val PREFS_NAME = "ridermesh_prefs"
    private val KEY_LAST_ROOM_UUID = "last_room_uuid"

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Saves the current connected Room UUID.
     * Call this when a connection is successfully established.
     */
    fun saveCurrentRoomId(uuid: String) {
        prefs.edit().putString(KEY_LAST_ROOM_UUID, uuid).apply()
    }

    /**
     * Retrieves the last known Room UUID.
     * Used by ReconnectionService to find the lost group.
     */
    fun getLastRoomId(): String? {
        return prefs.getString(KEY_LAST_ROOM_UUID, null)
    }

    /**
     * Clears the stored Room UUID.
     * Call this when the user explicitly leaves a group.
     */
    fun clearRoomId() {
        prefs.edit().remove(KEY_LAST_ROOM_UUID).apply()
    }
}
