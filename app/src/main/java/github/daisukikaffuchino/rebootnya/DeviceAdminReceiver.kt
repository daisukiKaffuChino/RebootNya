package github.daisukikaffuchino.rebootnya

import android.app.admin.DevicePolicyManager
import android.app.admin.DeviceAdminReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.content.edit
import github.daisukikaffuchino.rebootnya.utils.NyaSettings

class DeviceAdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        updateSupportMessages(context)
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        NyaSettings.initialize(context)
        NyaSettings.preferences.edit { putBoolean("device_policy_enabled", false) }
    }

    companion object {
        fun updateSupportMessages(context: Context) {
            val manager = context.getSystemService(DevicePolicyManager::class.java) ?: return
            val admin = ComponentName(context, DeviceAdminReceiver::class.java)
            if (!manager.isAdminActive(admin)) return

            try {
                manager.setShortSupportMessage(
                    admin,
                    context.getString(R.string.device_policy_support_message)
                )
                manager.setLongSupportMessage(
                    admin,
                    context.getString(R.string.device_policy_support_message)
                )
            } catch (_: SecurityException) {
            }
        }
    }
}
