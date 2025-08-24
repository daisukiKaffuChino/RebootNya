package github.daisukikaffuchino.rebootnya

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toDrawable
import github.daisukikaffuchino.rebootnya.databinding.ActivityMainBinding
import github.daisukikaffuchino.rebootnya.utils.NyaSettings
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

        arrayOf(
            createShortcut(
                "lock_screen",
                getString(R.string.lock_screen),
                R.mipmap.ic_lock_screen,
            ),
            createShortcut(
                "power_off",
                getString(R.string.power_off),
                R.mipmap.ic_shutdown,
            ),
            createShortcut(
                "reboot",
                getString(R.string.reboot),
                R.mipmap.ic_reboot,
            )
        ).forEach {
            ShortcutManagerCompat.addDynamicShortcuts(this, listOf(it))
        }

    }

    private fun createShortcut(
        id: String,
        label: String,
        iconRes: Int,
    ): ShortcutInfoCompat {
        return ShortcutInfoCompat.Builder(this, id)
            .setShortLabel(label)
            .setIcon(IconCompat.createWithResource(this, iconRes))
            .setIntent(Intent(this, MainActivity::class.java).apply {
                action = Intent.ACTION_RUN
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("extra", id)
            })
            .build()
    }

    override fun onResume() {
        super.onResume()
        if (listFilterStatus != checkListFilterStatus() ||
            uiStyleChanged != NyaSettings.getMainInterfaceStyle()
        )
            recreate()
    }

}