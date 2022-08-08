package com.fimbleenterprises.torquepidcaster.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.fimbleenterprises.torquepidcaster.data.model.FullPid
import com.fimbleenterprises.torquepidcaster.data.model.TriggeredPid

@Database(
    entities = [FullPid::class, TriggeredPid::class],
    version = 2,
    exportSchema = false
)

/** Since this abstract class overrides the RoomDatabase class, Room will look to the abstract
 *  functions that stipulate Dao interfaces.  It is within these dao interfaces that the logic
 *  is written to perform crud operations.
 */
abstract class PidsDatabase : RoomDatabase() {
    abstract fun getPidsDao() : PidsDao
    abstract fun getLogsDao() : LogsDao
}
