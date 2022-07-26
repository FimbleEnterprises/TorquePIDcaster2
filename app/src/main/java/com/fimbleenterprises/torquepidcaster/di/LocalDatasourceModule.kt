package com.fimbleenterprises.torquepidcaster.di

import com.fimbleenterprises.torquepidcaster.data.db.PidsDao
import com.fimbleenterprises.torquepidcaster.data.repository.datasource.LocalDatasource
import com.fimbleenterprises.torquepidcaster.data.repository.datasourceImpl.LocalDatasourceImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class LocalDataSourceModule {

    @Provides
    @Singleton
    fun providesLocalDataSource(pidsDao: PidsDao): LocalDatasource {
        return LocalDatasourceImpl(pidsDao)
    }

}