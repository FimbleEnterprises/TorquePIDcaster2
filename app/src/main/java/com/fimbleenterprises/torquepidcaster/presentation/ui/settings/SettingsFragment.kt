package com.fimbleenterprises.torquepidcaster.presentation.ui.settings

import android.content.*
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.fimbleenterprises.torquepidcaster.MyApp
import com.fimbleenterprises.torquepidcaster.R
import com.fimbleenterprises.torquepidcaster.util.Helpers
import dagger.hilt.android.AndroidEntryPoint
import java.util.prefs.PreferenceChangeEvent
import java.util.prefs.PreferenceChangeListener


@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat(), PreferenceChangeListener,
    SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        PreferenceManager.getDefaultSharedPreferences(requireContext())
        preferenceManager.sharedPreferences!!.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onResume() {
        super.onResume()

        val prefBatteryOpt = findPreference<Preference>(PREF_BATT_OPT)!!
        prefBatteryOpt.setOnPreferenceClickListener {
            Helpers.Application.sendToAppSettings(activity)
            true
        }

        val prefCopyEcuConn = findPreference<Preference>(PREF_COPY_ECU_CONN_CLIPBOARD)
        prefCopyEcuConn?.setOnPreferenceClickListener {
            Context.CLIPBOARD_SERVICE
            val clipboard: ClipboardManager? = ContextCompat.getSystemService(requireContext(), ClipboardManager::class.java)
            val clip = ClipData.newPlainText("", getString(R.string.fully_qualified_broadcast, MyApp.AppPreferences.ecuConnectedBroadcastAction))
            clipboard?.setPrimaryClip(clip)
            Toast.makeText(context, getString(R.string.copied), Toast.LENGTH_SHORT).show()
            true
        }

        val prefCopyEcuDisConn = findPreference<Preference>(PREF_COPY_ECU_DISCONN_CLIPBOARD)
        prefCopyEcuDisConn?.setOnPreferenceClickListener {
            Context.CLIPBOARD_SERVICE
            val clipboard: ClipboardManager? = ContextCompat.getSystemService(requireContext(), ClipboardManager::class.java)
            val clip = ClipData.newPlainText("", getString(R.string.fully_qualified_broadcast, MyApp.AppPreferences.ecuDisconnectedBroadcastAction))
            clipboard?.setPrimaryClip(clip)
            Toast.makeText(context, getString(R.string.copied), Toast.LENGTH_SHORT).show()
            true
        }

        when (Helpers.Application.isIgnoringBatteryOptimizations(activity)) {
            true -> {
                prefBatteryOpt.summary = getString(R.string.pref_batt_opt_okay)
            }
            false -> {
                prefBatteryOpt.summary = getString(R.string.pref_go_to_app_settings_summary)
            }
        }

    }

    init { Log.i(TAG, "Initialized:SettingsFragment") }
    companion object {
        private const val TAG = "FIMTOWN|SettingsFragment"
        private const val PREF_WHILE_CONNECTED_ACTION = "PREF_WHILE_CONNECTED_ACTION"
        private const val PREF_WHILE_DISCONNECTED_ACTION = "PREF_WHILE_DISCONNECTED_ACTION"
        private const val PREF_BATT_OPT = "PREF_BATT_OPT"
        private const val PREF_COPY_ECU_DISCONN_CLIPBOARD = "PREF_COPY_ECU_DISCONN_CLIPBOARD"
        private const val PREF_COPY_ECU_CONN_CLIPBOARD = "PREF_COPY_ECU_CONN_CLIPBOARD"
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            // It's a long damn string - copy it to the clipboard for the poor bastards.
            PREF_WHILE_CONNECTED_ACTION -> {
                Context.CLIPBOARD_SERVICE
                val clipboard: ClipboardManager? =
                    ContextCompat.getSystemService(requireContext(), ClipboardManager::class.java)
                val clip = ClipData.newPlainText("", MyApp.AppPreferences.ecuConnectedBroadcastAction)
                clipboard?.setPrimaryClip(clip)
                Toast.makeText(context, getString(R.string.copied), Toast.LENGTH_SHORT).show()
            }
            // It's a long damn string - copy it to the clipboard for the poor bastards.
            PREF_WHILE_DISCONNECTED_ACTION -> {
                Context.CLIPBOARD_SERVICE
                val clipboard: ClipboardManager? =
                    ContextCompat.getSystemService(requireContext(), ClipboardManager::class.java)
                val clip = ClipData.newPlainText("", MyApp.AppPreferences.ecuDisconnectedBroadcastAction)
                clipboard?.setPrimaryClip(clip)
                Toast.makeText(context, getString(R.string.copied), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun preferenceChange(evt: PreferenceChangeEvent?) { /*null*/ }
}