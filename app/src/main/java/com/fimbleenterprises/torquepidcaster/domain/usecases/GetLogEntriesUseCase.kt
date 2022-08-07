package com.fimbleenterprises.torquepidcaster.domain.usecases

import android.util.Log
import com.fimbleenterprises.torquepidcaster.data.model.FullPid
import com.fimbleenterprises.torquepidcaster.data.model.TriggeredPid
import com.fimbleenterprises.torquepidcaster.domain.repository.MainRepository
import kotlinx.coroutines.flow.Flow

class GetLogEntriesUseCase(private val mainRepository: MainRepository) {

    fun executeMany() : Flow<List<TriggeredPid>> = mainRepository.getLogEntries()

    init { Log.i(TAG, "Initialized:GetLogEntriesUseCase") }
    companion object { private const val TAG = "FIMTOWN|GetLogEntriesUseCase" }
}