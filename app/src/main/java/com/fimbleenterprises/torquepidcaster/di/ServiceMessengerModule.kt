package com.fimbleenterprises.pidcaster.presentation.di

import android.util.Log
import com.fimbleenterprises.torquepidcaster.domain.service.ServiceMessenger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class ServiceMessengerModule {

    @Singleton
    @Provides
    fun provideServiceMessenger(): ServiceMessenger {
        return ServiceMessenger()
    }

    companion object {
        private const val TAG = "FIMTOWN|AdapterModule"
    }

    init {
        Log.i(TAG, "Initialized:AdapterModule")
    }
}