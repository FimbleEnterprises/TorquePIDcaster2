package com.fimbleenterprises.torquepidcaster.di

import android.util.Log
import com.fimbleenterprises.torquepidcaster.data.repository.MainRepositoryImpl
import com.fimbleenterprises.torquepidcaster.data.repository.datasource.LocalDatasource
import com.fimbleenterprises.torquepidcaster.domain.repository.MainRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class RepositoryModule {

    @Singleton
    @Provides
    fun provideMainRepository(
        localDatasource: LocalDatasource
    ): MainRepository {
        return MainRepositoryImpl(localDatasource)
    }

    init { Log.i(TAG, "Initialized:RepositoryModule") }
    companion object { private const val TAG = "FIMTOWN|RepositoryModule" }

}
