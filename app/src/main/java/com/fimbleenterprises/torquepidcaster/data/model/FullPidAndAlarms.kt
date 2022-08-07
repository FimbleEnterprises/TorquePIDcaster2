package com.fimbleenterprises.torquepidcaster.data.model

import androidx.room.Embedded
import androidx.room.Relation

/**
 * This class acts as a JOIN for the saved_pids ([FullPid]) table and the pid_alarms
 * ([Alarm]) table.  This allows a one-to-many relationship between [FullPid] and [Alarm].
 *
 * @see <a href="https://developer.android.com/training/data-storage/room/relationships">
 *          Room developer docs, yo: Define relationships between objects</a>
 */
/*data class FullPidAndAlarms(
    @Embedded
    val fullpid: FullPid,
    @Relation(
        parentColumn = "id",
        entityColumn = "fullpid"
    )
    var alarms: List<Alarm>?
)*/
