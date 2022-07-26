package com.fimbleenterprises.torquepidcaster.data.repository.datasource

import androidx.room.*
import com.fimbleenterprises.torquepidcaster.data.model.FullPid
import kotlinx.coroutines.flow.Flow

interface LocalDatasource {

    suspend fun savePid(pid: FullPid): Long
    suspend fun updatePid(pid: FullPid): Int
    suspend fun deletePid(pid: FullPid): Int
    fun getSavedPid(strName: String): Flow<FullPid>
    fun getSavedPids(): Flow<List<FullPid>>

}