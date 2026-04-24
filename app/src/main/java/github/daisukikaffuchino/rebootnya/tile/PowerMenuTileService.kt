package github.daisukikaffuchino.rebootnya.tile

import github.daisukikaffuchino.rebootnya.MainActivity
import github.daisukikaffuchino.rebootnya.R

class PowerMenuTileService : BaseTileService() {
    override val tileLabelResId: Int = R.string.tile_power_menu

    override fun onClick() {
        super.onClick()
        launchAndCollapse(MainActivity.createLaunchIntent(this))
    }
}
