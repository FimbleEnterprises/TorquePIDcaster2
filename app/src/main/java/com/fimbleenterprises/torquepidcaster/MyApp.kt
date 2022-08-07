package com.fimbleenterprises.torquepidcaster

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.preference.PreferenceManager
import com.fimbleenterprises.torquepidcaster.domain.service.PidMonitoringPsuedoService
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.Executors
import javax.inject.Singleton

@HiltAndroidApp
class MyApp : Application() {

    override fun getApplicationContext(): Context {
        return this
    }

    override fun onCreate() {
        super.onCreate()
        AppPreferences.init(this)
        // region EXPERIMENTAL STUFF - NOT IMPLEMENTED
        // val executor = Executors.newFixedThreadPool(1)
        // val myPsuedoService: PidMonitoringPsuedoService = PidMonitoringPsuedoService(this)
        /*executor.execute(Runnable {
            while (true) {

               // myPseudoService.bindToTorqueService()

                while (AppPreferences.runService) {
                    Log.d(TAG, "SERVICE RUNNING - I will log this line every 3 seconds forever")
                    Thread.sleep(3000);
                }

                while (!AppPreferences.runService) {
                    Log.d(TAG, "SERVICE NOT RUNNING - I will log this line every 3 seconds forever")
                    Thread.sleep(3000)
                }

            }
        })*/
        // endregion

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
        private const val PREF_WHILE_CONNECTED_ACTION = "PREF_WHILE_CONNECTED_ACTION"
        private const val PREF_WHILE_DISCONNECTED_ACTION = "PREF_WHILE_DISCONNECTED_ACTION"
        private const val PREF_SHOW_VALUES_IN_LISTVIEW = "PREF_SHOW_VALUES_IN_LISTVIEW"
        private const val PREF_USE_IMPERIAL = "PREF_USE_IMPERIAL"
        private const val PREF_START_SERVICE_WITH_TORQUE = "PREF_START_SERVICE_WITH_TORQUE"

        /**
         * The time in MS between calls to the Torque service to update the PIDs
         */
        var scanInterval: Float
            get() = prefs.getString(PREF_SCAN_INTERVAL, "500")!!.toFloat()
            set(value: Float) {
                prefs.edit().putString(PREF_SCAN_INTERVAL, value.toString()).apply()
            }

        var useImperial: Boolean
        get() = prefs.getBoolean(PREF_USE_IMPERIAL, false)
        set(value: Boolean) {
            prefs.edit().putBoolean(PREF_USE_IMPERIAL, value).apply()
        }

        /*var showValuesInListView: Boolean
            get() = prefs.getBoolean(PREF_SHOW_VALUES_IN_LISTVIEW, false)
            set(value: Boolean) {
                prefs.edit().putBoolean(PREF_SHOW_VALUES_IN_LISTVIEW, value).apply()
            }*/

        var startServiceWithTorque: Boolean
            get() = prefs.getBoolean(PREF_START_SERVICE_WITH_TORQUE, true)
            set(value: Boolean) {
                prefs.edit().putBoolean(PREF_START_SERVICE_WITH_TORQUE, value).apply()
            }

        var disconnectedFromEcu: String
            get() = prefs.getString(PREF_WHILE_DISCONNECTED_ACTION, "ECU_DISCONNECTED")!!
            set(value: String) {
                prefs.edit().putString(PREF_WHILE_DISCONNECTED_ACTION, value).apply()
            }

        var connectedToEcu: String
            get() = prefs.getString(PREF_WHILE_CONNECTED_ACTION, "ECU_CONNECTED")!!
            set(value: String) {
                prefs.edit().putString(PREF_WHILE_CONNECTED_ACTION, value).apply()
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
            prefs = PreferenceManager.getDefaultSharedPreferences(context)
        }

    }

    init { Log.i(TAG, "Initialized:MyApp") }
    companion object { private const val TAG = "FIMTOWN|MyApp" }
}
