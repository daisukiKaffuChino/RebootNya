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

class MainActivity : BaseActivity() {
    companion object {
        var listFilterStatus by Delegates.notNull<Boolean>()
        fun checkListFilterStatus(): Boolean {
            return Shizuku.pingBinder()
                    && Shizuku.getUid() == 2000
                    && NyaSettings.getWorkMode() == NyaSettings.STORE.SHIZUKU
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

        val binding: ActivityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.getRoot())

        if(!NyaSettings.preferences.getBoolean("isShortcutCreated",false)){
            val shortcutHelper= ShortcutHelper(this)
            shortcutHelper.setDynamicShortcuts(shortcutHelper.items)
            NyaSettings.preferences.edit { putBoolean("isShortcutCreated", true) }
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