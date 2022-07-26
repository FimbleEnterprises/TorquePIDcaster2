package com.fimbleenterprises.pidcaster

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.fimbleenterprises.torquepidcaster.domain.service.PidMonitoringService


/**
 * Created by Matt on 11/2/2016.
 */
@SuppressLint("LongLogTag")
open class MyTorqueBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            APP_LAUNCHED -> {
                Log.w(TAG, "onReceive: Received Torque broadcast: APP_LAUNCHED")
                val serviceStartIntent = Intent(context, PidMonitoringService::class.java)
                context.startForegroundService(serviceStartIntent)
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
        val intentFilter = IntentFilter(
            APP_LAUNCHED
            .plus(OBD_CONNECTED)
            .plus(OBD_DISCONNECTED)
            .plus(APP_QUITTING))
    }
}