package com.fimbleenterprises.torquepidcaster.data.model

import android.content.Context
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fimbleenterprises.torquepidcaster.R
import com.fimbleenterprises.torquepidcaster.util.Helpers
import org.joda.time.DateTime

/**
 * Represents an event where a saved PID's threshold for broadcast was met.  This
 * class serves to provide a running log of sent broadcasts in a recyclerview.
 */
@Entity(tableName = "trigger_log")
data class TriggeredPid(
    @PrimaryKey(autoGenerate = true)
    var index: Int? = null,
    var pidFullname: String? = null,
    var triggeredOnMillis: Long? = null,
    var conditions: String? = null,
    var broadcastAction: String? = null,
    var threshold: Double? = null,
    var value: Double? = null,
    var operator: FullPid.AlarmOperator? = null
) {

    fun getTriggeredOnAsDateTime(): DateTime {
        return DateTime(triggeredOnMillis)
    }
    
    fun getTriggeredOnAsPrettyDateTime(): String {
        return Helpers.DatesAndTimes.getPrettyDateAndTime(
            DateTime(triggeredOnMillis), 
            false,
            true,
            false
        )
    }

    /**
     * Returns both the action stipulated by the user as well as the preamble.
     */
    fun showFullBroadcast(context: Context): String {
        return context.getString(R.string.fully_qualified_broadcast, this.broadcastAction)
    }
    
}
