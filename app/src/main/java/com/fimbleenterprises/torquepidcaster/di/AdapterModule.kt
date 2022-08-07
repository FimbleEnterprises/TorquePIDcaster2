package com.fimbleenterprises.torquepidcaster.di

import android.content.Context
import android.util.Log
import com.fimbleenterprises.torquepidcaster.presentation.adapters.TriggeredPidsAdapter
import com.fimbleenterprises.torquepidcaster.presentation.adapters.PIDsAdapter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.FragmentComponent
import dagger.hilt.android.qualifiers.ApplicationContext

@Module
@InstallIn(FragmentComponent::class)
class AdapterModule {

    @Provides
    fun providePIDsAdapter(@ApplicationContext context: Context): PIDsAdapter {
        return PIDsAdapter(context)
    }

    @Provides
    fun provideTriggeredPidsAdapter(@ApplicationContext context: Context): TriggeredPidsAdapter {
        return TriggeredPidsAdapter(context)
    }

/*    @Provides
    fun provideAlarmsAdapter(@ApplicationContext context: Context): AlarmsAdapter {
        return AlarmsAdapter(context)
    }*/

    companion object {
        private const val TAG = "FIMTOWN|AdapterModule"
    }

    init {
        Log.i(TAG, "Initialized:AdapterModule")
    }
}