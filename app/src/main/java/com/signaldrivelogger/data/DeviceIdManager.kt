package com.signaldrivelogger.data

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

/**
 * Phase 3: Manages device ID using UUID stored in SharedPreferences.
 * Replaces ANDROID_ID to comply with Google Play policies.
 */
class DeviceIdManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "device_prefs",
        Context.MODE_PRIVATE
    )

    private val DEVICE_ID_KEY = "device_id"

    /**
     * Gets or generates a unique device ID.
     * On first launch, generates a UUID and stores it in SharedPreferences.
     * On subsequent launches, retrieves the stored UUID.
     */
    fun getDeviceId(): String {
        val storedId = prefs.getString(DEVICE_ID_KEY, null)
        return if (storedId != null) {
            storedId
        } else {
            // Generate new UUID on first launch
            val newId = UUID.randomUUID().toString()
            prefs.edit().putString(DEVICE_ID_KEY, newId).apply()
            newId
        }
    }

    /**
     * Resets the device ID (for testing/debugging purposes).
     */
    fun resetDeviceId() {
        prefs.edit().remove(DEVICE_ID_KEY).apply()
    }
}
