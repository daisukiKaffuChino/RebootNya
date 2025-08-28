package github.daisukikaffuchino.rebootnya.utils

import android.annotation.SuppressLint
import android.app.UiModeManager
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import android.text.TextUtils
import androidx.annotation.IntDef
import androidx.appcompat.app.AppCompatDelegate
import java.util.Locale

object NyaSettings {
    const val NAME = "settings"

    private var sPreferences: SharedPreferences? = null

    @JvmStatic
    val preferences: SharedPreferences
        get() {
            if (sPreferences == null) {
                throw IllegalStateException("NyaSettings has not been initialized. Call NyaSettings.initialize(context) first.")
            }
            return sPreferences!!
        }

    @JvmStatic
    private fun getSettingsStorageContext(context: Context): Context {
        var storageContext: Context = context.createDeviceProtectedStorageContext()

        storageContext = object : ContextWrapper(storageContext) {
            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences {
                return try {
                    super.getSharedPreferences(name, mode)
                } catch (_: IllegalStateException) {
                    EmptySharedPreferencesImpl()
                }
            }
        }
        return storageContext
    }

    @JvmStatic
    fun initialize(context: Context) {
        if (sPreferences == null) {
            sPreferences = getSettingsStorageContext(context)
                .getSharedPreferences(NAME, Context.MODE_PRIVATE)
        }
    }

    @SuppressLint("UniqueConstants")
    @IntDef(
        MODE.SHIZUKU,
        MODE.ROOT,
        MODE.PROCESS,
        MODE.USER_SERVICE
    )
    @Retention(AnnotationRetention.SOURCE)
    annotation class MODE {
        companion object {
            const val SHIZUKU = 1
            const val ROOT = 2
            const val PROCESS = 1 // Note: Same value as SHIZUKU
            const val USER_SERVICE = 2 // Note: Same value as ROOT
        }
    }

    @IntDef(
        STYLE.CLASSIC_LIST,
        STYLE.MODERN_BUTTONS
    )
    @Retention(AnnotationRetention.SOURCE)
    annotation class STYLE {
        companion object {
            const val CLASSIC_LIST = 1
            const val MODERN_BUTTONS = 2
        }
    }

    @JvmStatic
    @MODE
    fun getWorkMode(): Int {
        return preferences.getInt("work_mode", MODE.SHIZUKU)
    }

    @JvmStatic
    @STYLE
    fun getMainInterfaceStyle(): Int {
        return preferences.getInt("main_interface_style", STYLE.CLASSIC_LIST)
    }

    @JvmStatic
    @MODE
    fun getShizukuShellMode(): Int {
        return preferences.getInt("shizuku_shell_mode", MODE.PROCESS)
    }

    @JvmStatic
    @AppCompatDelegate.NightMode
    fun getNightMode(context: Context): Int {
        var defValue = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        val uiModeManager = context.getSystemService(UiModeManager::class.java)
        if (uiModeManager != null && uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_WATCH) {
            defValue = AppCompatDelegate.MODE_NIGHT_YES
        }
        return preferences.getInt("night_mode", defValue)
    }

    @JvmStatic
    fun getLocale(): Locale {
        val tag = preferences.getString("language", null)
        return if (TextUtils.isEmpty(tag) || "SYSTEM" == tag) {
            Locale.getDefault()
        } else {
            Locale.forLanguageTag(tag!!)
        }
    }

    @JvmStatic
    fun getIsHideUnavailableOptions(): Boolean {
        return preferences.getBoolean("hide_unavailable_options", true)
    }

    @JvmStatic
    fun isUsingSystemColor(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                preferences.getBoolean("dynamic_color", false)
    }
}
