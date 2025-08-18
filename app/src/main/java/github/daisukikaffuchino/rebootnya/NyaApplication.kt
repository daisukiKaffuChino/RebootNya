package github.daisukikaffuchino.rebootnya

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.SharedPreferences

class NyaApplication : Application() {
    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
        lateinit var sp: SharedPreferences
    }

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
        sp = getSharedPreferences("Nya", MODE_PRIVATE)
    }
}