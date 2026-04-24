package github.daisukikaffuchino.rebootnya.utils

import android.content.Context
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import github.daisukikaffuchino.rebootnya.MainActivity
import github.daisukikaffuchino.rebootnya.R
import github.daisukikaffuchino.rebootnya.data.ListItemEnum

class ShortcutHelper(private val context: Context) {

    data class ShortcutItem(
        val item: ListItemEnum,
        val shortLabel: String,
        val iconRes: Int,
    )

    val items: List<ShortcutItem>
        get() {
            return listOf(
                ShortcutItem(
                    ListItemEnum.LOCK_SCREEN,
                    context.getString(R.string.lock_screen),
                    R.mipmap.ic_lock_screen,
                ),
                ShortcutItem(
                    ListItemEnum.POWER_OFF,
                    context.getString(R.string.power_off),
                    R.mipmap.ic_shutdown,
                ),
                ShortcutItem(
                    ListItemEnum.REBOOT,
                    context.getString(R.string.reboot),
                    R.mipmap.ic_reboot,
                )
            )
        }

    private fun buildShortcutInfo(item: ShortcutItem): ShortcutInfoCompat {
        val builder = ShortcutInfoCompat.Builder(context, item.id)
            .setShortLabel(item.shortLabel)
            .setIcon(IconCompat.createWithResource(context, item.iconRes))
            .setIntent(MainActivity.createRunIntent(context, item.item))

        return builder.build()
    }

    private val ShortcutItem.id: String
        get() = item.displayName

    fun setDynamicShortcuts(items: List<ShortcutItem>) {
        val shortcuts = items.map { buildShortcutInfo(it) }
        ShortcutManagerCompat.addDynamicShortcuts(context, shortcuts)
    }

    fun requestPinShortcut(item: ShortcutItem) {
        val shortcut = buildShortcutInfo(item)
        ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
    }

}
