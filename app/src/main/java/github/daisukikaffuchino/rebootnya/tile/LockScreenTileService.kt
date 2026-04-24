package github.daisukikaffuchino.rebootnya.tile

import github.daisukikaffuchino.rebootnya.MainActivity
import github.daisukikaffuchino.rebootnya.R
import github.daisukikaffuchino.rebootnya.data.ListItemEnum

class LockScreenTileService : BaseTileService() {
    override val tileLabelResId: Int = R.string.lock_screen

    override fun onClick() {
        super.onClick()
        launchAndCollapse(MainActivity.createRunIntent(this, ListItemEnum.LOCK_SCREEN))
    }
}
