package github.daisukikaffuchino.rebootnya.adapter

data class SettingsSection(
    val title: String,
    val rows: List<SettingsRow>
)

sealed class SettingsRow {
    data class Choice(
        val title: String,
        val entries: List<String>,
        val values: List<Int>,
        val selectedValue: Int,
        val onSelected: (Int) -> Unit
    ) : SettingsRow()

    data class StringChoice(
        val title: String,
        val entries: List<CharSequence>,
        val values: List<String>,
        val selectedValue: String?,
        val onSelected: (String) -> Unit
    ) : SettingsRow()

    data class Switch(
        val title: String,
        val summary: CharSequence? = null,
        val checked: Boolean,
        val enabled: Boolean = true,
        val disableAnimation: Boolean = false,
        val onCheckedChange: (Boolean) -> Unit
    ) : SettingsRow()

    data class Action(
        val title: String,
        val summary: CharSequence? = null,
        val enabled: Boolean = true,
        val onClick: () -> Unit
    ) : SettingsRow()

    data object Command : SettingsRow()
}
