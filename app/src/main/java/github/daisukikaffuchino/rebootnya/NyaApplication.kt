package github.daisukikaffuchino.rebootnya

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import github.daisukikaffuchino.rebootnya.utils.NyaSettings
import rikka.material.app.LocaleDelegate

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
}