package github.daisukikaffuchino.rebootnya.data

import android.content.Context
import androidx.annotation.StringRes
import github.daisukikaffuchino.rebootnya.R

enum class ListItemEnum(
    val displayName: String,
    @param:StringRes private val labelResId: Int? = null
) {
    LOCK_SCREEN("lock_screen", R.string.lock_screen),
    REBOOT("reboot", R.string.reboot),
    SOFT_REBOOT("soft_reboot", R.string.soft_reboot),
    SYSTEM_UI("system_ui", R.string.system_ui),
    RECOVERY("Recovery"),
    BOOTLOADER("Bootloader"),
    SAMSUNG_DOWNLOAD("samsung_download", R.string.download_mode),
    SAFE_MODE("safe_mode", R.string.safe_mode),
    POWER_OFF("power_off", R.string.power_off);

    fun getLocalizedDisplayName(context: Context): String =
        labelResId?.let(context::getString) ?: displayName

    companion object {
        fun fromDisplayName(displayName: String): ListItemEnum =
            entries.find { it.displayName == displayName } ?: LOCK_SCREEN
    }
}
