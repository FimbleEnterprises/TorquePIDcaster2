package com.fimbleenterprises.torquepidcaster.domain.usecases

import android.util.Log
import com.fimbleenterprises.torquepidcaster.data.model.FullPid
import com.fimbleenterprises.torquepidcaster.data.model.TriggeredPid
import com.fimbleenterprises.torquepidcaster.domain.repository.MainRepository

class DeleteLogEntriesUseCase(private val mainRepository: MainRepository) {

    suspend fun execute(triggeredPid: TriggeredPid) : Int = mainRepository.deleteLogEntry(triggeredPid)
    suspend fun executeMany(): Int = mainRepository.deleteAllLogEntries()

    init { Log.i(TAG, "Initialized:DeleteLogEntriesUseCase") }
    companion object { private const val TAG = "FIMTOWN|DeleteLogEntriesUseCase" }
}