package com.fimbleenterprises.torquepidcaster.presentation.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.fimbleenterprises.torquepidcaster.domain.service.ServiceMessenger
import com.fimbleenterprises.torquepidcaster.domain.usecases.*
import javax.inject.Singleton

/**
 * Since our ViewModel class has constructors we need to create this factory class in order to
 * create one as viewmodels that contain constructors cannot be directly instantiated (this is just
 * one of the quirks of Android's ViewModel framework).  To create the ViewModel you will create
 * it like so:
 * ```
 * viewModel = ViewModelProvider(this,factory).get(NewsViewModel::class.java)
 */
@SuppressLint("LongLogTag")
@Singleton
@Suppress("UNCHECKED_CAST")
class MainViewModelFactory(
    private val app:Application,
    private val serviceMessenger: ServiceMessenger,
    private val savePidUseCase: SavePidUseCase,
    private val getSavedPidsUseCase: GetSavedPidsUseCase,
    private val deletePidUseCase: DeletePidUseCase,
    private val insertLogEntryUseCase : InsertLogEntryUseCase,
    private val getLogEntryUseCase : GetLogEntriesUseCase,
    private val deleteLogEntriesUseCase: DeleteLogEntriesUseCase
):ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel(
            app,
            serviceMessenger,
            savePidUseCase,
            getSavedPidsUseCase,
            deletePidUseCase,
            insertLogEntryUseCase,
            getLogEntryUseCase,
            deleteLogEntriesUseCase
        ) as T
    }

    companion object {
        private const val TAG = "FIMTOWN|SportsdbViewModelFactory"
    }

    init {
        Log.i(TAG, "Initialized:SportsdbViewModelFactory")
    }
}









