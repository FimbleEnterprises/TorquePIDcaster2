package com.fimbleenterprises.torquepidcaster.data.repository

import com.fimbleenterprises.torquepidcaster.data.model.FullPid
import com.fimbleenterprises.torquepidcaster.data.model.TriggeredPid
import com.fimbleenterprises.torquepidcaster.data.repository.datasource.LocalDatasource
import com.fimbleenterprises.torquepidcaster.domain.repository.MainRepository
import kotlinx.coroutines.flow.Flow

/**
 * This class dictates all database operations that can be performed.
 */
class MainRepositoryImpl
    constructor(
        private val localDatasource: LocalDatasource
    ): MainRepository {

    override fun getSavedPids(): Flow<List<FullPid>> {
        return localDatasource.getSavedPids()
    }

    override fun getSavedPid(strName: String): Flow<FullPid> {
        return localDatasource.getSavedPid(strName)
    }

    override suspend fun savePid(pid: FullPid): Long {
        return localDatasource.savePid(pid)
    }

    override suspend fun updatePid(pid: FullPid): Int {
        return localDatasource.updatePid(pid)
    }

    override suspend fun deletePid(pid: FullPid): Int {
        return localDatasource.deletePid(pid)
    }

    override suspend fun deleteAll(): Int {
        return localDatasource.deleteAll()
    }

    override suspend fun deleteLogEntry(triggeredPid: TriggeredPid): Int {
        return localDatasource.deleteLogEntry(triggeredPid)
    }

    override suspend fun deleteAllLogEntries(): Int {
        return localDatasource.clearLog()
    }

    override fun getLogEntries(): Flow<List<TriggeredPid>> {
        return localDatasource.getLogEntries()
    }

    override suspend fun insertLogEntry(triggeredPid: TriggeredPid): Long {
        return localDatasource.insertLogEntry(triggeredPid)
    }


}