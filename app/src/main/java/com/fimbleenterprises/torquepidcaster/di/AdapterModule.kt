package com.fimbleenterprises.torquepidcaster.di

import android.content.Context
import android.util.Log
import com.fimbleenterprises.torquepidcaster.presentation.adapter.PIDsAdapter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.FragmentComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(FragmentComponent::class)
class AdapterModule {

    @Provides
    fun providePIDsAdapter(@ApplicationContext context: Context): PIDsAdapter {
        return PIDsAdapter(context)
    }

    companion object {
        private const val TAG = "FIMTOWN|AdapterModule"
    }

    init {
        Log.i(TAG, "Initialized:AdapterModule")
    }
}