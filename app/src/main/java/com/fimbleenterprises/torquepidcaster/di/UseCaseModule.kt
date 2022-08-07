package com.fimbleenterprises.torquepidcaster.di

import android.util.Log
import com.fimbleenterprises.torquepidcaster.domain.repository.MainRepository
import com.fimbleenterprises.torquepidcaster.domain.usecases.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class UseCaseModule {

    @Singleton
    @Provides
    fun provideSavePidUseCase(
        mainRepository: MainRepository
    ): SavePidUseCase {
        return SavePidUseCase(mainRepository)
    }

    @Singleton
    @Provides
    fun provideDeletePidUseCase(
        mainRepository: MainRepository
    ): DeletePidUseCase {
        return DeletePidUseCase(mainRepository)
    }

    @Singleton
    @Provides
    fun provideGetSavedPidsUseCase(
        mainRepository: MainRepository
    ): GetSavedPidsUseCase {
        return GetSavedPidsUseCase(mainRepository)
    }

    @Singleton
    @Provides
    fun provideInsertLogEntryUseCase(
        mainRepository: MainRepository
    ): InsertLogEntryUseCase {
        return InsertLogEntryUseCase(mainRepository)
    }

    @Singleton
    @Provides
    fun provideGetLogEntryUseCase(
        mainRepository: MainRepository
    ): GetLogEntriesUseCase {
        return GetLogEntriesUseCase(mainRepository)
    }

    @Singleton
    @Provides
    fun provideDeleteLogEntryUseCase(
        mainRepository: MainRepository
    ): DeleteLogEntriesUseCase {
        return DeleteLogEntriesUseCase(mainRepository)
    }

    init { Log.i(TAG, "Initialized:UseCaseModule") }
    companion object { private const val TAG = "FIMTOWN|UseCaseModule" }
}