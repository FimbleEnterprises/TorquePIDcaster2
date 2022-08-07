package com.fimbleenterprises.torquepidcaster.data.repository.datasourceImpl

import android.util.Log
import com.fimbleenterprises.torquepidcaster.data.db.LogsDao
import com.fimbleenterprises.torquepidcaster.data.db.PidsDao
import com.fimbleenterprises.torquepidcaster.data.model.FullPid
import com.fimbleenterprises.torquepidcaster.data.model.TriggeredPid
import com.fimbleenterprises.torquepidcaster.data.repository.datasource.LocalDatasource
import kotlinx.coroutines.flow.Flow

/**
 * This class allows us to interact with the Room DB via the [PidsDao].
 */
class LocalDatasourceImpl
    constructor(
        private val pidsDao: PidsDao,
        private val logsDao: LogsDao
    ) : LocalDatasource {

    /**
     * Inserts a new [FullPid] object in the database via [PidsDao].
     */
    override suspend fun savePid(pid: FullPid): Long {
        return pidsDao.savePid(pid)
    }

    /**
     * Updates a saved (monitored) [FullPid] object in the database via [PidsDao].
     */
    override suspend fun updatePid(pid: FullPid): Int {
        return pidsDao.updatePid(pid)
    }

    /**
     * Removes a saved (monitored) [FullPid] object from the database via [PidsDao].
     */
    override suspend fun deletePid(pid: FullPid): Int {
        return pidsDao.deletePid(pid)
    }

    override suspend fun deleteAll(): Int {
        return pidsDao.deleteAll()
    }

    /**
     * Returns a saved (monitored) [FullPid] object from the database via [PidsDao].
     */
    override fun getSavedPid(strName: String): Flow<FullPid> {
        return pidsDao.getSavedPid(strName)
    }

    /**
     * Returns all saved (monitored) [FullPid] objects from the database via [PidsDao].
     */
    override fun getSavedPids(): Flow<List<FullPid>> {
        return pidsDao.getSavedPids()
    }

    override fun getLogEntries(): Flow<List<TriggeredPid>> {
        return logsDao.getAll()
    }

    override suspend fun insertLogEntry(triggeredPid: TriggeredPid): Long {
        return logsDao.insert(triggeredPid)
    }

    override suspend fun deleteLogEntry(triggeredPid: TriggeredPid): Int {
        val num = logsDao.delete(triggeredPid)
        Log.w(TAG, "deleted $num LogEntries: ")
        return num
    }

    override suspend fun clearLog(): Int {
        return logsDao.clearLog()
    }

    init { Log.i(TAG, "Initialized:LocalDatasourceImpl") }
    companion object { private const val TAG = "FIMTOWN|LocalDatasourceImpl" }
}