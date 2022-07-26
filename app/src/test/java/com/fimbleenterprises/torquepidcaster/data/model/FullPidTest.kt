package com.fimbleenterprises.torquepidcaster.data.model

import com.google.common.truth.Truth.*
import org.junit.Before
import org.junit.Test

class FullPidTest {



    private val pidInfo = Array(1) { "Acceleration Sensor(Total),Accel,g,1,-1,1" }
    private val pidValues = DoubleArray(1) { .3 }
    private lateinit var pid: FullPid

    @Before
    fun makePid() {
        pid = FullPid(pidInfo, pidValues)
    }

    @Test
    fun pidValueExceedsAlarmThresholdAndShouldBroadcast() {
        pid.operator = FullPid.AlarmOperator.GREATER_THAN
        pid.threshold = .2
        assertThat(pid.shouldBroadcast()).isTrue()
    }

    @Test
    fun canConstructFullPid() {
        assertThat(pid).isNotNull()
    }

    @Test
    fun canGetRawValuesForTorqueUpdate() {
        val strings : Array<String?> = pid.rawValueForTorqueServiceQueries
        assertThat(strings).isNotNull()
    }

    @Test
    fun canGetCorrectRawValue() {
        assertThat(pid.value).isEqualTo(0.3)
    }

    @Test
    fun canCovertMetersToMiles() {
        val kmsInMile = 1.609344
        assertThat(FullPid.tryConvertToImperial(kmsInMile, "km").toFloat()).isEqualTo(1f)
    }
}