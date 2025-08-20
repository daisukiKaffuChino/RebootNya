package github.daisukikaffuchino.rebootnya

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toDrawable
import com.google.android.material.color.DynamicColors
import github.daisukikaffuchino.rebootnya.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val window = getWindow()
        window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

        if (NyaApplication.sp.getBoolean("monet", false))
            DynamicColors.applyToActivityIfAvailable(this)

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
                putExtra("extra", id)
            })
            .build()
    }
}