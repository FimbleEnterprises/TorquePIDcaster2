package com.fimbleenterprises.torquepidcaster

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Singleton

@HiltAndroidApp
class MyApp : Application() {

    override fun getApplicationContext(): Context {
        return this
    }

    override fun onCreate() {
        super.onCreate()
        AppPreferences.init(this)
        /**
         * This is deprecated and will be auto-initialized unless you opt out using a meta-data
         * line in the manifest.
         */
        /*FacebookSdk.sdkInitialize(applicationContext) {
            Log.i(TAG, "-= MyApp:onCreate FacebookSdk was initialized! =-")
        }
        AppEventsLogger.activateApp(this)*/
    }

    @Singleton
    object AppPreferences {
        private const val NAME = "ALL_PREFS"
        private const val MODE = Context.MODE_PRIVATE
        private lateinit var prefs: SharedPreferences

        // list of preferences
        private const val PREF_USE_WAKELOCK = "PREF_USE_WAKELOCK"
        private const val PREF_SCAN_INTERVAL = "PREF_SCAN_INTERVAL"

        /**
         * The time in MS between calls to the Torque service to update the PIDs
         */
        var scanInterval: Float
            get() = prefs.getFloat(PREF_SCAN_INTERVAL, 250f)
            set(value: Float) {
                prefs.edit().putFloat(PREF_SCAN_INTERVAL, value).apply()
            }

        /**
         * The time in MS between calls to the Torque service to update the PIDs
         */
        var useWakelock: Boolean
            get() = prefs.getBoolean(PREF_USE_WAKELOCK, false)
            set(value: Boolean) {
                prefs.edit().putBoolean(PREF_USE_WAKELOCK, value).apply()
            }

        fun init(context: Context) {
            prefs = context.getSharedPreferences(NAME, MODE)
        }

    }

    init { Log.i(TAG, "Initialized:MyApp") }
    companion object { private const val TAG = "FIMTOWN|MyApp" }
}
