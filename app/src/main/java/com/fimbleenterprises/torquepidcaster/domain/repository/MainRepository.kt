package com.fimbleenterprises.torquepidcaster.domain.repository

import com.fimbleenterprises.torquepidcaster.data.model.FullPid
import kotlinx.coroutines.flow.Flow

interface MainRepository {

    // DB ops
    fun getSavedPids(): Flow<List<FullPid>>
    fun getSavedPid(strName: String): Flow<FullPid>
    suspend fun savePid(pid: FullPid): Long
    suspend fun updatePid(pid: FullPid): Int
    suspend fun deletePid(pid: FullPid): Int
}