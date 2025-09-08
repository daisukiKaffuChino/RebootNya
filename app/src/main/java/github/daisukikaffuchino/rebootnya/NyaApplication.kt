package github.daisukikaffuchino.rebootnya

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import github.daisukikaffuchino.rebootnya.utils.NyaSettings
import rikka.material.app.LocaleDelegate
import kotlin.system.exitProcess

class NyaApplication : Application() {
    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
    }

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
        NyaSettings.initialize(applicationContext)
        LocaleDelegate.defaultLocale = NyaSettings.getLocale()
        AppCompatDelegate.setDefaultNightMode(NyaSettings.getNightMode(this))
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level == TRIM_MEMORY_UI_HIDDEN) {
            // 所有 UI 不可见后杀死进程
            android.os.Process.killProcess(android.os.Process.myPid())
            exitProcess(0)
        }
    }
}