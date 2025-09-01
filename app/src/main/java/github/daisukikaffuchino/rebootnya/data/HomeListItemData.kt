package github.daisukikaffuchino.rebootnya.data

class HomeListItemData {
    var text: String? = null
    var checked: Boolean = false
    var indexInSection: Int = 0
    var sectionCount: Int = 0

    constructor(text: String?, indexInSection: Int, sectionCount: Int) {
        this.text = text
        this.indexInSection = indexInSection
        this.sectionCount = sectionCount
    }
}