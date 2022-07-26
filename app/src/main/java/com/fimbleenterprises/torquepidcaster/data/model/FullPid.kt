package com.fimbleenterprises.torquepidcaster.data.model

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.fimbleenterprises.torquepidcaster.util.Helpers
import org.prowl.torque.remote.ITorqueService


@Suppress("UNUSED", "MemberVisibilityCanBePrivate")

@Entity(
    tableName = "savedpids"
)
/**
 * Used as a container to track a single pid as returned by Torque.  This class holds the
 * information about the pid as well as the value and logic to determine whether or not
 * that value has exceeded an arbitrary value and should be broadcast to 3rd party apps.
 */
data class FullPid (

    @PrimaryKey(autoGenerate = false)
    /**
     * The long name as returned by Torque.  Also the primary key in the Room db.
     */
    var fullName: String = "ERROR_CREATING_PID",
    /**
     * The short name of the pid as reported by Torque
     */
    var shortName: String? = null,
    /**
     * Not saved to database.
     */
    @Ignore private var value : Double = 0.0,
    /**
     * The minimum value that can be returned by Torque.
     */
    var min: Double = 0.0,
    /**
     * The maximum value that can be returned by Torque.
     */
    var max: Double = 0.0,
    /**
     * The unit of measurement as returned by Torque (e.g. km/h)
     */
    var unit: String? = null,
    var scale: Double = 0.0,
    /**
     * The value that is returned by: ``ITorqueService.getPIDInformation({ "rawpidvalue1" })``
     *
     * `` ``
     *
     * Which will return a string array containing values for however many pids were requested
     * (usually one in our cases). Each element will have a string value formatted as such:
     * ```
     * ```
     *
     * *`"Speed (GPS),GPS Spd,km/h,160,0,1"`*
     *
     * ```
     * ```
     * Element 0: Full name e.g. "Speed (GPS)"
     *
     * Element 1: Short name e.g. "GPS Spd"
     *
     * Element 2: Unit type e.g. "km/h"
     *
     * Element 3: Max value e.g. "160"
     *
     * Element 4: Min value e.g. "0"
     *
     * Element 5: Scale (usually/always) "1"
     */
    var commaDelimitedPidInfo: String? = null,
    /**
     * This is most basic of values that is returned by ITorqueService when calling any of its
     * retrieve pids methods. To get more useful info about this pid we need to use this value and
     * make an additional call to:
     *
     * **`ITorqueService.getPIDInformation([rawPidId])`** method.
     *
     * That will return something like this:
     *
     * **`"Speed (GPS),GPS Spd,km/h,160,0,1"`**
     *
     * ``` ```
     *
     * We also use this rawPidId when to get the PIDs values using:
     *
     * **`ITorqueService.getPIDValuesAsDouble(rawPidId)`**
     *
     * ``` ```
     *
     * Which returns a DoubleArray with each element representing the value of each PID requested.
     */
    var rawPidId: String? = null,
    /**
     * Used by us to determine if a value has exceeded an arbitrary value
     * and should be broadcast.
     */
    var threshold: Double = 0.0,
    /**
     * Logical operators to apply when evaluating the threshold and value properties to 4
     * determine whether or not to broadcast.
     */
    var operator: AlarmOperator? = null,
    /**
     * This is the action property for the intent that will be broadcast and be evaluated by
     * 3rd party apps.
     */
    var broadcastAction: String? = null,
    /**
     * Not saved to database.
     */
    @Ignore private var lastUpdatedInMS: Long = 0,
    /**
     * Not saved to database.
     */
    @Ignore var isMonitored: Boolean = false
) {
    /**
     * If you query ITorqueService to get info or values it will return those things
     * as arrays.  This create function saves a line of code for the caller.
     *
     * ``
     *
     * If multiple pid info/values are passed it will process only the first element.
     * @param commaDelimitedPidInfo A string array containing comma-delimited strings
     * as returned by ITorqueService.getPIDInformation()
     * @param commaDelimitedPidValues A DoubleArray as returned by ITorqueService.
     * getPIDValuesAsDouble()
     */
    constructor(commaDelimitedPidInfo: Array<String>, commaDelimitedPidValues: DoubleArray) : this() {
        val list = createMany(commaDelimitedPidInfo, commaDelimitedPidValues)
        if (list.size > 0) {
            val fullPid = list[0]
            this.fullName = fullPid.fullName
            this.shortName = fullPid.shortName
            this.unit = fullPid.unit
            this.max = fullPid.max
            this.min = fullPid.min
            this.scale = fullPid.scale
            this.commaDelimitedPidInfo = fullPid.commaDelimitedPidInfo
            this.value = fullPid.value
        }
    }

    /**
     * Constructs a new FullPid.
     * @param info comma-delimited string as returned by ITorqueService
     * @param value The pid's value as a double.
     */
    constructor(info: String, value: Double) : this() {
        val fullPid = create(info, value)
        this.fullName = fullPid.fullName
        this.shortName = fullPid.shortName
        this.unit = fullPid.unit
        this.max = fullPid.max
        this.min = fullPid.min
        this.scale = fullPid.scale
        this.commaDelimitedPidInfo = fullPid.commaDelimitedPidInfo
        this.setValue(value)
    }

    /**
     * Used to construct logic when evaluating the value and threshold properties.
     */
    enum class AlarmOperator {
        LESS_THAN, GREATER_THAN, EQUALS, NOT_EQUALS, SEND_ALWAYS
    }

    /**
     * Gets the last updated value timestamp in millis.
     */
    fun getLastUpdatedInMS(): Long {
        return System.currentTimeMillis() - lastUpdatedInMS
    }

    /**
     * Converts the timestamp from millis to seconds
     */
    fun getLastUpdatedInSecs(): Double {
        val diffInMs = System.currentTimeMillis() - lastUpdatedInMS
        return Helpers.Numbers.formatAsXDecimalPointNumber((diffInMs / 1000).toDouble(), 4)
    }

    /**
     * Takes the commaDelimitedPidInfo property and adds it a StringArray as that is the argument
     * that the ITorqueService.getPIDValuesAsDouble() method accepts.
     */
    val rawValueForTorqueServiceQueries: Array<String?>
        get() = arrayOf(commaDelimitedPidInfo)

    /**
     * Returns this PID's current raw value as well as updating its raw value.  As I understand it,
     * this updates the PID and sends that updated value to the app NOT the ECU.
     * @param torqueService
     * @return
     */
    /*fun updateValueInTorque(torqueService: ITorqueService) {
        try {
            val vals = torqueService.sendPIDDataV2()
            value = vals[0]
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }*/

    /**
     * Updates this pid's value and timestamps it.
     */
    fun setValue(value: Double) {
        this.value = value
        this.lastUpdatedInMS = System.currentTimeMillis()
    }

    /**
     * Returns this pid's private value property.
     */
    fun getValue(): Double {
        return this.value
    }

    /**
     * Evaluates the supplied value against the previously specified alarm threshold
     * using the operator stipulated at the alarm's creation.
     * @return true if the threshold is breached and false if not.
     */
    fun shouldBroadcast(): Boolean {
        return when (operator) {
            AlarmOperator.EQUALS -> (value == threshold)
            AlarmOperator.NOT_EQUALS -> (value != threshold)
            AlarmOperator.GREATER_THAN -> (value > threshold)
            AlarmOperator.LESS_THAN -> (value < threshold)
            AlarmOperator.SEND_ALWAYS -> true
            else -> false
        }
    }

    companion object {
        private const val TAG = "FIMTOWN|MyPID"
        private const val PREF_KEY_ALARM_ENTRY = "PREF_KEY_ALARM_ENTRY"
        private const val RESET_VALUE_IF_NO_UPDATE_IN_SECONDS = 2

        /**
         * Creates a FullPid using a comma-delimited info string and a double value.
         */
        fun create(info: String, value: Double): FullPid {
            val fullPid = FullPid()
            try {
                val pidInfo = info.split(",").toTypedArray()
                fullPid.fullName = pidInfo[0]
                fullPid.shortName = pidInfo[1]
                fullPid.unit = pidInfo[2]
                fullPid.max = pidInfo[3].toDouble()
                fullPid.min = pidInfo[4].toDouble()
                fullPid.scale = pidInfo[5].toDouble()
                fullPid.commaDelimitedPidInfo = pidInfo.joinToString(",")
                fullPid.setValue(value)
            } catch (exception:Exception) { return FullPid() }
            return fullPid
        }

        /**
         * Creates many FullPids and accepts the values as returned by the relevant
         * ITorqueService calls.
         */
        fun createMany(infos: Array<String>, values: DoubleArray) : ArrayList<FullPid> {
            val fullPids = ArrayList<FullPid>()
            for (i in infos.indices) {
                fullPids.add(FullPid(infos[i], values[i]))
            }
            return fullPids
        }
        
        /**
         * Updates the PID's value by querying the ECU for a current value.  By default values are metric.
         * <br></br><br></br>
         * If the user's preferences suggest that the PID values ought to be converted to imperial this
         * function tries to convert the current PID's value from metric.  It should be successful if the
         * PID's UNIT property is determined as either meters, kilometers or celsius.  If the UNIT value
         * is of another type (K/PA etc.) or if a known UNIT type's conversion fails then the fallback
         * value will always be updated as metric.
         * @param pidRawValue The PID's .getRawValue() value
         * @param unitType The PID's .unitType() value
         * @return The supplied value, as a Float converted to imperial.
         */
        fun tryConvertToImperial(pidRawValue: Double, unitType: String): Double {
            when {
                pidRawValue == 0.0 -> {
                    return 0.0
                }
                unitType == "km" -> {
                    val meters = (pidRawValue * 1000.0)
                    val feet = meters * 3.280839895
                    return feet / 5280.0
                }
                unitType == "m" -> {
                    return pidRawValue * 3.280839895
                }
                unitType == "°C" -> {
                    return pidRawValue.toFloat() * 1.8 + 32.0
                }
                else -> {
                    return pidRawValue
                }
            }
        }
    }

}