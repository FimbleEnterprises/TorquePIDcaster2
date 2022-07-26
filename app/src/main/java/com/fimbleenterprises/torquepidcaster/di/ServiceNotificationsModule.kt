package com.fimbleenterprises.pidcaster.presentation.di

import android.content.Context
import com.fimbleenterprises.torquepidcaster.util.Helpers
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class ServiceNotificationsModule {

    @Singleton
    @Provides
    fun providesServiceNotifications(@ApplicationContext context: Context): Helpers.Notifications {
        return Helpers.Notifications(context)
    }

}