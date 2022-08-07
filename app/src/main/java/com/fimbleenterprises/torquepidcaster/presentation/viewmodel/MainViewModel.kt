@file:Suppress("unused")

package com.fimbleenterprises.torquepidcaster.presentation.viewmodel
import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.*
import com.fimbleenterprises.torquepidcaster.data.model.FullPid
import com.fimbleenterprises.torquepidcaster.data.model.TriggeredPid
import com.fimbleenterprises.torquepidcaster.domain.service.*
import com.fimbleenterprises.torquepidcaster.domain.usecases.*
import com.fimbleenterprises.torquepidcaster.util.Helpers
import com.google.android.gms.tasks.Tasks.await
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
    private val deletePidUseCase: DeletePidUseCase,
    private val insertLogEntryUseCase : InsertLogEntryUseCase,
    private val getLogEntriesUseCase : GetLogEntriesUseCase,
    private val deleteLogEntriesUseCase: DeleteLogEntriesUseCase
) : ViewModel(), ServiceMessenger.ServiceListener {

    // region Private, MutableLiveData values
    // Observers will use the public LiveData instead of these private MutableLiveData values.
    private val _showValuesOnMainFrag: MutableLiveData<Boolean> = MutableLiveData(false)
    private val _isConnectedToEcu: MutableLiveData<Boolean> = MutableLiveData(false)
    private val _isConnectedToTorque: MutableLiveData<Boolean> = MutableLiveData(false)
    private val _allPids: MutableLiveData<ArrayList<FullPid>> = MutableLiveData()
    private val _filteredPids: MutableLiveData<ArrayList<FullPid>> = MutableLiveData()
    /*When we are filtering we change the _allPids list for observers.  We use this arraylist to
    continue tracking the updates from the service so that we can instantly rebuild the _allPids
    array when filtering finished without having to wait for a new update.*/
    private var _allPidsLatestValues: ArrayList<FullPid> = ArrayList()
    private val _serviceRunning: MutableLiveData<Boolean> = MutableLiveData(false)
    private val _wakeLockHeld: MutableLiveData<Boolean> = MutableLiveData(false)
    private val _deleteCount: MutableLiveData<Int> = MutableLiveData()
    private var _serviceConnectionState: MutableLiveData<ServiceRunningState> = MutableLiveData()
    private var _torqueConnectionState: MutableLiveData<TorqueServiceConnectionState> = MutableLiveData()
    private var _wakelockState: MutableLiveData<WakelockState> = MutableLiveData()
    private var _monitoredPids: MutableLiveData<ArrayList<FullPid>> = MutableLiveData()
    private var _triggeredPids: MutableLiveData<ArrayList<TriggeredPid>> = MutableLiveData()
    private var _triggeredPidsLocalList = ArrayList<TriggeredPid>()
    private var _defaultBroadcastValue: MutableLiveData<String> = MutableLiveData()
    private var _forceRedraw: MutableLiveData<ArrayList<FullPid>> = MutableLiveData()
    /* Flag used to determine whether or not to apply the updated PIDs that are received from the
       service to update the _allPids array. */
    var isFiltering = false
    var filterString: String? = null
    var showSavedOnly = false
    // endregion

    // region Public, observable LiveData  values
    /**
     * Shows the broadcast action that is sent on every loop of the service stating the ECU
     * connection status.
     */
    val defaultBroadcastValue: LiveData<String> = _defaultBroadcastValue

    /**
     * Show values in the recyclerview as Torque retrieves them.
     */
    val showRealtimeValues: LiveData<Boolean> = _showValuesOnMainFrag

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
     * Contains a list of all PIDs obtained by our foreground service (and by extension the ITorqueService).
     * This list is updated every couple of seconds while the foreground service is running and is
     * connected to the 3rd party, ITorqueService.
     */
    val filteredPids: LiveData<ArrayList<FullPid>> = _filteredPids

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
     * PIDs saved to the local database
     */
     val monitoredPids: LiveData<ArrayList<FullPid>> = _monitoredPids

    /**
     * PIDs that were triggered by the service and broadcasts were sent.
     */
    val triggeredPids: LiveData<ArrayList<TriggeredPid>> = _triggeredPids

    /**
     * When true all observers should resubmit the list to their adapter or call notifyDatasetChanged
     */
    val forceRedraw: LiveData<ArrayList<FullPid>> = _forceRedraw
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
     * Since this viewmodel implements the ServiceMessenger.ServiceListener we must override this
     * method and by doing so we can monitor the status of our connection to Torque itself.  If
     * "true" our service can call methods from the Torque service.
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
     * Since this viewmodel implements the ServiceMessenger.ServiceListener we must override this
     * method we receive the full list of pids that our foreground service retrieved from the 3rd
     * party ITorqueService.  We will look for pids that match the one's the user has selected for
     * monitoring.
     */
    override fun onFullPidsUpdated(updatedPids: ArrayList<FullPid>) {

        // Keep our private list always current
        _allPidsLatestValues = updatedPids

        if (!isFiltering) {
            // Update the livedata for any observers
            _allPids.value = updatedPids
        } else if (showSavedOnly) {
            showSavedOnly(true)
        } else {
            filterPidsByName(filterString)
        }

    }

    /**
     * Filters all pids and changes the allPids MutableLiveData so observers will see only the
     * filtered pids.  IMPORTANT: Pass null or an empty string to finish filtering and go back
     * to real-time pids.
     */
    fun filterPidsByName(query: String?) {

        isFiltering = !query.isNullOrEmpty()
        filterString = query

        val filteredArray = ArrayList<FullPid>()
        allPids.value?.forEach {
            if (isFiltering && it.id.lowercase().contains(query!!.lowercase())) {
                filteredArray.add(it)
            }
        }
        _filteredPids.value = filteredArray

        // We need a way to trigger the observer otherwise the list will not be updated since
        //  it technically hasn't changed.
        _allPids.value = _allPidsLatestValues
    }

    /**
     * Filters all pids and changes the allPids MutableLiveData so observers will see only the
     * filtered pids.  IMPORTANT: Pass null or an empty string to finish filtering and go back
     * to real-time pids.
     */
    fun showSavedOnly(boolean: Boolean) {

        isFiltering = boolean
        showSavedOnly = boolean

        /**
         * Build a new array of only our saved PIDs by looping over all known pids and adding
         * only ones found with their [FullPid.isMonitored] flag set to true. */
        val filteredArray = ArrayList<FullPid>()
        allPids.value?.forEach {
            if (showSavedOnly && it.isMonitored) {
                filteredArray.add(it)
            }
        }
        /**
         * Update the filtered pids livedata with our newly built array.
         */
        _filteredPids.value = filteredArray

        /** We need a way to alert observers watching the the [_allPids] livedata that it has
         * changed - just adding/removing items will not do it - the value itself needs to change
         * so we do that here. */
        _allPids.value = _allPidsLatestValues
    }

    /**
     * Since this viewmodel implements the ServiceMessenger.ServiceListener we must override this
     * method and by doing so can keep a real-time tally of whether or not Torque is connected to
     * the vehicle's ECU.
     */
    override fun onEcuConnectionListener(state: ConnectedToECUState) {
        when (state) {
            ConnectedToECUState.NOTCONNECTED -> _isConnectedToEcu.value = false
            ConnectedToECUState.CONNECTED -> _isConnectedToEcu.value = true
        }
    }

    /**
     * Since this viewmodel implements the ServiceMessenger.ServiceListener we must override this
     * method and by doing so can keep a real-time tally of the default broadcast being sent.
     */
    override fun onDefaultBroadcastSent(action: String) {
        _defaultBroadcastValue.value = action
    }

    /**
     * Saves the selected PID to the local database as well as updating it in working memory
     */
    suspend fun monitorPid(pid: FullPid, pos: Int) {

        // Find this pid in our in-memory array of pids and update its monitored flag.
        _allPids.value?.get(pos)?.isMonitored = true

        // Save this pid to persistent storage for use next time we instantiate this viewmodel.
        viewModelScope.launch(IO) {
            pid.isMonitored = true
            savePidUseCase.execute(pid)
        }
    }

    /**
     * Removes a pid from the database.
     */
    suspend fun stopMonitoringPid(pid: FullPid, pos: Int) {

        // Update our in-memory array immediately
        _allPids.value?.get(pos)?.isMonitored = false

        // Create a temp array that we can iterate over containing the pids we intend to remove.
        val pidsToRemove: ArrayList<FullPid> = ArrayList()

        // Populate the temp array with the values we intend to remove.
        _monitoredPids.value?.forEach {
            if (it.id == pid.id) {
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
     * Removes all saved pids from the database.
     */
    suspend fun stopMonitoringAllPids(): Int {
        val count = deletePidUseCase.executeMany()
        withContext(Main) {
            _allPidsLatestValues.forEach {
               it.isMonitored = false
            }
            _forceRedraw.value = _allPidsLatestValues
        }
        return count
    }

    /**
     * Removes all [TriggeredPid]s from the database.
     */
    fun clearLog() {
        viewModelScope.launch(IO) {
            deleteLogEntriesUseCase.executeMany()
        }
    }

    /**
     * Since this viewmodel implements the ServiceMessenger.ServiceListener we must override this
     * method and by doing so we get notified whenever a PID has triggered and sent a broadcast.
     */
    override fun onPidTriggered(triggeredPid: TriggeredPid) {/* Nothing to do here in viewmodel */}

    fun showValuesInListView(value: Boolean) {
        _showValuesOnMainFrag.value = value
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

    /**
     * Private method to save space.
     */
    private fun context(): Context {
        return app.applicationContext
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

    /**
     * Checks if the app is currently under the thumb of the OS battery nazis.
     */
    fun isIgnoringBattOptimizations(): Boolean {
        return Helpers.Application.isIgnoringBatteryOptimizations(context())
    }

    init {
        Log.i(TAG, "Initialized:MainViewModel")

        // Grab the saved pids array from persistent storage (Room) and assign it to our in-memory
        // monitored pids array and continue to monitor it for changes, updating the livedata when
        // it does.
        viewModelScope.launch(Main) {
            getSavedPidsUseCase.execute().collect() {
                _monitoredPids.value = ArrayList(it)
            }
        }

        // Get the log entries from the DB and continue to monitor it, updating the livedata as
        // records are added/removed.
        viewModelScope.launch(Main) {
            getLogEntriesUseCase.executeMany().collect {
                _triggeredPids.value = ArrayList(it)
            }
        }

        // Initialize the arraylist for triggered pids
        _triggeredPidsLocalList = ArrayList<TriggeredPid>()
    }
    companion object { private const val TAG = "FIMTOWN|MainViewModel" }

}














