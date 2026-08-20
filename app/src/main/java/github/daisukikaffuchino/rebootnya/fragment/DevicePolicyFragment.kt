package github.daisukikaffuchino.rebootnya.fragment

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.edit
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.TwoStatePreference
import androidx.recyclerview.widget.RecyclerView
import github.daisukikaffuchino.rebootnya.DeviceAdminReceiver
import github.daisukikaffuchino.rebootnya.R
import github.daisukikaffuchino.rebootnya.utils.NyaSettings
import github.daisukikaffuchino.rebootnya.utils.openUrlLink
import rikka.recyclerview.addEdgeSpacing
import rikka.recyclerview.fixEdgeEffect
import rikka.widget.borderview.BorderRecyclerView

class DevicePolicyFragment : PreferenceFragmentCompat() {
    private lateinit var authorizePreference: Preference
    private lateinit var enabledPreference: TwoStatePreference
    private lateinit var revokePreference: Preference

    private fun deviceAdminComponent(context: Context) =
        ComponentName(context, DeviceAdminReceiver::class.java)

    private fun devicePolicyManager(context: Context) =
        context.getSystemService(DevicePolicyManager::class.java)

    private fun isAdminActive(context: Context): Boolean {
        return devicePolicyManager(context)?.isAdminActive(deviceAdminComponent(context)) == true
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.setStorageDeviceProtected()
        preferenceManager.sharedPreferencesName = NyaSettings.NAME
        preferenceManager.sharedPreferencesMode = Context.MODE_PRIVATE
        setPreferencesFromResource(R.xml.preference_device_policy, rootKey)

        authorizePreference = findPreference("device_policy_authorize")!!
        enabledPreference = findPreference("device_policy_enabled")!!
        revokePreference = findPreference("device_policy_revoke")!!
        findPreference<Preference>("device_policy_reference")?.setOnPreferenceClickListener {
            openUrlLink(requireContext(), "https://github.com/F33RNI/LockdownUtility")
            true
        }
        authorizePreference.setOnPreferenceClickListener {
            requestDeviceAdmin()
            true
        }
        enabledPreference.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                if (newValue == true && !isAdminActive(requireContext())) {
                    requestDeviceAdmin()
                    false
                } else {
                    true
                }
            }
        revokePreference.setOnPreferenceClickListener {
            NyaSettings.preferences.edit { putBoolean("device_policy_enabled", false) }
            devicePolicyManager(requireContext())
                ?.removeActiveAdmin(deviceAdminComponent(requireContext()))
            refreshAfterAdminRemoval()
            true
        }
        updatePreferences()
    }

    override fun onResume() {
        super.onResume()
        updatePreferences()
        DeviceAdminReceiver.updateSupportMessages(requireContext())
    }

    private fun requestDeviceAdmin() {
        val context = requireContext()
        val manager = devicePolicyManager(context) ?: return
        if (manager.isAdminActive(deviceAdminComponent(context))) return
        startActivity(Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, deviceAdminComponent(context))
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                getString(R.string.device_policy_support_message)
            )
        })
    }

    private fun updatePreferences() {
        if (!::authorizePreference.isInitialized) return
        val active = isAdminActive(requireContext())
        authorizePreference.summary = getString(
            if (active) R.string.device_policy_authorized else R.string.device_policy_not_authorized
        )
        authorizePreference.isEnabled = !active
        enabledPreference.isEnabled = active
        revokePreference.isVisible = active
        enabledPreference.isChecked =
            active && NyaSettings.preferences.getBoolean("device_policy_enabled", false)
        if (!active) {
            NyaSettings.preferences.edit { putBoolean("device_policy_enabled", false) }
        }
    }

    private fun refreshAfterAdminRemoval(attempt: Int = 0) {
        view?.postDelayed({
            if (!isAdded) return@postDelayed
            updatePreferences()
            if (isAdminActive(requireContext()) && attempt < 10) {
                refreshAfterAdminRemoval(attempt + 1)
            }
        }, 100L)
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
}
