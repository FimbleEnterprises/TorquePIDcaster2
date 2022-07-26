@file:Suppress("unused")

package com.fimbleenterprises.torquepidcaster.domain.service

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Configuration
import android.os.*
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.LifecycleService
import com.fimbleenterprises.torquepidcaster.MyApp
import com.fimbleenterprises.torquepidcaster.PluginActivity
import com.fimbleenterprises.torquepidcaster.R
import com.fimbleenterprises.torquepidcaster.data.model.FullPid
import com.fimbleenterprises.torquepidcaster.domain.usecases.GetSavedPidsUseCase
import com.fimbleenterprises.torquepidcaster.util.Helpers
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import org.prowl.torque.remote.ITorqueService
import java.lang.Runnable
import javax.inject.Inject


@AndroidEntryPoint
open class PidMonitoringService : LifecycleService(), ServiceConnection {

    /**
     * This class acts as a messenger to communicate data between this service and a viewmodel
     * without violating MVVM principals.
     */
    @Inject
    lateinit var serviceMessenger: ServiceMessenger
    
    @Inject
    lateinit var getSavedPidsUseCase: GetSavedPidsUseCase

    /**
     * Helper class for quickly managing a single notification.
     */
    @Inject
    lateinit var notifs: Helpers.Notifications

    /**
     * A third party service provided by the Torque application and connected to via an aidl interface.
     */
    private var torqueService: ITorqueService? = null

    /**
     * Retrieved from the Torque service and stored in this array.  These are not the values, these
     * are just the names and information about the OBD data they represent.  The values must be
     * queried individually.  We will use the FullPid class to hold both this informative data
     * about the PID as well as the actual values from Torque -> ECU.
     */
    private var infoOnlyPids: Array<String>? = null

    /**
     * Uses our FullPid class to store the pids received from Torque as well as their values in one object.
     */
    private var receivedPIDs: ArrayList<FullPid> = ArrayList()

    private var isDebugMode: Boolean = false
    private var binder: IBinder? = null
    private var myHandler: Handler = Handler(Looper.myLooper()!!)
    private var runner: Runnable? = null

    /**
     * I'm not positive but I think we'll need a wakelock to ensure pids are monitored and broadcasts
     * are sent when the device is pushed into the background.
     */
    private lateinit var wakeLock: PowerManager.WakeLock

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Show a the mandatory notification for a foreground service.
        val notification = notifs.create(getString(R.string.app_name), getString
            (R.string.connecting_to_torque), true, PluginActivity::class.java)
        isDebugMode = intent?.getBooleanExtra(DEBUG_MODE, false)!!
        startForeground(notifs.START_ID, notification)
        serviceMessenger.serviceStarted()
        bindToTorqueService()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCreate() {
        super.onCreate()
        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, PIDCASTER_WAKELOCK)
        }
        if (wakeLock.isHeld) {
            serviceMessenger.wakelockAcquired()
        } else {
            serviceMessenger.wakelockReleased()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.i(TAG, "-=PidMonitoringService:onConfigurationChanged =-")
    }

    override fun onDestroy() {
        super.onDestroy()
        stopEverything()
        serviceMessenger.torqueDisconnected()
        serviceMessenger.serviceStopped()
        Log.i(TAG, " -=PidMonitoringService:onDestroy =-")
    }

    /**
     * Connects to the Torque application's service using the .aidl file.
     */
    @SuppressLint("WakelockTimeout")
    private fun bindToTorqueService() {
        try {

            if (MyApp.AppPreferences.useWakelock) {
                if (!wakeLock.isHeld) {
                    wakeLock.acquire()
                    serviceMessenger.wakelockAcquired()
                }
            }

            val intent = Intent().setClassName(
                TORQUE_PACKAGE_NAME,
                TORQUE_CLASS_NAME
            )
            try {
                // Bind to the torque service
                val successfulBind = bindService(intent, this, 0)
                if (successfulBind) {
                    Log.i(
                        TAG, "-=PidMonitoringService:bindToTorqueService was successful" +
                            "- now waiting for onServiceConnected to be called... =-")
                    /**
                     * Not really anything to do here. Once you have bound to the service, you can
                     * start calling methods on torqueService.someMethod()  - look at the aidl file
                     * for more info on the calls
                     */
                }
            } catch (e: Exception) {
                Log.e(TAG, "bindToTorqueService: ${e.localizedMessage}")
                e.printStackTrace()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopEverything() {
        if (wakeLock.isHeld) {
            wakeLock.release()
            serviceMessenger.wakelockReleased()
        }
        myHandler.removeCallbacksAndMessages(null)
        stopForeground(true)
        stopSelf()
    }

    /**
     * Bits of service code. You usually won't need to change this.  Worth noting that this WILL NOT
     * be called if Torque isn't running which shouldn't ever happen in production (though it can
     * CERTAINLY HAPPEN IN DEVELOPMENT!  I WANT MY TWO HOURS BACK, DAMMIT!)
     */
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onServiceConnected(name: ComponentName, service: IBinder?) {
        // Update subscribers that the Torque service has been bound and is ready for use.
        serviceMessenger.torqueConnected()
        binder = service
        notifs.update(getString(R.string.default_notif_title), getString(R.string.connected_to_torque), false, PluginActivity::class.java)
        torqueService = ITorqueService.Stub.asInterface(service)
        torqueService?.setDebugTestMode(isDebugMode)
        assert(torqueService != null)
        startMonitoring()
    }

    /**
     * Will be called if Torque is shut down and I cannot think of another reason...
     */
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onServiceDisconnected(name: ComponentName?) {

        torqueService = null
        serviceMessenger.torqueDisconnected()
        stopEverything()

        notifs.update(getString(R.string.default_notif_title),
            getString(R.string.failed_to_connect_torque_notif_msg), false, PluginActivity::class.java)
        Log.e(TAG, " -= onServiceDisconnected|${name?.shortClassName} =-")
    }

    /**
     * Starts a runner that does pretty much all of the work by repeatedly
     * calling [getAndProcessTorquePids].
     */
    private fun startMonitoring() {
        if (runner != null) {
            try {
                myHandler.removeCallbacks(runner!!)
                myHandler.removeCallbacksAndMessages(null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        runner = Runnable {
            if (torqueService == null) {
                myHandler.removeCallbacks(runner!!)
            } else {
                getAndProcessTorquePids()
                myHandler.postDelayed(runner!!, (MyApp.AppPreferences.scanInterval * 1000).toLong())
            }
        }
        myHandler.postDelayed(runner!!, (MyApp.AppPreferences.scanInterval * 1000).toLong())
    }

    /**
     * Retrieves PIDS from Torque and evaluates them against saved pids and
     * sending broadcasts when alarm conditions are met.
     */
    private fun getAndProcessTorquePids() {

        // If the activity is killed (manually or otherwise) and restarted, the torqueConnected
        // livedata will not get updated and the fragment will show false despite it actually
        // being connected.  Thus, we update it via the messenger every time this function is called.
        if (torqueService == null) {
            serviceMessenger.torqueDisconnected()
            myHandler.removeCallbacksAndMessages(null)
            return
        }

        // Alert all subscribers that Torque is now connected.
        serviceMessenger.torqueConnected()

        // Container to load all populated PIDs into.
        receivedPIDs = ArrayList()

        // Get basic, raw PID info from Torque if none exist
        if (infoOnlyPids == null || infoOnlyPids!!.isEmpty()) {
            infoOnlyPids = torqueService!!.listAllPIDs()
        }

        // Using the array of basic info pids, query ITorqueService for more info about the PIDs
        // as well as the values for the PIDs
        val arrayInfo: Array<String> = torqueService!!.getPIDInformation(infoOnlyPids)
        val arrayValue: DoubleArray = torqueService!!.getPIDValuesAsDouble(infoOnlyPids)
        receivedPIDs = FullPid.createMany(arrayInfo, arrayValue)

        // Evaluate received pids vs. saved pids and look for values that should be broadcast.
        CoroutineScope(Dispatchers.Main).launch {
            // Get the saved PIDs from the database
            getSavedPidsUseCase.execute().collect { savedPIDs ->
                // We start by looping through all PIDs received from ITorqueService
                receivedPIDs.forEach { receivedPID ->
                    // Loop through the smaller array of saved pids we got from Room
                    savedPIDs.forEach { savedPID ->
                        // If the names match then user is following it.
                        if (receivedPID.fullName == savedPID.fullName) {
                            // Assign properties from the saved pid to the received pid before publish
                            receivedPID.isMonitored = true
                            receivedPID.threshold = savedPID.threshold
                            receivedPID.operator = savedPID.operator
                            // Check if its value qualifies it to be broadcast.
                            if (receivedPID.shouldBroadcast()) {
                                // Since saved pids do not store a value we assign it using the
                                // most recent value and then do the actual broadcast.
                                savedPID.setValue(receivedPID.getValue())
                                doBroadcast(savedPID)
                                // Publish the triggered pid.
                                serviceMessenger.publishTriggeredPid(savedPID)
                            } // shouldBroadcast
                        } // fullname == fullname
                    } // for/each saved pid
                } // for/each received pid

                // Publish the arraylist via the ServiceMessenger to all subscribers.  This is
                // NOT a broadcast event such that an alarm has been triggered (that happens above).
                // This is purely for showing PIDs in a recyclerview or other informational purposes.
                serviceMessenger.publishFullPids(receivedPIDs)

                // Keep checking the wakelock and signal that too for good measure.
                if (wakeLock.isHeld) {
                    serviceMessenger.wakelockAcquired()
                } else {
                    serviceMessenger.wakelockReleased()
                }
            }

        }
    }

    /**
     * Constructs and sends a system-wide broadcast that can be consumed by 3rd party apps.
     */
    private fun doBroadcast(fullPid: FullPid) {
        val intent = Intent(fullPid.broadcastAction)
        intent.putExtra(fullPid.broadcastAction, fullPid.getValue())
        sendBroadcast(intent)
    }

    init { Log.i(TAG, "Initialized:PidMonitoringService") }
    companion object {

        private const val TAG = "FIMTOWN|PMService"
        private const val PIDCASTER_WAKELOCK = "PIDCASTER:WAKELOCK"

        /**
         * Used when binding
         */
        const val TORQUE_PACKAGE_NAME = "org.prowl.torque"
        const val TORQUE_CLASS_NAME = "org.prowl.torque.remote.TorqueService"
        const val DEBUG_MODE = "START_DEBUG_MODE"

        /**
         * Not implemented yet
         */
        var isPaused = false

        /**
         * Torque is connected to the car's ECU via OBD.
         */
        var connectedToECU = false

        /**
         * This service has been started and is running.  Doesn't necessarily mean that we are connected
         * to the Torque service however.
         */
        var serviceRunning = false

    }


}