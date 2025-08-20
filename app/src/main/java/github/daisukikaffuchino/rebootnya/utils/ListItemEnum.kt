package github.daisukikaffuchino.rebootnya.utils

import github.daisukikaffuchino.rebootnya.NyaApplication
import github.daisukikaffuchino.rebootnya.R

enum class ListItemEnum(val displayName: String) {
    LOCK_SCREEN("lock_screen"),
    REBOOT("reboot"),
    SOFT_REBOOT("soft_reboot"),
    SYSTEM_UI("system_ui"),
    RECOVERY("Recovery"),
    BOOTLOADER("Bootloader"),
    SAFE_MODE("safe_mode"),
    POWER_OFF("power_off");

    fun getLocalizedDisplayName(): String {
        val context = NyaApplication.context
        return when (this) {
            LOCK_SCREEN -> context.getString(R.string.lock_screen)
            REBOOT -> context.getString(R.string.reboot)
            SOFT_REBOOT -> context.getString(R.string.soft_reboot)
            SYSTEM_UI -> context.getString(R.string.system_ui)
            SAFE_MODE -> context.getString(R.string.safe_mode)
            POWER_OFF -> context.getString(R.string.power_off)
            else -> displayName
        }
    }

    companion object {
        fun fromLocalizedDisplayName(displayName: String): ListItemEnum {
            return entries.find { it.getLocalizedDisplayName() == displayName } ?: LOCK_SCREEN//默认锁屏
        }

        fun fromDisplayName(displayName: String): ListItemEnum {
            return entries.find { it.displayName == displayName } ?: LOCK_SCREEN
        }
    }
}