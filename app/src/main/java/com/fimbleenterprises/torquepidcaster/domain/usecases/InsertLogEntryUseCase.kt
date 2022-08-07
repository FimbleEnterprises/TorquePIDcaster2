package com.fimbleenterprises.torquepidcaster.domain.usecases

import android.util.Log
import com.fimbleenterprises.torquepidcaster.data.model.FullPid
import com.fimbleenterprises.torquepidcaster.data.model.TriggeredPid
import com.fimbleenterprises.torquepidcaster.domain.repository.MainRepository

class InsertLogEntryUseCase(private val mainRepository: MainRepository) {

    /**
     * Saves a single [FullPid] object to the database.
     */
    suspend fun execute(triggeredPid: TriggeredPid) : Long = mainRepository.insertLogEntry(triggeredPid)

    init { Log.i(TAG, "Initialized:InsertLogEntryUseCase") }
    companion object { private const val TAG = "FIMTOWN|InsertLogEntryUseCase" }
}