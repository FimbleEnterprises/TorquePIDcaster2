package com.fimbleenterprises.torquepidcaster.domain.usecases

import android.content.Context
import android.util.Log
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.*
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fimbleenterprises.torquepidcaster.TestUtil
import com.fimbleenterprises.torquepidcaster.data.db.PidsDao
import com.fimbleenterprises.torquepidcaster.data.db.PidsDatabase
import com.fimbleenterprises.torquepidcaster.data.model.FullPid
import org.junit.After
import org.junit.Before
import com.google.common.truth.Truth.*
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class SaveAndRetrievePidsFromRoomDbTest {

    private lateinit var pidsDao: PidsDao
    private lateinit var db: PidsDatabase
    private val testDispatcher = TestCoroutineDispatcher()

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, PidsDatabase::class.java).allowMainThreadQueries().build()
        pidsDao = db.getPidsDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun testSavePidReturnsCorrectRowNumber() {
        runBlocking {
            val fullPid = TestUtil.createPids(1)[0]
            val result = pidsDao.savePid(fullPid)
            assertThat(result).isEqualTo(1)
        }
    }

    @Test
    fun testCanSaveMultiplePidsAndRetrieveThem()  {

        val originalPids = TestUtil.createPids(5)

        runBlocking {
            for (i in 0 until originalPids.size) {
                pidsDao.savePid(originalPids[i])
            }

            pidsDao.getSavedPids().take(1).collect { savedPids ->
                assertThat(savedPids.size).isEqualTo(5)
                for (i in 0 until savedPids.size - 1) {
                    assertThat(savedPids[i].fullName).isEqualTo(originalPids[i].fullName)
                }
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun testSaveAndRetrieveFullPid() {
        runBlocking {
            val fullPid = TestUtil.createPids(1)[0]
            var retrievedPid = FullPid()
            pidsDao.savePid(fullPid)
            val list = pidsDao.getSavedPid(fullPid.fullName).take(1).toList()
            retrievedPid = list[0]
            assertThat(retrievedPid.fullName).isEqualTo(fullPid.fullName)
        }

    }
    
    init { Log.i(TAG, "Initialized:SaveAndRetrievePidsFromRoomDbTest") }
    companion object { private const val TAG = "FIMTOWN|SaveAndRetrievePidsFromRoomDbTest" }
}