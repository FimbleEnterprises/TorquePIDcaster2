package com.fimbleenterprises.torquepidcaster.domain.service

import android.annotation.SuppressLint
import android.util.Log
import com.fimbleenterprises.torquepidcaster.data.model.FullPid
import com.fimbleenterprises.torquepidcaster.data.model.TriggeredPid

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
        fun onPidTriggered(triggeredPid: TriggeredPid)
        fun onEcuConnectionListener(state: ConnectedToECUState)
        fun onDefaultBroadcastSent(action: String)
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
    private fun publishDefaultBroadcastActionUpdated(action: String) {
        for (listener in connectionListenerListeners) {
            listener.onDefaultBroadcastSent(action)
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
    private fun publishPidTriggered(triggeredPid: TriggeredPid) {
        for(listener in connectionListenerListeners) {
            listener.onPidTriggered(triggeredPid)
        }
    }

    /**
     * Sends the state of whether or not Torque has connected to the ECU to all listeners.
     */
    private fun publishEcuState(state: ConnectedToECUState) {
        for(listener in connectionListenerListeners) {
            listener.onEcuConnectionListener(state)
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

    /**
     * Signal that our service and the Torque service have been connected
     */
    fun torqueConnected() {
        publishTorqueConnectionState(TorqueServiceConnectionState.CONNECTED)
    }

    /**
     * Signal that our service and the Torque service have been disconnected
     */
    fun torqueDisconnected() {
        publishTorqueConnectionState(TorqueServiceConnectionState.DISCONNECTED)
    }

    /**
     * Signal that our service has acquired a wakelock and the user's battery is about to get fucked!
     */
    fun wakelockAcquired() {
        publishWakelockState(WakelockState.ISHELD)
    }

    /**
     * Signal that our service does not have a wakelock active.
     */
    fun wakelockReleased() {
        publishWakelockState(WakelockState.NOTHELD)
    }

    /**
     * Signal that Torque has successfully connected to the vehicle's ECU.
     */
    fun ecuConnected() {
        publishEcuState(ConnectedToECUState.CONNECTED)
    }

    /**
     * Signal that Torque is not connected to the vehicle's ECU.
     */
    fun ecuDisconnected() {
        publishEcuState(ConnectedToECUState.NOTCONNECTED)
    }

    /**
     * Publish all of the PIDs that Torque is aware of.
     */
    fun publishFullPids(pids: ArrayList<FullPid>) {
        publishFullPidsUpdated(pids)
    }

    /**
     * Publish a PID that has just triggered and a broadcast was sent.
     */
    fun publishTriggeredPid(pid: TriggeredPid) {
        publishPidTriggered(pid)
    }

    fun publishDefaultBroadcastAction(action: String) {
        publishDefaultBroadcastActionUpdated(action)
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

@SuppressLint("LongLogTag")
sealed class ConnectedToECUState {
    object CONNECTED : ConnectedToECUState()
    object NOTCONNECTED : ConnectedToECUState()
    init { Log.i(TAG, "Initialized:ConnectedToECUState") }
    companion object { private const val TAG = "FIMTOWN|ConnectedToECUState" }
}