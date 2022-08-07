package com.fimbleenterprises.torquepidcaster.data.db

import androidx.room.*
import androidx.room.OnConflictStrategy.REPLACE
import com.fimbleenterprises.torquepidcaster.data.model.FullPid
import com.fimbleenterprises.torquepidcaster.data.model.TriggeredPid
import kotlinx.coroutines.flow.Flow

/**
 * This interface is what Room will use to actually perform CRUD operations in the db.
 */
@Dao // This annotation turns this interface into a magical mechanism to actually perform CRUD operations in the Room db.
interface LogsDao {

    @Insert(entity = TriggeredPid::class, onConflict = REPLACE)
    suspend fun insert(triggeredPid: TriggeredPid): Long

    @Delete
    suspend fun delete(triggeredPid: TriggeredPid): Int

    @Query("SELECT * FROM trigger_log")
    fun getAll(): Flow<List<TriggeredPid>>

    @Query("DELETE FROM trigger_log")
    fun clearLog(): Int

}