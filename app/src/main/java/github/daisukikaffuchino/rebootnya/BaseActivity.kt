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
    private var recreateWithFadePending = false
    private var playEnterFadeAfterRecreate = false

    lateinit var themeChanged: String

    override fun onCreate(savedInstanceState: Bundle?) {
        recreateWithFadePending = false
        playEnterFadeAfterRecreate =
            savedInstanceState?.getBoolean(STATE_PLAY_ENTER_FADE_AFTER_RECREATE, false) == true
        localeDelegate.onCreate(this)
        if (NyaSettings.preferences.getBoolean("dynamic_color", false))
            DynamicColors.applyToActivityIfAvailable(this)
        super.onCreate(savedInstanceState)
        themeChanged = computeThemeChanged()
        if (playEnterFadeAfterRecreate) {
            window?.decorView?.alpha = 0f
        }
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
        ) recreateWithFade()
    }

    override fun onPostResume() {
        super.onPostResume()
        if (playEnterFadeAfterRecreate) {
            playEnterFadeAfterRecreate = false
            window?.decorView?.animate()
                ?.alpha(1f)
                ?.setDuration(180L)
                ?.start()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(
            STATE_PLAY_ENTER_FADE_AFTER_RECREATE,
            playEnterFadeAfterRecreate || recreateWithFadePending
        )
    }

    fun recreateWithFade() {
        if (recreateWithFadePending) return
        recreateWithFadePending = true
        playEnterFadeAfterRecreate = true

        val decorView = window?.decorView ?: run {
            recreate()
            return
        }
        decorView.animate().cancel()
        decorView.animate()
            .alpha(0f)
            .setDuration(140L)
            .withEndAction {
                if (!isFinishing && !isDestroyed) {
                    recreate()
                }
            }
            .start()
    }

    private fun computeThemeChanged(): String {
        return NyaSettings.isUsingSystemColor()
            .toString() + ResourceUtils.isNightMode(getResources().configuration).toString()
    }

    companion object {
        private const val STATE_PLAY_ENTER_FADE_AFTER_RECREATE = "play_enter_fade_after_recreate"
    }
}
