package com.runtracker.shared.data.db

import androidx.room.*
import com.runtracker.shared.data.model.BodyScan
import kotlinx.coroutines.flow.Flow

@Dao
interface BodyScanDao {
    
    @Query("SELECT * FROM body_scans ORDER BY timestamp DESC")
    fun getAllScans(): Flow<List<BodyScan>>
    
    @Query("SELECT * FROM body_scans ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestScan(): BodyScan?
    
    @Query("SELECT * FROM body_scans WHERE id = :id")
    suspend fun getScanById(id: Long): BodyScan?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: BodyScan): Long
    
    @Update
    suspend fun updateScan(scan: BodyScan)
    
    @Delete
    suspend fun deleteScan(scan: BodyScan)
    
    @Query("DELETE FROM body_scans")
    suspend fun deleteAllScans()
}
