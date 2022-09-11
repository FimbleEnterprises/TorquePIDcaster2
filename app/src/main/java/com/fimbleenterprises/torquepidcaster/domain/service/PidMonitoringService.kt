@file:Suppress("unused")

package com.fimbleenterprises.torquepidcaster.domain.service

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleService
import com.fimbleenterprises.torquepidcaster.MyApp.AppPreferences
import com.fimbleenterprises.torquepidcaster.PluginActivity
import com.fimbleenterprises.torquepidcaster.R
import com.fimbleenterprises.torquepidcaster.data.model.FullPid
import com.fimbleenterprises.torquepidcaster.data.model.TriggeredPid
import com.fimbleenterprises.torquepidcaster.domain.usecases.DeleteLogEntriesUseCase
import com.fimbleenterprises.torquepidcaster.domain.usecases.GetLogEntriesUseCase
import com.fimbleenterprises.torquepidcaster.domain.usecases.GetSavedPidsUseCase
import com.fimbleenterprises.torquepidcaster.domain.usecases.InsertLogEntryUseCase
import com.fimbleenterprises.torquepidcaster.util.Helpers
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.joda.time.DateTime
import org.prowl.torque.remote.ITorqueService
import javax.inject.Inject


@AndroidEntryPoint
open class PidMonitoringService : LifecycleService(), ServiceConnection, LifecycleOwner {

    /**
     * This class acts as a messenger to communicate data between this service and a viewmodel
     * without violating MVVM principals.
     */
    @Inject
    lateinit var serviceMessenger: ServiceMessenger

    @Inject
    lateinit var getSavedPidsUseCase: GetSavedPidsUseCase

    @Inject
    lateinit var deleteLogEntriesUseCase: DeleteLogEntriesUseCase

    @Inject
    lateinit var insertLogEntryUseCase: InsertLogEntryUseCase

    @Inject
    lateinit var getLogEntriesUseCase: GetLogEntriesUseCase

    /**
     * Helper class for quickly managing a single notification.
     */
    @Inject
    lateinit var notifs: Helpers.AppNotificationManager

    /**
     * Will get populated oncreate and be updated by an observer observing saved pids as a flow.
     */
    private lateinit var savedPIDs: List<FullPid>

    /**
     * Used to prevent broadcast spamming.
     */
    private var lastBroadcast: Long = System.currentTimeMillis()

    /**
     * Updating the PID values multiple times per second makes for a janky recyclerview despite
     * using DiffUtil so this value is used to throttle the updates.
     */
    private var lastUpdatedPids: Long = System.currentTimeMillis()

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

    private var logEntries: ArrayList<TriggeredPid> = ArrayList()

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
        val notification = notifs.create(
            getString(R.string.app_name),
            getString(R.string.connecting_to_torque),
            false,
            true,
            PluginActivity::class.java
        )
        startForeground(notifs.START_ID, notification)
        serviceMessenger.serviceStarting()
        serviceMessenger.serviceStarted()
        bindToTorqueService()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCreate() {
        super.onCreate()
        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, PIDCASTER_WAKELOCK)
        }

        // Make a call at creation to establish the list of saved pids and then have it updated only
        // as changes are made.  As opposed to calling this on every looper loop.
        CoroutineScope(Main).launch {
            getSavedPidsUseCase.execute().collect { // Gets called whenever record is added/removed
                savedPIDs = it
                Log.i(TAG, "-=PidMonitoringService:onCreate Retrieved initial saved PIDs =-")
            }
        }

        // Grab a handle on our log
        CoroutineScope(Main).launch {
            getLogEntriesUseCase.executeMany().collect {
                logEntries = ArrayList(it)
            }
        }

        if (wakeLock.isHeld) {
            serviceMessenger.wakelockAcquired()
        } else {
            serviceMessenger.wakelockReleased()
        }
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

            if (AppPreferences.useWakelock) {
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
                startService(intent)
                val successfulBind = bindService(intent, this, BIND_AUTO_CREATE)
                if (successfulBind) {
                    Log.i(
                        TAG, "-=PidMonitoringService:bindToTorqueService was successful" +
                                "- now waiting for onServiceConnected to be called... =-")
                    /**
                     * Not really anything to do here. Once you have bound to the service, you can
                     * start calling methods on torqueService.someMethod() - look at the aidl file
                     * for more info on the calls
                     */
                }
            } catch (e: Exception) {
                Log.w(TAG, "bindToTorqueService: ${e.localizedMessage}")
                e.printStackTrace()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopEverything() {
        Log.w(TAG, "-=========================-")
        Log.w(TAG, "-=!!STOPPING EVERYTHING!!=-")
        Log.w(TAG, "-=========================-")
        if (wakeLock.isHeld) {
            wakeLock.release()
            serviceMessenger.wakelockReleased()
        }
        if (torqueService != null) {
            unbindService(this)
        }
        myHandler.removeCallbacksAndMessages(null)
        stopForeground(false)
        notifs.cancel()
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
        notifs.update(
            getString(R.string.default_notif_title),
            getString(R.string.connected_to_torque),
            false,
            true,
            PluginActivity::class.java)
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
        unbindService(this)
        torqueService = null
        serviceMessenger.torqueDisconnected()
        serviceMessenger.ecuDisconnected()
        stopEverything()

        notifs.cancel()
        Log.w(TAG, " -= onServiceDisconnected|${name?.shortClassName} =-")
    }

    /**
     * Starts a runner that does pretty much all of the work by repeatedly
     * calling [getAndProcessTorquePids].
     */
    private fun startMonitoring() {
        notifs.update(
            getString(
                R.string.default_notif_title
            ),
            getString(
                R.string.pidcaster_is_running
            ),
            false,
            true,
            PluginActivity::class.java
        )
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
                stopEverything()
            } else {
                doDefaultBroadcast()
                getAndProcessTorquePids()
                reportEcuConnectionState()
                myHandler.postDelayed(runner!!, (AppPreferences.scanInterval).toLong())
            }
        }
        myHandler.postDelayed(runner!!, (AppPreferences.scanInterval).toLong())
    }

    /**
     * Publish that we are/not connected to the ECU
     */
    private fun reportEcuConnectionState() {
        if (torqueService!!.isConnectedToECU) {
            serviceMessenger.ecuConnected()
        } else {
            serviceMessenger.ecuDisconnected()
        }
    }

    /**
     * Retrieves PIDS from Torque and evaluates them against saved pids and
     * sending broadcasts when alarm conditions are met.
     */
    private fun getAndProcessTorquePids() {

        // Log.i("THREADING", "${Thread.currentThread().name}|MASTER|START")


        // If the activity is killed (manually or otherwise) and restarted, the torqueConnected
        // livedata will not get updated and the fragment will show false despite it actually
        // being connected.  Thus, we update it via the messenger every time this function is called.
        if (torqueService == null) {
            serviceMessenger.torqueDisconnected()
            serviceMessenger.ecuDisconnected()
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
        try {
            val arrayInfo: Array<String> = torqueService!!.getPIDInformation(infoOnlyPids)
            val arrayValue: DoubleArray = torqueService!!.getPIDValuesAsDouble(infoOnlyPids)
            receivedPIDs = FullPid.createMany(arrayInfo, arrayValue)
            receivedPIDs.sortBy {
                it.id
            }
        } catch (e: DeadObjectException) { // Found this in testing when killing the Torque app.
            Log.w(TAG, "-= =============================================================== =-")
            Log.w(TAG, " -= getAndProcessTorquePids failed with DeadObjectException. Pretty " +
                    "sure the Torque service died after we started this method.  Will try to " +
                    "gracefully shut everything down with stopEverything() =-")
            Log.w(TAG, "-= =============================================================== =-")
            stopEverything()
        }

        // Evaluate received pids vs. saved pids and look for values that should be broadcast.
        CoroutineScope(Main).launch {
            // Log.i("THREADING", "${Thread.currentThread().name}|MAINPARENT|START")
            // Run on background thread.
            withContext(IO) {
                //Log.i("THREADING", "${Thread.currentThread().name}|IO|1")
                // We start by looping through all PIDs received from ITorqueService
                receivedPIDs.forEach { receivedPID ->
                    // Loop through the smaller array of saved pids we got from Room
                    savedPIDs.forEach { savedPID ->
                        // If the names match then user is following it.
                        if (receivedPID.id == savedPID.id) {
                            // Assign properties from the saved pid to the received pid before publish
                            receivedPID.isMonitored = true
                            receivedPID.threshold = savedPID.threshold
                            receivedPID.operator = savedPID.operator
                            receivedPID.setBroadcastAction(savedPID.getBroadcastAction())
                            // Check if its value qualifies it to be broadcast.
                            if (receivedPID.shouldBroadcast()) {
                                if ((System.currentTimeMillis() - lastBroadcast)
                                    > MINIMUM_WAIT_TIME_TO_BROADCAST
                                ) {
                                    // Since saved pids do not store a value we assign it using the
                                    // most recent value and then do the actual broadcast.
                                    savedPID.setValue(receivedPID.getValue())

                                    // Send broadcast to 3rd party apps.
                                    doBroadcast(savedPID)

                                    // Broadcast the triggered pid with the receiver which must be done from the main thread.
                                    CoroutineScope(Main).launch {
                                        // Create a TriggeredPid object to send with the messenger.
                                        val triggeredPid = TriggeredPid()
                                        triggeredPid.pidFullname = savedPID.id
                                        triggeredPid.triggeredOnMillis = System.currentTimeMillis()
                                        triggeredPid.operator = savedPID.operator
                                        triggeredPid.broadcastAction = savedPID.getBroadcastAction()
                                        triggeredPid.threshold = savedPID.threshold
                                        triggeredPid.value = savedPID.getValue()
                                        // serviceMessenger.publishTriggeredPid(triggeredPid)
                                        lastBroadcast = System.currentTimeMillis()

                                        // Save this alert to the database log
                                        while(logEntries.size > AppPreferences.maxLogEntries) {
                                            deleteLogEntriesUseCase.execute(logEntries[0])
                                        }
                                        insertLogEntryUseCase.execute(triggeredPid)

                                    } // main thread
                                } // can broadcast (spam check)
                            } // pid alarm exceeded, shouldBroadcast
                        } // fullname == fullname
                    } // for/each saved pid
                } // for/each received pid
                //Log.i("THREADING", "${Thread.currentThread().name}|IO|2")
            } // bg thread

            withContext(Main) {
                //Log.i("THREADING", "${Thread.currentThread().name}|MAIN|1")
                    serviceMessenger.publishFullPids(receivedPIDs)
                    lastUpdatedPids = System.currentTimeMillis()
                    notifs.update(
                        getString(
                            R.string.default_notif_title
                        ),
                        getString(R.string.pidcaster_is_running_with_count, savedPIDs.size),
                        false,
                        true,
                        PluginActivity::class.java
                    )

                    // Keep checking the wakelock and signal that too for good measure.
                    if (wakeLock.isHeld) {
                        serviceMessenger.wakelockAcquired()
                    } else {
                        serviceMessenger.wakelockReleased()
                    }
                //Log.i("THREADING", "${Thread.currentThread().name}|MAIN|2")
            }  // IO thread
            //Log.i("THREADING", "${Thread.currentThread().name}|MAINPARENT|END")
        } // CoroutineScope (IO)

       // Log.i("THREADING", "${Thread.currentThread().name}|MASTER|END")
    }

    /**
     * Constructs and sends a system-wide broadcast that can be consumed by 3rd party apps.
     */
    private fun doBroadcast(pid: FullPid) {

        val intent = Intent(getString(R.string.fully_qualified_broadcast, pid.getBroadcastAction()))
        intent.putExtra(getString(R.string.fully_qualified_broadcast, pid.getBroadcastAction()), pid.getValue())
        sendBroadcast(intent)
        notifs.update(
            getString(R.string.default_notif_title),
            "${getString(R.string.sent_broadcast)}\n${
                getString(
                    R.string.trigger_log2,
                    pid.id,
                    pid.getValue().toString(),
                    pid.operator?.name,
                    pid.threshold.toString()
                )}\n${Helpers.DatesAndTimes.getPrettyDateAndTime(DateTime.now(),false, true )}",
            false,
            true,
            PluginActivity::class.java
        )
    }

    // Sends out the broadcast action declaring the connected to ECU state.
    private fun doDefaultBroadcast() {
        try {
            val action: String = if (torqueService?.isConnectedToECU == true) {
                getString(R.string.fully_qualified_broadcast, AppPreferences.ecuConnectedBroadcastAction)
            } else {
                getString(R.string.fully_qualified_broadcast, AppPreferences.ecuDisconnectedBroadcastAction)
            }
            serviceMessenger.publishDefaultBroadcastAction(action)
            val intent = Intent(action)
            sendBroadcast(intent)
            Log.v(TAG, "doDefaultBroadcast: Broadcast sent: $action")
        } catch (exception:DeadObjectException) {
            Log.w(TAG, "-= =============================================================== =-")
            Log.w(TAG, " -= getAndProcessTorquePids failed with DeadObjectException. Pretty " +
                    "sure the Torque service died after we started this method.  Will try to " +
                    "gracefully shut everything down with stopEverything() =-")
            Log.w(TAG, "-= =============================================================== =-")
            stopEverything()
        }
    }

    init { Log.i(TAG, "Initialized:PidMonitoringService") }
    companion object {

        private const val TAG = "FIMTOWN|PMService"
        private const val PIDCASTER_WAKELOCK = "PIDCASTER:WAKELOCK"

        // When the interval is set low (like .1) or so it can create a backlog of broadcasts.  Even
        // if the interval is changed while the backlog exists it will continue to fire.  This will
        // hopefully prevent that.
        private const val MINIMUM_WAIT_TIME_TO_BROADCAST = 1000

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