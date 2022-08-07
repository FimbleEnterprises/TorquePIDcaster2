package com.fimbleenterprises.torquepidcaster

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.fimbleenterprises.torquepidcaster.data.model.FullPid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TestUtil {

    init { Log.i(TAG, "Initialized:TestUtil") }

    companion object {
        private const val TAG = "FIMTOWN|TestUtil"

        /**
         * Creates an arbitrary amount of [FullPid] objects for testing.
         * @param howMany The amount of [FullPid] objects to create.
         */
        fun createPids(howMany: Int) : ArrayList<FullPid> {
            val pids = ArrayList<FullPid>()
            for (i in 1..howMany) {
                val pidInfo = "Acceleration Sensor $i (Total),Accel,g,1,-1,$i"
                val pidValues = .3 / howMany
                val pid = FullPid(pidInfo, pidValues)
                pid.broadcastAction = "POOP"
                pid.threshold = 1.1
                pid.operator = FullPid.AlarmOperator.GREATER_THAN
                pids.add(pid)
            }
            return pids
        }

    }

}