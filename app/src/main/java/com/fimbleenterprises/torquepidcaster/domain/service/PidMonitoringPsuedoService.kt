package com.fimbleenterprises.torquepidcaster.domain.service
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.fimbleenterprises.torquepidcaster.MyApp
import com.fimbleenterprises.torquepidcaster.R
import com.fimbleenterprises.torquepidcaster.data.model.FullPid
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import org.prowl.torque.remote.ITorqueService
import java.lang.Runnable

@AndroidEntryPoint
class PidMonitoringPsuedoService(private val context: Context) : AppCompatActivity(),  ServiceConnection {

    /**
     * This class acts as a messenger to communicate data between this service and a viewmodel
     * without violating MVVM principals.
     */
    val serviceMessenger: ServiceMessenger = ServiceMessenger()


    /**
     * Helper class for quickly managing a single notification.
     */
    /*@Inject
    lateinit var notifs: Helpers.Notifications*/

    /**
     * Used to prevent broadcast spamming.
     */
    private var lastBroadcast: Long = System.currentTimeMillis()

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
     * Connects to the Torque application's service using the .aidl file.
     */
    @SuppressLint("WakelockTimeout")
    fun bindToTorqueService() {
        try {

            val intent = Intent().setClassName(
                PidMonitoringService.TORQUE_PACKAGE_NAME,
                PidMonitoringService.TORQUE_CLASS_NAME
            )
            try {
                // Bind to the torque service
                context.startService(intent)
                val successfulBind = context.bindService(intent, this, BIND_AUTO_CREATE)
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
        // notifs.update(context.getString(R.string.default_notif_title), context.getString(R.string.connected_to_torque), false, PluginActivity::class.java)
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
        // unbindService(this)
        torqueService = null
        serviceMessenger.torqueDisconnected()
        // stopEverything()

        /*notifs.update(context.getString(R.string.default_notif_title),
            context.getString(R.string.failed_to_connect_torque_notif_msg), false, PluginActivity::class.java)*/
        Log.w(TAG, " -= onServiceDisconnected|${name?.shortClassName} =-")
    }

    /**
     * Starts a runner that does pretty much all of the work by repeatedly
     * calling [getAndProcessTorquePids].
     */
    fun startMonitoring() {
        /*notifs.update(
            context.getString(
                R.string.default_notif_title
            ),
            context.getString(
                R.string.pidcaster_is_running
            ),
            false,
            PluginActivity::class.java
        )*/
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
    fun getAndProcessTorquePids() {

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
        CoroutineScope(Dispatchers.IO).launch {
            // Get the saved PIDs from the database
            /*getSavedPidsUseCase.execute().collect { savedPIDs ->
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
                            receivedPID.broadcastAction = savedPID.broadcastAction
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

                                    // Create a TriggeredPid object to send with the messenger.
                                    withContext(Main) {
                                        val triggeredPid = TriggeredPid()
                                        triggeredPid.pidFullname = savedPID.id
                                        triggeredPid.triggeredOnMillis = System.currentTimeMillis()
                                        triggeredPid.operator = savedPID.operator
                                        triggeredPid.broadcastAction = savedPID.broadcastAction
                                        triggeredPid.threshold = savedPID.threshold
                                        triggeredPid.value = savedPID.getValue()
                                        serviceMessenger.publishTriggeredPid(triggeredPid)
                                    }
                                }
                            } // shouldBroadcast
                        } // fullname == fullname
                    } // for/each saved pid
                } // for/each received pid

                withContext(Main) {
                    // Publish that we are/not connected to the ECU
                    if (torqueService!!.isConnectedToECU) {
                        serviceMessenger.ecuConnected()
                    } else {
                        serviceMessenger.ecuDisconnected()
                    }

                    // Publish the arraylist via the ServiceMessenger to all subscribers.  This is
                    // NOT a broadcast event such that an alarm has been triggered (that happens above).
                    // This is purely for showing PIDs in a recyclerview or other informational purposes.
                    serviceMessenger.publishFullPids(receivedPIDs)
                    // lastUpdatedValues = System.currentTimeMillis()
                }
            } // GetSavedPidsUseCase*/
        } // CoroutineScope (IO)
    }

    /**
     * Constructs and sends a system-wide broadcast that can be consumed by 3rd party apps.
     */
    private fun doBroadcast(pid: FullPid) {

        val intent = Intent(context.getString(R.string.broadcast_preamble, pid.getBroadcastAction()))
        intent.putExtra(context.getString(R.string.broadcast_preamble, pid.getBroadcastAction()), pid.getValue())
        context.sendBroadcast(intent)
        lastBroadcast = System.currentTimeMillis()
        /*notifs.update(
            context.getString(R.string.default_notif_title),
            "Sent a broadcast!\n${context.getString(
                R.string.trigger_log2,
                pid.id,
                pid.getValue().toString(),
                pid.operator?.name,
                pid.threshold.toString())}\n${Helpers.DatesAndTimes.getPrettyDateAndTime(DateTime.now(),false, true )}",
            false,
            PluginActivity::class.java
        )*/
    }

    init { Log.i(TAG, "Initialized:PidMonitoringPsuedoService") }
    companion object {
        private const val TAG = "FIMTOWN|PidMonitoringPsuedoService"

        // When the interval is set low (like .1) or so it can create a backlog of broadcasts.  Even
        // if the interval is changed while the backlog exists it will continue to fire.  This will
        // hopefully prevent that.
        private const val MINIMUM_WAIT_TIME_TO_BROADCAST = 1000
    }

}