package github.daisukikaffuchino.rebootnya

import android.graphics.Color
import android.os.Bundle
import androidx.core.content.edit
import androidx.core.graphics.drawable.toDrawable
import github.daisukikaffuchino.rebootnya.databinding.ActivityMainBinding
import github.daisukikaffuchino.rebootnya.utils.NyaSettings
import github.daisukikaffuchino.rebootnya.utils.ShortcutHelper
import rikka.shizuku.Shizuku
import kotlin.properties.Delegates

import android.content.Intent

import androidx.core.view.WindowCompat

class MainActivity : BaseActivity() {
    companion object {
        const val ACTION_LAUNCH = "github.daisukikaffuchino.rebootnya.action.LAUNCH"
        const val ACTION_CLOSE = "github.daisukikaffuchino.rebootnya.action.CLOSE"

        var listFilterStatus by Delegates.notNull<Boolean>()
        fun checkListFilterStatus(): Boolean {
            return Shizuku.pingBinder()
                    && Shizuku.getUid() == 2000
                    && NyaSettings.getWorkMode() == NyaSettings.MODE.SHIZUKU
                    && NyaSettings.getIsHideUnavailableOptions()
        }
    }

    var uiStyleChanged by Delegates.notNull<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        listFilterStatus = checkListFilterStatus()
        uiStyleChanged = NyaSettings.getMainInterfaceStyle()

        val window = getWindow()
        window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        if (window != null) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }

        val binding: ActivityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.getRoot())

        if(!NyaSettings.preferences.getBoolean("isShortcutCreated",false)){
            val shortcutHelper= ShortcutHelper(this)
            shortcutHelper.setDynamicShortcuts(shortcutHelper.items)
            NyaSettings.preferences.edit { putBoolean("isShortcutCreated", true) }
        }

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        when (intent.action) {
            ACTION_LAUNCH -> {
                // Already launched, nothing specific needed as activity is brought to front
            }
            ACTION_CLOSE -> {
                finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (listFilterStatus != checkListFilterStatus() ||
            uiStyleChanged != NyaSettings.getMainInterfaceStyle()
        )
            recreate()
    }

}