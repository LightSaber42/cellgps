package com.signaldrivelogger.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.signaldrivelogger.data.database.entities.SimProfileEntity

/**
 * Data Access Object for SimProfileEntity.
 */
@Dao
interface SimProfileDao {
    /**
     * Insert or update a SIM profile.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(profile: SimProfileEntity)

    /**
     * Update a SIM profile.
     */
    @Update
    suspend fun update(profile: SimProfileEntity)

    /**
     * Get a SIM profile by subscription ID.
     */
    @Query("SELECT * FROM sim_profiles WHERE subscription_id = :subscriptionId")
    suspend fun getSimProfile(subscriptionId: Int): SimProfileEntity?

    /**
     * Get all SIM profiles.
     */
    @Query("SELECT * FROM sim_profiles ORDER BY sim_slot_index ASC")
    suspend fun getAllSimProfiles(): List<SimProfileEntity>

    /**
     * Delete a SIM profile.
     */
    @Query("DELETE FROM sim_profiles WHERE subscription_id = :subscriptionId")
    suspend fun deleteSimProfile(subscriptionId: Int)
}
