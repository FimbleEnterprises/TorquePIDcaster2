package com.fimbleenterprises.torquepidcaster.di

import com.fimbleenterprises.torquepidcaster.MyApp
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class MyAppModule {

    @Singleton
    @Provides
    fun providesMyApp() : MyApp {
        return MyApp()
    }

}