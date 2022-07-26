@file:Suppress("unused")

package com.fimbleenterprises.torquepidcaster.presentation.viewmodel
import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.*
import com.fimbleenterprises.torquepidcaster.data.model.FullPid
import com.fimbleenterprises.torquepidcaster.domain.service.*
import com.fimbleenterprises.torquepidcaster.domain.usecases.DeletePidUseCase
import com.fimbleenterprises.torquepidcaster.domain.usecases.GetSavedPidsUseCase
import com.fimbleenterprises.torquepidcaster.domain.usecases.SavePidUseCase
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import javax.inject.Singleton

@Singleton
class MainViewModel (
    private val app: Application,
    private val serviceMessenger: ServiceMessenger,
    private val savePidUseCase: SavePidUseCase,
    private val getSavedPidsUseCase: GetSavedPidsUseCase,
    private val deletePidUseCase: DeletePidUseCase
) : ViewModel(), ServiceMessenger.ServiceListener {

    // region Private, MutableLiveData values
    // Observers will use the public LiveData instead of these private MutableLiveData values.
    private val _isConnectedToEcu: MutableLiveData<Boolean> = MutableLiveData(false)
    private val _isConnectedToTorque: MutableLiveData<Boolean> = MutableLiveData(false)
    private val _allPids: MutableLiveData<ArrayList<FullPid>> = MutableLiveData()
    private val _serviceRunning: MutableLiveData<Boolean> = MutableLiveData(false)
    private val _wakeLockHeld: MutableLiveData<Boolean> = MutableLiveData(false)
    private val _deleteCount: MutableLiveData<Int> = MutableLiveData()
    private var _serviceConnectionState: MutableLiveData<ServiceRunningState> = MutableLiveData()
    private var _torqueConnectionState: MutableLiveData<TorqueServiceConnectionState> = MutableLiveData()
    private var _wakelockState: MutableLiveData<WakelockState> = MutableLiveData()
    private var _monitoredPids: MutableLiveData<ArrayList<FullPid>> = MutableLiveData()
    private var _triggeredPid: MutableLiveData<FullPid> = MutableLiveData()
    // endregion

    // region Public, observable LiveData  values
    /**
     * Used as a quasi-callback when deleting rows from the db.  Value represents how many rows were
     * successfully deleted.
     */
    val deleteCount: LiveData<Int> = _deleteCount

    /**
     * Simple boolean representing whether or not the foreground service is currently connected to
     * the 3rd party ITorqueService
     */
    val isConnectedToTorque: LiveData<Boolean> = _isConnectedToTorque

    /**
     * Just a boolean indicating whether the foreground service is running.
     */
    val serviceRunning: LiveData<Boolean> = _serviceRunning

    /**
     * Should be true when ITorqueService is actively connected the vehicle's ECU.
     */
    val isConnectedToEcu: LiveData<Boolean> = _isConnectedToEcu

    /**
     * Should be true when the foreground service successfully acquires a wakelock and false when it
     * gets released.
     */
    val wakeLockHeld: LiveData<Boolean> = _wakeLockHeld

    /**
     * Contains a list of all PIDs obtained by our foreground service (and by extension the ITorqueService).
     * This list is updated every couple of seconds while the foreground service is running and is
     * connected to the 3rd party, ITorqueService.
     */
    val allPids: LiveData<ArrayList<FullPid>> = _allPids

    /**
     * Gets the current state of the foreground service.
     */
    val serviceConnectionState: LiveData<ServiceRunningState> = _serviceConnectionState

    /**
     * Gets the current state of the Torque service.
     */
    val torqueConnectionState: LiveData<TorqueServiceConnectionState> = _torqueConnectionState

    /**
     * Gets the current state of the wakelock within the service.
     */
    val wakelockState: LiveData<WakelockState> = _wakelockState

    /**
     * PIDs saved to the local database - these are the monitored PIDs
     */
     val monitoredPids: LiveData<ArrayList<FullPid>> = _monitoredPids

    /**
     * PIDs that were triggered by the service and broadcasts were sent.
     */
    val triggeredPids: LiveData<FullPid> = _triggeredPid
    // endregion

    /**
     * Events sourced from the PidMonitoringService and reflect the status of the foreground service
     * only.  Does not reflect whether the service has actually connected to the Torque service.
     */
    override fun onServiceRunningChanged(state: ServiceRunningState) {

        // Set the livedata of the actual connection state in case we have observers looking at it.
        _serviceConnectionState.value = state

        // Set the simpler livedata boolean for any observers.
        when (state) {
            ServiceRunningState.RUNNING -> {
                _serviceRunning.value = true
            }
            ServiceRunningState.STOPPED -> {
                _serviceRunning.value = false
            }
            ServiceRunningState.STARTING -> {
                _serviceRunning.value = false
            }
        }
    }

    /**
     * Events sourced from the PidMonitoringService and reflect the status of our connection to
     * Torque itself.  If "true" we can call methods of the Torque service.
     */
    override fun onTorqueServiceConnectionChanged(state: TorqueServiceConnectionState) {

        // Set the livedata of the actual connection state in case we have observers looking at it.
        _torqueConnectionState.value = state

        // Set the simpler, boolean live data for any observers
        when (state) {
            TorqueServiceConnectionState.CONNECTED -> {
                _isConnectedToTorque.value = true
            }
            TorqueServiceConnectionState.DISCONNECTED -> {
                _isConnectedToTorque.value = false
            }
        }
    }

    /**
     * Will be notified when the wakelock status changes in the service.
     */
    override fun onWakelockStatusChanged(state: WakelockState) {
        when (state) {
            WakelockState.ISHELD -> { _wakeLockHeld.value = true }
            WakelockState.NOTHELD -> { _wakeLockHeld.value = false }
        }
        _wakelockState.value = state
    }

    /**
     * By virtue of overriding the ServiceMessenger.OnFullPidsUpdated we receive the full list of
     * pids that our foreground service retrieved from the 3rd party ITorqueService.  We will
     * look for pids that match the one's the user has selected for monitoring.
     */
    override fun onFullPidsUpdated(updatedPids: ArrayList<FullPid>) {

        // Update the livedata for any observers
        _allPids.value = updatedPids

    }

    /**
     * By virtue of overriding the ServiceMessenger.onTriggeredPidsUpdated we receive the full list of
     * pids that our foreground service retrieved from the 3rd party ITorqueService.  We will
     * look for pids that match the one's the user has selected for monitoring.
     */
    override fun onTriggeredPidUpdated(triggeredPid: FullPid) {

        // Update livedata
        _triggeredPid.value = triggeredPid

    }

    /**
     * Saves the selected PID to the local database as well as updating it in working memory
     */
    suspend fun monitorPid(pid: FullPid, pos: Int) {

        // Find this pid in our in-memory array of pids and update its monitored flag.
        _allPids.value?.get(pos)?.isMonitored = true
        pid.broadcastAction = "fuck"

        // Save this pid to persistent storage for use next time we instantiate this viewmodel.
        viewModelScope.launch(IO) {
            savePidUseCase.execute(pid)
        }
    }

    /**
     * Removes a pid from the database.
     */
    suspend fun deletePid(pid: FullPid, pos: Int) {

        // Update our in-memory array immediately
        _allPids.value?.get(pos)?.isMonitored = false

        // Create a temp array that we can iterate over containing the pids we intend to remove.
        val pidsToRemove: ArrayList<FullPid> = ArrayList()

        // Populate the temp array with the values we intend to remove.
        _monitoredPids.value?.forEach {
            if (it.fullName == pid.fullName) {
                pidsToRemove.add(it)
            }
        }

        // Now we can iterate over our temp array instead of the real one to prevent concurrency
        // exceptions and remove the value it represents from the actual array.
        pidsToRemove.forEach {
            _monitoredPids.value?.remove(it)
        }

        // Finally, schedule this pid for deletion from persistent storage.
        viewModelScope.launch(IO){
            deletePidUseCase.execute(pid)
        }
    }

    /**
     * Starts the service foreground service
     */
    fun startService(debugMode: Boolean = false)  {
        // Subscribe to the service messenger so we can receive updates from the foreground service.
        serviceMessenger.addConnectionListener(this)
        // Start the foreground service
        val service = PidMonitoringService()
        val intent = Intent(app, service::class.java)
        intent.putExtra(PidMonitoringService.DEBUG_MODE, debugMode)
        app.startForegroundService(intent)
    }

    /**
     * Sends an intent to the PidMonitoringService instructing it to stop and release all resources.
     */
    fun stopService() {
        val intent = Intent(app, PidMonitoringService::class.java)
        app.stopService(intent)
    }

    override fun onCleared() {
        super.onCleared()

        // These values need to be cleared or we end up with weird behavior when the app is closed
        // (service still running) and resumed later.  It becomes impossible to un-monitor pids.
        // They DO get removed from the DB but the in-memory array is not updated correctly so the
        // next time the service sends us updated pids they get re-monitored.  It's almost like
        // there is two in-memory arrays for monitoredPids, that or it is a race condition.  I
        // don't know exactly what but this seems to totally resolve it!
        _monitoredPids.value?.clear()
        _allPids.value?.clear()
        _deleteCount.value = 0
    }

    init {
        Log.i(TAG, "Initialized:MainViewModel")

        // Grab the saved pids array from persistent storage (Room) and assign it to our in-memory
        // monitored pids array.
        viewModelScope.launch(Main) {
            getSavedPidsUseCase.execute().collect() {
                _monitoredPids.value = ArrayList(it)
            }
        }
    }
    companion object { private const val TAG = "FIMTOWN|MainViewModel" }

}














