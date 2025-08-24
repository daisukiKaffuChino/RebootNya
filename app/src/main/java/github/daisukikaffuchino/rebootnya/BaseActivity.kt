package github.daisukikaffuchino.rebootnya

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.color.DynamicColors
import github.daisukikaffuchino.rebootnya.utils.NyaSettings
import rikka.core.util.ResourceUtils
import rikka.material.app.LocaleDelegate

open class BaseActivity : AppCompatActivity() {
    private val localeDelegate by lazy {
        LocaleDelegate()
    }

    lateinit var themeChanged: String

    override fun onCreate(savedInstanceState: Bundle?) {
        localeDelegate.onCreate(this)
        if (NyaSettings.preferences.getBoolean("dynamic_color", false))
            DynamicColors.applyToActivityIfAvailable(this)
        super.onCreate(savedInstanceState)
        themeChanged = computeThemeChanged()
    }

    override fun attachBaseContext(newBase: Context) {
        val configuration = newBase.resources.configuration
        localeDelegate.updateConfiguration(configuration)
        super.attachBaseContext(newBase.createConfigurationContext(configuration))
    }

    override fun onResume() {
        super.onResume()
        if (localeDelegate.isLocaleChanged ||
            themeChanged != computeThemeChanged()
        ) recreate()
    }

    private fun computeThemeChanged(): String {
        return NyaSettings.isUsingSystemColor()
            .toString() + ResourceUtils.isNightMode(getResources().configuration).toString()
    }

}