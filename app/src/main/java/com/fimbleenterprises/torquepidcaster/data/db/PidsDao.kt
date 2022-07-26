package com.fimbleenterprises.torquepidcaster.data.db

import androidx.room.*
import androidx.room.OnConflictStrategy.REPLACE
import com.fimbleenterprises.torquepidcaster.data.model.FullPid
import kotlinx.coroutines.flow.Flow

/**
 * This interface is what Room will use to actually perform CRUD operations in the db.
 */
@Dao // This annotation turns this interface into a magical mechanism to actually perform CRUD operations in the Room db.
interface PidsDao {

    @Insert(entity = FullPid::class, onConflict = REPLACE)
    suspend fun savePid(pid: FullPid): Long

    @Update
    suspend fun updatePid(pid: FullPid): Int

    @Delete
    suspend fun deletePid(pid: FullPid): Int

    @Query("SELECT * FROM savedpids WHERE fullName = :strName")
    fun getSavedPid(strName: String): Flow<FullPid>

    @Query("SELECT * FROM savedpids")
    fun getSavedPids(): Flow<List<FullPid>>

}