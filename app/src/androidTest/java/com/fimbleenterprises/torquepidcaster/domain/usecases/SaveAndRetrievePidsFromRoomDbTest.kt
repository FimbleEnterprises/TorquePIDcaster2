package com.fimbleenterprises.torquepidcaster.domain.usecases

import android.content.Context
import android.util.Log
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fimbleenterprises.torquepidcaster.TestUtil
import com.fimbleenterprises.torquepidcaster.data.db.PidsDao
import com.fimbleenterprises.torquepidcaster.data.db.PidsDatabase
import com.fimbleenterprises.torquepidcaster.data.model.FullPid
import com.fimbleenterprises.torquepidcaster.data.repository.MainRepositoryImpl
import com.fimbleenterprises.torquepidcaster.data.repository.datasource.LocalDatasource
import com.fimbleenterprises.torquepidcaster.data.repository.datasourceImpl.LocalDatasourceImpl
import com.fimbleenterprises.torquepidcaster.domain.repository.MainRepository
import org.junit.After
import org.junit.Before
import com.google.common.truth.Truth.*
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class SaveAndRetrievePidsFromRoomDbTest {

    private lateinit var pidsDao: PidsDao
    private lateinit var db: PidsDatabase
    private lateinit var mainRepository: MainRepository
    private lateinit var localDatasource: LocalDatasource
    private lateinit var getSavedPidsUseCase: GetSavedPidsUseCase
    private lateinit var savePidUseCase: SavePidUseCase

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context,
            PidsDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
        pidsDao = db.getPidsDao()
        localDatasource = LocalDatasourceImpl(pidsDao)
        mainRepository = MainRepositoryImpl(localDatasource)
        getSavedPidsUseCase = GetSavedPidsUseCase(mainRepository)
        savePidUseCase = SavePidUseCase(mainRepository)
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
            val result = savePidUseCase.execute(fullPid)
            assertThat(result).isEqualTo(1)
        }
    }

    @Test
    fun testCanSaveMultiplePidsAndRetrieveThem()  {

        val originalPids = TestUtil.createPids(5)

        runBlocking {
            for (i in 0 until originalPids.size) {
                savePidUseCase.execute(originalPids[i])
            }

            getSavedPidsUseCase.execute().take(1).collect { savedPids ->
                assertThat(savedPids.size).isEqualTo(5)
                for (i in 0 until savedPids.size - 1) {
                    assertThat(savedPids[i].id).isEqualTo(originalPids[i].id)
                }
            }
        }
    }

    @Test
    fun testPidAlarmIsSavedAndRetrievedWithCorrectValues()  {

        val originalPids = TestUtil.createPids(1)

        runBlocking {
            for (i in 0 until originalPids.size) {
                savePidUseCase.execute(originalPids[i])
            }

            getSavedPidsUseCase.execute().take(1).collect { savedPids ->
                for (i in 0 until savedPids.size - 1) {
                    assertThat(savedPids[i].broadcastAction).isEqualTo("POOP")
                    assertThat(savedPids[i].threshold).isEqualTo(1.1)
                    assertThat(savedPids[i].operator).isEqualTo(FullPid.AlarmOperator.GREATER_THAN)
                }
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun testSaveAndRetrieveFullPid() {
        runBlocking {
            val fullPid = TestUtil.createPids(1)[0]
            savePidUseCase.execute(fullPid)
            val list = getSavedPidsUseCase.execute(fullPid.id).take(1).toList()
            val retrievedPid = list[0]
            assertThat(retrievedPid.id).isEqualTo(fullPid.id)
        }

    }
    
    init { Log.i(TAG, "Initialized:SaveAndRetrievePidsFromRoomDbTest") }
    companion object { private const val TAG = "FIMTOWN|SaveAndRetrievePidsFromRoomDbTest" }
}