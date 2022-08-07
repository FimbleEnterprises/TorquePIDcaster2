package com.fimbleenterprises.torquepidcaster.domain.usecases

import android.util.Log
import com.fimbleenterprises.torquepidcaster.data.model.FullPid
import com.fimbleenterprises.torquepidcaster.domain.repository.MainRepository

class SavePidUseCase(private val mainRepository: MainRepository) {

    /**
     * Saves a single [FullPid] object to the database.
     */
    suspend fun execute(pid : FullPid) : Long = mainRepository.savePid(pid)

    init { Log.i(TAG, "Initialized:SavePidUseCase") }
    companion object { private const val TAG = "FIMTOWN|SavePidUseCase" }
}