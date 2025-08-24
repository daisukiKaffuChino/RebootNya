package github.daisukikaffuchino.rebootnya.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import github.daisukikaffuchino.rebootnya.MainActivity
import github.daisukikaffuchino.rebootnya.R

class ShortcutHelper(private val context: Context) {

    data class ShortcutItem(
        val id: String,
        val shortLabel: String,
        val iconRes: Int,
    )

    val items: List<ShortcutItem>
        get() {
            return listOf(
                ShortcutItem(
                    "lock_screen",
                    context.getString(R.string.lock_screen),
                    R.mipmap.ic_lock_screen,
                ),
                ShortcutItem(
                    "power_off",
                    context.getString(R.string.power_off),
                    R.mipmap.ic_shutdown,
                ),
                ShortcutItem(
                    "reboot",
                    context.getString(R.string.reboot),
                    R.mipmap.ic_reboot,
                )
            )
        }

    private fun buildShortcutInfo(item: ShortcutItem): ShortcutInfoCompat {
        val builder = ShortcutInfoCompat.Builder(context, item.id)
            .setShortLabel(item.shortLabel)
            .setIcon(IconCompat.createWithResource(context, item.iconRes))
            .setIntent(Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_RUN
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("extra", item.id)
            })

        return builder.build()
    }

    fun setDynamicShortcuts(items: List<ShortcutItem>) {
        val shortcuts = items.map { buildShortcutInfo(it) }
        ShortcutManagerCompat.addDynamicShortcuts(context, shortcuts)
    }

    fun requestPinShortcut(item: ShortcutItem) {
        val shortcut = buildShortcutInfo(item)
        ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
    }

}
