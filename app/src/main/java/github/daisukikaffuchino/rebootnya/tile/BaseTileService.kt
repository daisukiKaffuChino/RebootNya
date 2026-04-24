package github.daisukikaffuchino.rebootnya.tile

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi

abstract class BaseTileService : TileService() {
    abstract val tileLabelResId: Int

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            label = getString(tileLabelResId)
            state = Tile.STATE_INACTIVE
            updateTile()
        }
    }

    protected fun launchAndCollapse(intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            launchAndCollapseApi34(intent)
        else
            launchAndCollapseLegacy(intent)
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun launchAndCollapseApi34(intent: Intent) {
        val pendingIntent = PendingIntent.getActivity(
            this,
            intent.filterHashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        startActivityAndCollapse(pendingIntent)
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    @Suppress("DEPRECATION")
    private fun launchAndCollapseLegacy(intent: Intent) {
        startActivityAndCollapse(intent)
    }
}
