package github.daisukikaffuchino.rebootnya.data

data class HomeListItemData(
    val text: String,
    val indexInSection: Int,
    val sectionCount: Int,
    var checked: Boolean
)
