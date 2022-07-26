package com.fimbleenterprises.torquepidcaster.data.repository

import com.fimbleenterprises.torquepidcaster.data.model.FullPid
import com.fimbleenterprises.torquepidcaster.data.repository.datasource.LocalDatasource
import com.fimbleenterprises.torquepidcaster.domain.repository.MainRepository
import kotlinx.coroutines.flow.Flow
import org.prowl.torque.remote.ITorqueService

/**
 * This class will do the actual lifting for all things local.
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


}