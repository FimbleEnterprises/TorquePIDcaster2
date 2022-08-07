package com.fimbleenterprises.torquepidcaster.domain.repository

import com.fimbleenterprises.torquepidcaster.data.model.FullPid
import com.fimbleenterprises.torquepidcaster.data.model.TriggeredPid
import kotlinx.coroutines.flow.Flow

interface MainRepository {

    // PIDs
    fun getSavedPids(): Flow<List<FullPid>>
    fun getSavedPid(strName: String): Flow<FullPid>
    suspend fun savePid(pid: FullPid): Long
    suspend fun updatePid(pid: FullPid): Int
    suspend fun deletePid(pid: FullPid): Int
    suspend fun deleteAll(): Int

    // Log entries
    suspend fun deleteLogEntry(triggeredPid: TriggeredPid): Int
    suspend fun deleteAllLogEntries(): Int
    fun getLogEntries(): Flow<List<TriggeredPid>>
    suspend fun insertLogEntry(triggeredPid: TriggeredPid): Long
}