package github.daisukikaffuchino.rebootnya

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import github.daisukikaffuchino.rebootnya.utils.RootUtil
import github.daisukikaffuchino.rebootnya.utils.ShizukuUtil

class NyaApplication : Application() {
    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
        lateinit var sp: SharedPreferences
        lateinit var shizukuUtil: ShizukuUtil
        lateinit var rootUtil: RootUtil
    }

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
        sp = getSharedPreferences("Nya", MODE_PRIVATE)
        shizukuUtil = ShizukuUtil()
        rootUtil = RootUtil()
    }
}