package com.fimbleenterprises.torquepidcaster.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.fimbleenterprises.torquepidcaster.data.model.FullPid

@Database(
    entities = [FullPid::class],
    version = 13,
    exportSchema = false
)

/** Since this abstract class overrides the RoomDatabase class, Room will look to the abstract functions
 * it contains to determine what db operations to perform.  It determines this by interpreting the
 * the magic interfaces you created (naming convention: <some_name>DAO etc.) that these functions
 * must implement.  Shit's confusing for sure but pretty cool and gets cooler the more you learn it.
 */
abstract class PidsDatabase : RoomDatabase() {
    // This function will get
    abstract fun getPidsDao() : PidsDao
}
