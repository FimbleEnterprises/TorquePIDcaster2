package com.fimbleenterprises.torquepidcaster

import android.content.Intent
import android.os.Bundle
import android.os.Debug
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.CompoundButton
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.fimbleenterprises.torquepidcaster.databinding.ActivityPluginBinding
import com.fimbleenterprises.torquepidcaster.domain.service.PidMonitoringService
import com.fimbleenterprises.torquepidcaster.presentation.viewmodel.MainViewModel
import com.fimbleenterprises.torquepidcaster.presentation.viewmodel.MainViewModelFactory
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PluginActivity : AppCompatActivity() {

    @Inject
    lateinit var viewModelFactory: MainViewModelFactory
    var mFirebaseAnalytics: FirebaseAnalytics? = null
    private lateinit var binding: ActivityPluginBinding

    lateinit var viewmodel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewmodel = ViewModelProvider(this, viewModelFactory)[MainViewModel::class.java]
        viewmodel.startService(Debug.isDebuggerConnected())

        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        binding = ActivityPluginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_main, R.id.navigation_choose_pids
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        initMonitorService()
    }

    private fun initMonitorService() {
        viewmodel.serviceRunning.observeForever {
            binding.switchService.isChecked = it
        }

        binding.editTextNumberDecimal.setText(MyApp.AppPreferences.scanInterval.toString())

        binding.editTextNumberDecimal.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s?.length!! > 0) {
                    try {
                        val interval = s.toString().toFloat()
                        MyApp.AppPreferences.scanInterval = interval.toFloat()
                    } catch (exception:Exception) {
                        Log.e(TAG, "onTextChanged: ${exception.localizedMessage}"
                            , exception)

                    }
                }
            }
        })

        binding.switchService.setOnCheckedChangeListener { _, isChecked ->
            viewmodel.apply {
                if (isChecked) {
                    if (serviceRunning.value != true) {
                        // If we are debugging we start the Torque service in debug mode so we get
                        // fake data.
                        startService(Debug.isDebuggerConnected())
                    }
                } else {
                    if (serviceRunning.value == true) {
                        stopService()
                    }
                }
            }
        }
    }
    init { Log.i(TAG, "Initialized:PluginActivity") }
    companion object { private const val TAG = "FIMTOWN|PluginActivity" }
}





















