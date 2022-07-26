package com.fimbleenterprises.torquepidcaster.di

import android.app.Application
import android.util.Log
import com.fimbleenterprises.torquepidcaster.domain.service.ServiceMessenger
import com.fimbleenterprises.torquepidcaster.domain.usecases.DeletePidUseCase
import com.fimbleenterprises.torquepidcaster.domain.usecases.GetSavedPidsUseCase
import com.fimbleenterprises.torquepidcaster.domain.usecases.SavePidUseCase
import com.fimbleenterprises.torquepidcaster.presentation.viewmodel.MainViewModelFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Like most every @Module annotated classes this allows us to instantiate an instance of this class
 * by simply declaring it as a lateinit var annotated with @Inject wherever we need this class.
 *
 * e.g.
 * ```
 *  @Inject
 *  ```
    lateinit var factory: NewsViewModelFactory

 *  BAM!  You now have this class.  Note that it does not matter what you name the function (annotated
 *  with @Provides) that does the injection; it is the return type that tells dagger which function
 *  to use.
 */

@Module
@InstallIn(SingletonComponent::class)
class FactoryModule {

    @Singleton
    @Provides
    fun providesMainViewModelFactory(
        application: Application,
        serviceManager: ServiceMessenger,
        savePidUseCase: SavePidUseCase,
        getSavedPidsUseCase: GetSavedPidsUseCase,
        deletePidUseCase: DeletePidUseCase
    ): MainViewModelFactory {
        return MainViewModelFactory(
            application,
            serviceManager,
            savePidUseCase,
            getSavedPidsUseCase,
            deletePidUseCase
        )
    }

    init { Log.i(TAG, "Initialized:FactoryModule") }
    companion object { private const val TAG = "FIMTOWN|FactoryModule" }
}








