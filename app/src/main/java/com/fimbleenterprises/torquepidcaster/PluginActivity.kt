package com.fimbleenterprises.torquepidcaster

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Debug
import android.os.PowerManager
import android.provider.Settings
import android.text.Html
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.text.HtmlCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.fimbleenterprises.torquepidcaster.databinding.ActivityPluginBinding
import com.fimbleenterprises.torquepidcaster.presentation.viewmodel.MainViewModel
import com.fimbleenterprises.torquepidcaster.presentation.viewmodel.MainViewModelFactory
import com.fimbleenterprises.torquepidcaster.util.Helpers
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
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
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)

        binding = ActivityPluginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView
        navView.itemIconTintList = null

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
        /*supportActionBar?.setBackgroundDrawable(ColorDrawable(Color.parseColor("#000000")))
        supportActionBar?.title = Html.fromHtml(
            "<font color='#FFFFFF'>${getString(R.string.app_name)}</font>"
        )*/

        initMonitorService()

        if (!viewmodel.isIgnoringBattOptimizations()) {
            showSnackBar()
        }
    }

    private fun showSnackBar() {

        val snackbar = Snackbar.make(
            binding.root,
            getString(R.string.batt_opt_snack_message),
            Snackbar.LENGTH_INDEFINITE
        )

        snackbar.setAction(getString(R.string.fix)) {
            val dialogBuilder = AlertDialog.Builder(this)
            dialogBuilder
                .setMessage(getString(R.string.batt_opt_message))
                .setCancelable(true)
                .setPositiveButton(getString(R.string.take_me_there)) { _, _ ->
                    Helpers.Application.sendToAppSettings(this)
                }
            val alert = dialogBuilder.create()
            alert.show()
        }
        snackbar.show()
    }

    private fun initMonitorService() {
        viewmodel.serviceRunning.observeForever {
            binding.switchService.isChecked = it
        }

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

    private fun openPowerSettings(context: Context) {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
            val intent = Intent()
            intent.action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
            context.startActivity(intent)
        }
    }

    init {
        Log.i(TAG, "Initialized:PluginActivity")
    }
    companion object { private const val TAG = "FIMTOWN|PluginActivity" }
}





















