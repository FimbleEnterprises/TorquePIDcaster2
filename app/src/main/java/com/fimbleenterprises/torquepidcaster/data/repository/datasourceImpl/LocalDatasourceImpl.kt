package com.fimbleenterprises.torquepidcaster.data.repository.datasourceImpl

import com.fimbleenterprises.torquepidcaster.data.db.PidsDao
import com.fimbleenterprises.torquepidcaster.data.model.FullPid
import com.fimbleenterprises.torquepidcaster.data.repository.datasource.LocalDatasource
import kotlinx.coroutines.flow.Flow

/**
 * This class does the heavy lifting by interacting with the Room DB.
 */
class LocalDatasourceImpl
    constructor(
        private val pidsDao: PidsDao
    ) : LocalDatasource {

    override suspend fun savePid(pid: FullPid): Long {
        return pidsDao.savePid(pid)
    }

    override suspend fun updatePid(pid: FullPid): Int {
        return pidsDao.updatePid(pid)
    }

    override suspend fun deletePid(pid: FullPid): Int {
        return pidsDao.deletePid(pid)
    }

    override fun getSavedPid(strName: String): Flow<FullPid> {
        return pidsDao.getSavedPid(strName)
    }

    override fun getSavedPids(): Flow<List<FullPid>> {
        return pidsDao.getSavedPids()
    }

}