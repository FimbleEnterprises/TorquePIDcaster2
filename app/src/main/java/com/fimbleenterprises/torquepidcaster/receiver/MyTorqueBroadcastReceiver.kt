package com.fimbleenterprises.torquepidcaster.receiver

import android.annotation.SuppressLint
import android.content.*
import android.util.Log
import android.widget.Toast
import com.fimbleenterprises.torquepidcaster.MyApp
import com.fimbleenterprises.torquepidcaster.R
import com.fimbleenterprises.torquepidcaster.domain.service.PidMonitoringService
import com.fimbleenterprises.torquepidcaster.util.Helpers


@SuppressLint("LongLogTag")
open class MyTorqueBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            APP_LAUNCHED -> {
                Log.w(TAG, "onReceive: Received Torque broadcast: APP_LAUNCHED")
                if (Helpers.Application.isIgnoringBatteryOptimizations(context)
                    && MyApp.AppPreferences.startServiceWithTorque) {
                    val serviceStartIntent = Intent(context, PidMonitoringService::class.java)
                    context.startService(serviceStartIntent)
                    Toast.makeText(context, context.getString(R.string.pidcaster_starting_up), Toast.LENGTH_SHORT).show()
                }
            }
            OBD_CONNECTED -> {
                Log.w(TAG, "onReceive: Received Torque broadcast: OBD_CONNECTED")
            }
            OBD_DISCONNECTED -> {
                Log.w(TAG, "onReceive: Received Torque broadcast: OBD_DISCONNECTED")
            }
            APP_QUITTING -> {
                Log.w(TAG, "onReceive: Received Torque broadcast: APP_STOPPED")
                val serviceStartIntent = Intent(context, PidMonitoringService::class.java)
                context.stopService(serviceStartIntent)
                Toast.makeText(context, "Torque quitting!", Toast.LENGTH_SHORT).show()
            }
            STOP_PIDCASTER -> {
                val serviceStartIntent = Intent(context, PidMonitoringService::class.java)
                context.stopService(serviceStartIntent)
            }
        }
    }

    init {
        Log.i(TAG, "-= Initialized:MyTorqueBroadcastReceiver =-")
    }

    companion object {
        private const val TAG = "-= FIMTOWN|MyTorqueBroadcastReceiver =-"
        const val APP_LAUNCHED = "org.prowl.torque.APP_LAUNCHED"
        const val OBD_CONNECTED = "org.prowl.torque.OBD_CONNECTED"
        const val APP_QUITTING = "org.prowl.torque.APP_QUITTING"
        const val OBD_DISCONNECTED = "org.prowl.torque.OBD_DISCONNECTED"
        const val STOP_PIDCASTER = "STOP_PIDCASTER"
        val intentFilter = IntentFilter(
            APP_LAUNCHED
            .plus(OBD_CONNECTED)
            .plus(OBD_DISCONNECTED)
            .plus(APP_QUITTING))
    }
}