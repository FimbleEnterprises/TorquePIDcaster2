package com.fimbleenterprises.torquepidcaster.domain.usecases

import android.util.Log
import com.fimbleenterprises.torquepidcaster.data.model.FullPid
import com.fimbleenterprises.torquepidcaster.domain.repository.MainRepository
import kotlinx.coroutines.flow.Flow

class GetSavedPidsUseCase(private val mainRepository: MainRepository) {

    fun execute() : Flow<List<FullPid>> = mainRepository.getSavedPids()

    init { Log.i(TAG, "Initialized:SavePidUseCase") }
    companion object { private const val TAG = "FIMTOWN|SavePidUseCase" }
}