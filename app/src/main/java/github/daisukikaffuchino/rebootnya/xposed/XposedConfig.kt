package github.daisukikaffuchino.rebootnya.xposed

import android.content.SharedPreferences
import androidx.core.content.edit
import github.daisukikaffuchino.rebootnya.utils.NyaSettings

object XposedConfig {
    const val REMOTE_GROUP = "power_key_long_press"

    const val KEY_ENABLED = "xposed_power_key_enabled"
    const val KEY_FALLBACK_WHEN_APP_MISSING = "xposed_fallback_app_missing"
    const val KEY_FALLBACK_WHEN_START_FAILED = "xposed_fallback_start_failed"
    const val KEY_FALLBACK_WHEN_LOCKED = "xposed_fallback_when_locked"
    const val KEY_KEEP_VERY_LONG_PRESS = "xposed_keep_very_long_press"

    const val DEFAULT_ENABLED = false
    const val DEFAULT_FALLBACK_WHEN_APP_MISSING = true
    const val DEFAULT_FALLBACK_WHEN_START_FAILED = true
    const val DEFAULT_FALLBACK_WHEN_LOCKED = true
    const val DEFAULT_KEEP_VERY_LONG_PRESS = true

    data class Snapshot(
        val enabled: Boolean,
        val fallbackWhenAppMissing: Boolean,
        val fallbackWhenStartFailed: Boolean,
        val fallbackWhenLocked: Boolean,
        val keepVeryLongPress: Boolean
    )

    @JvmStatic
    fun read(preferences: SharedPreferences): Snapshot {
        return Snapshot(
            enabled = preferences.getBoolean(KEY_ENABLED, DEFAULT_ENABLED),
            fallbackWhenAppMissing = preferences.getBoolean(
                KEY_FALLBACK_WHEN_APP_MISSING,
                DEFAULT_FALLBACK_WHEN_APP_MISSING
            ),
            fallbackWhenStartFailed = preferences.getBoolean(
                KEY_FALLBACK_WHEN_START_FAILED,
                DEFAULT_FALLBACK_WHEN_START_FAILED
            ),
            fallbackWhenLocked = preferences.getBoolean(
                KEY_FALLBACK_WHEN_LOCKED,
                DEFAULT_FALLBACK_WHEN_LOCKED
            ),
            keepVeryLongPress = preferences.getBoolean(
                KEY_KEEP_VERY_LONG_PRESS,
                DEFAULT_KEEP_VERY_LONG_PRESS
            )
        )
    }

    @JvmStatic
    fun readLocal(): Snapshot = read(NyaSettings.preferences)

    @JvmStatic
    fun write(preferences: SharedPreferences, value: Snapshot) {
        preferences.edit {
            putBoolean(KEY_ENABLED, value.enabled)
            putBoolean(KEY_FALLBACK_WHEN_APP_MISSING, value.fallbackWhenAppMissing)
            putBoolean(KEY_FALLBACK_WHEN_START_FAILED, value.fallbackWhenStartFailed)
            putBoolean(KEY_FALLBACK_WHEN_LOCKED, value.fallbackWhenLocked)
            putBoolean(KEY_KEEP_VERY_LONG_PRESS, value.keepVeryLongPress)
        }
    }
}
