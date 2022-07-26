package com.fimbleenterprises.torquepidcaster.domain.service

import android.annotation.SuppressLint
import android.util.Log
import com.fimbleenterprises.torquepidcaster.data.model.FullPid

/**
 * Used as an intermediary object in order to communicate events in real-time between our
 * Service and our ViewModel.
 * Adapted from this brilliant solution: https://github.com/uberchilly/BoundServiceMVVM
 */
@SuppressLint("LongLogTag")
class ServiceMessenger {

    interface ServiceListener {
        fun onServiceRunningChanged(state: ServiceRunningState)
        fun onTorqueServiceConnectionChanged(state: TorqueServiceConnectionState)
        fun onWakelockStatusChanged(state: WakelockState)
        fun onFullPidsUpdated(updatedPids: ArrayList<FullPid>)
        fun onTriggeredPidUpdated(triggeredPid: FullPid)
    }

    // A list of subscribers to this manager
    private val connectionListenerListeners: HashSet<ServiceListener> = HashSet()

    /**
     * Subscribes a new listener for connection updates.
     */
    fun addConnectionListener(listener: ServiceListener) {
        connectionListenerListeners.add(listener)
    }

    /**
     * Removes a subscribed listener from the list.
     */
    fun removeConnectionListener(listener: ServiceListener) {
        if (connectionListenerListeners.contains(listener)) {
            connectionListenerListeners.remove(listener)
        }
    }

    /**
     * Sends the actual update containing the current state of the service to all subscribers.
     */
    private fun publishConnectionState(state: ServiceRunningState) {
        for (listener in connectionListenerListeners) {
            listener.onServiceRunningChanged(state)
        }
    }

    /**
     * Sends the actual update containing the current state of the Torque service status to all subscribers.
     */
    private fun publishTorqueConnectionState(state: TorqueServiceConnectionState) {
        for (listener in connectionListenerListeners) {
            listener.onTorqueServiceConnectionChanged(state)
        }
    }

    /**
     * Reports when the wakelock status changes between isHeld or not.
     */
    private fun publishWakelockState(state: WakelockState) {
        for(listener in connectionListenerListeners) {
            listener.onWakelockStatusChanged(state)
        }
    }

    /**
     * Sends the full list of pids with values to all listeners.
     */
    private fun publishFullPidsUpdated(fullPids: ArrayList<FullPid>) {
        for(listener in connectionListenerListeners) {
            listener.onFullPidsUpdated(fullPids)
        }
    }

    /**
     * Sends the full list of pids with values to all listeners.
     */
    private fun publishTriggeredPidUpdated(triggeredPid: FullPid) {
        for(listener in connectionListenerListeners) {
            listener.onTriggeredPidUpdated(triggeredPid)
        }
    }

    /**
     * This will set the ServiceRunningState to STARTING and then call the internal publish method
     * to send it off to all subscribers.
     */
    fun serviceStarting() {
        publishConnectionState(ServiceRunningState.STARTING)
    }

    /**
     * This will set the ServiceRunningState to RUNNING and then call the internal publish method
     * to send it off to all subscribers.
     */
    fun serviceStarted() {
        publishConnectionState(ServiceRunningState.RUNNING)
    }

    /**
     * This will set the ServiceRunningState to STOPPED and then call the internal publish method
     * to send it off to all subscribers.
     */
    fun serviceStopped() {
        publishConnectionState(ServiceRunningState.STOPPED)
    }

    fun torqueConnected() {
        publishTorqueConnectionState(TorqueServiceConnectionState.CONNECTED)
    }

    fun torqueDisconnected() {
        publishTorqueConnectionState(TorqueServiceConnectionState.DISCONNECTED)
    }

    fun wakelockAcquired() {
        publishWakelockState(WakelockState.ISHELD)
    }

    fun wakelockReleased() {
        publishWakelockState(WakelockState.NOTHELD)
    }

    fun publishFullPids(pids: ArrayList<FullPid>) {
        publishFullPidsUpdated(pids)
    }

    fun publishTriggeredPid(pid: FullPid) {
        publishTriggeredPidUpdated(pid)
    }

    init { Log.i(TAG, "Initialized:ServiceManager") }
    companion object { private const val TAG = "FIMTOWN|ServiceManager" }
}

@SuppressLint("LongLogTag")
sealed class ServiceRunningState {
    object STARTING : ServiceRunningState()
    object RUNNING : ServiceRunningState()
    object STOPPED : ServiceRunningState()
    init { Log.i(TAG, "Initialized:ServiceRunningState") }
    companion object { private const val TAG = "FIMTOWN|ServiceRunningState" }
}

@SuppressLint("LongLogTag")
sealed class TorqueServiceConnectionState {
    object CONNECTED : TorqueServiceConnectionState()
    object DISCONNECTED : TorqueServiceConnectionState()
    init { Log.i(TAG, "Initialized:TorqueServiceRunningState") }
    companion object { private const val TAG = "FIMTOWN|TorqueServiceRunningState" }
}

@SuppressLint("LongLogTag")
sealed class WakelockState {
    object ISHELD : WakelockState()
    object NOTHELD : WakelockState()
    init { Log.i(TAG, "Initialized:WakelockState") }
    companion object { private const val TAG = "FIMTOWN|WakelockState" }
}