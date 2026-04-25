package github.daisukikaffuchino.rebootnya.fragment

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.RecyclerView
import github.daisukikaffuchino.rebootnya.R
import github.daisukikaffuchino.rebootnya.preference.XposedStatusPreference
import github.daisukikaffuchino.rebootnya.utils.NyaSettings
import github.daisukikaffuchino.rebootnya.xposed.XposedServiceBridge
import rikka.recyclerview.addEdgeSpacing
import rikka.recyclerview.fixEdgeEffect
import rikka.widget.borderview.BorderRecyclerView

class XposedFragment : PreferenceFragmentCompat() {

    private lateinit var statusPreference: XposedStatusPreference
    private lateinit var syncNowPreference: Preference
    private lateinit var powerKeyEnabledPreference: Preference
    private lateinit var dependentPreferences: List<Preference>
    private lateinit var sharedPreferences: SharedPreferences

    private val preferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == null || !key.startsWith("xposed_")) {
                return@OnSharedPreferenceChangeListener
            }

            if (key == "xposed_power_key_enabled") {
                updateDependentPreferenceState(
                    sharedPreferences.getBoolean("xposed_power_key_enabled", false)
                )
            }

            XposedServiceBridge.syncLocalConfigToRemote()
            updateStatus()
        }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val context = requireContext()
        preferenceManager.setStorageDeviceProtected()
        preferenceManager.sharedPreferencesName = NyaSettings.NAME
        preferenceManager.sharedPreferencesMode = Context.MODE_PRIVATE
        setPreferencesFromResource(R.xml.preference_xposed, rootKey)
        sharedPreferences = preferenceManager.sharedPreferences!!

        statusPreference = findPreference("xposed_status")!!
        syncNowPreference = findPreference("xposed_sync_now")!!
        powerKeyEnabledPreference = findPreference("xposed_power_key_enabled")!!
        dependentPreferences = listOf(
            findPreference("xposed_fallback_app_missing")!!,
            findPreference("xposed_fallback_start_failed")!!,
            findPreference("xposed_fallback_when_locked")!!,
            findPreference("xposed_keep_very_long_press")!!
        )

        powerKeyEnabledPreference.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                updateDependentPreferenceState(newValue as? Boolean == true)
                true
            }

        syncNowPreference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val synced = XposedServiceBridge.syncLocalConfigToRemote()
            val message = if (synced) R.string.xposed_sync_success else R.string.xposed_sync_failed
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            updateStatus()
            true
        }

        updateDependentPreferenceState(
            sharedPreferences.getBoolean("xposed_power_key_enabled", false)
        )
        updateStatus()
    }

    override fun onResume() {
        super.onResume()
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
        XposedServiceBridge.syncLocalConfigToRemote()
        updateStatus()
    }

    override fun onPause() {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
        super.onPause()
    }

    override fun onCreateRecyclerView(
        inflater: LayoutInflater,
        parent: ViewGroup,
        savedInstanceState: Bundle?
    ): RecyclerView {
        val recyclerView =
            super.onCreateRecyclerView(inflater, parent, savedInstanceState) as BorderRecyclerView
        recyclerView.fixEdgeEffect()
        recyclerView.addEdgeSpacing(bottom = 8f, unit = TypedValue.COMPLEX_UNIT_DIP)

        return recyclerView
    }

    private fun updateStatus() {
        val connected = XposedServiceBridge.isConnected()
        statusPreference.isModuleRunning = connected
        statusPreference.statusMessage = if (connected) {
            getString(R.string.xposed_service_connected)
        } else {
            getString(R.string.xposed_service_disconnected)
        }
    }

    private fun updateDependentPreferenceState(enabled: Boolean) {
        dependentPreferences.forEach { it.isEnabled = enabled }
    }
}
