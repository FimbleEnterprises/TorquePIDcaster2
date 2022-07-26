package com.fimbleenterprises.torquepidcaster.di

import android.app.Application
import android.util.Log
import androidx.room.Room
import com.fimbleenterprises.torquepidcaster.data.db.PidsDao
import com.fimbleenterprises.torquepidcaster.data.db.PidsDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.Executors
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class DatabaseModule {

    @Singleton
    @Provides
    fun providesPidsDatabase(app: Application): PidsDatabase {
        return Room.databaseBuilder(app, PidsDatabase::class.java, "pidsdatabase")
            .fallbackToDestructiveMigration()
            .setQueryCallback(
                fun(sqlQuery: String, bindArgs: MutableList<Any>) {
                    Log.v(TAG, "-=QUERY:$sqlQuery ARGS:$bindArgs =-")
                }, Executors.newSingleThreadExecutor()
            )
            //.allowMainThreadQueries()
            .build()
    }

    @Singleton
    @Provides
    fun providePidsDAO(pidsDb: PidsDatabase): PidsDao {
        return pidsDb.getPidsDao()
    }

    init { Log.i(TAG, "Initialized:DatabaseModule") }
    companion object { private const val TAG = "FIMTOWN|DatabaseModule" }
}