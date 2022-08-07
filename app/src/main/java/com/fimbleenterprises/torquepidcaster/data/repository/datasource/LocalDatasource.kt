package com.fimbleenterprises.torquepidcaster.data.repository.datasource

import androidx.room.*
import com.fimbleenterprises.torquepidcaster.data.model.FullPid
import com.fimbleenterprises.torquepidcaster.data.model.TriggeredPid
import kotlinx.coroutines.flow.Flow

interface LocalDatasource {

    // Pids
    suspend fun savePid(pid: FullPid): Long
    suspend fun updatePid(pid: FullPid): Int
    suspend fun deletePid(pid: FullPid): Int
    suspend fun deleteAll(): Int
    fun getSavedPid(strName: String): Flow<FullPid>
    fun getSavedPids(): Flow<List<FullPid>>

    // Logging
    fun getLogEntries(): Flow<List<TriggeredPid>>
    suspend fun insertLogEntry(triggeredPid: TriggeredPid): Long
    suspend fun deleteLogEntry(triggeredPid: TriggeredPid): Int
    suspend fun clearLog(): Int

}