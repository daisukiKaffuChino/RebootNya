package github.daisukikaffuchino.rebootnya.fragment

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.core.os.BundleCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import github.daisukikaffuchino.rebootnya.BaseActivity
import github.daisukikaffuchino.rebootnya.R
import github.daisukikaffuchino.rebootnya.SettingsActivity
import github.daisukikaffuchino.rebootnya.adapter.SettingsAdapter
import github.daisukikaffuchino.rebootnya.adapter.SettingsItemSpacingDecoration
import github.daisukikaffuchino.rebootnya.adapter.SettingsRow
import github.daisukikaffuchino.rebootnya.adapter.SettingsSection
import github.daisukikaffuchino.rebootnya.data.AppLocales
import github.daisukikaffuchino.rebootnya.tile.LockScreenTileService
import github.daisukikaffuchino.rebootnya.tile.PowerMenuTileService
import github.daisukikaffuchino.rebootnya.utils.NyaSettings
import github.daisukikaffuchino.rebootnya.utils.ShortcutHelper
import github.daisukikaffuchino.rebootnya.utils.openUrlLink
import github.daisukikaffuchino.rebootnya.utils.sendEmail
import rikka.material.app.LocaleDelegate
import java.util.Locale

class SettingsFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private var recyclerLayoutState: Parcelable? = null
    private var settingsAdapter: SettingsAdapter? = null
    private var commandText: String? = null
    private var commandEditText: TextInputEditText? = null
    private var keyboardLayoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null
    private val commandExecutor by lazy {
        SettingsCommandExecutor(this, ::clearCommand)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        commandText = savedInstanceState?.getString(STATE_COMMAND_TEXT)
        recyclerLayoutState = savedInstanceState?.let {
            BundleCompat.getParcelable(it, STATE_RECYCLER_LAYOUT, Parcelable::class.java)
        }
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById<RecyclerView>(R.id.settings_recycler_view).apply {
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(SettingsItemSpacingDecoration(context))
            adapter = SettingsAdapter(
                sections = buildSections(),
                initialCommandText = commandText,
                onCommandEditTextBound = { commandEditText = it },
                onCommandSubmit = commandExecutor::execute
            ).also { settingsAdapter = it }
        }
        recyclerLayoutState?.let { state ->
            recyclerView.post {
                recyclerView.layoutManager?.onRestoreInstanceState(state)
                recyclerLayoutState = null
            }
        }
        val rootView = requireActivity().window.decorView
        keyboardLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            val rect = android.graphics.Rect()
            rootView.getWindowVisibleDisplayFrame(rect)
            val screenHeight = rootView.rootView.height
            val keypadHeight = screenHeight - rect.bottom
            if (keypadHeight < screenHeight * 0.15) {
                commandEditText?.clearFocus()
                val context = context ?: return@OnGlobalLayoutListener
                val imm = context.getSystemService(InputMethodManager::class.java)
                imm?.hideSoftInputFromWindow(commandEditText?.windowToken, 0)
            }
        }
        keyboardLayoutListener?.let(rootView.viewTreeObserver::addOnGlobalLayoutListener)
    }

    override fun onDestroyView() {
        val rootView = activity?.window?.decorView
        keyboardLayoutListener?.let { listener ->
            rootView?.viewTreeObserver?.removeOnGlobalLayoutListener(listener)
        }
        keyboardLayoutListener = null
        commandEditText = null
        settingsAdapter = null
        super.onDestroyView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_COMMAND_TEXT, settingsAdapter?.commandText ?: commandText)
        outState.putParcelable(
            STATE_RECYCLER_LAYOUT,
            recyclerView.layoutManager?.onSaveInstanceState()
        )
    }

    private fun buildSections(): List<SettingsSection> {
        return listOf(
            buildLaunchSection(),
            buildAdvancedSection(),
            buildAppearanceSection(),
            buildOtherSection(),
            buildLanguageSection(),
            buildAboutSection()
        )
    }

    private fun buildLaunchSection(): SettingsSection {
        val context = requireContext()
        val workMode = NyaSettings.getWorkMode()

        return SettingsSection(
            title = getString(R.string.launch),
            rows = buildList {
                add(
                    SettingsRow.Choice(
                        title = getString(R.string.work_mode),
                        entries = context.resources.getStringArray(R.array.work_mode).toList(),
                        values = context.resources.getIntArray(R.array.work_mode_value).toList(),
                        selectedValue = workMode,
                        onSelected = { value ->
                            if (workMode != value) {
                                NyaSettings.preferences.edit { putInt("work_mode", value) }
                                (activity as? BaseActivity)?.recreateWithFade() ?: activity?.recreate()
                            }
                        }
                    )
                )
                if (workMode == NyaSettings.MODE.SHIZUKU) {
                    add(
                        SettingsRow.Choice(
                            title = getString(R.string.shizuku_exec_mode),
                            entries = context.resources.getStringArray(R.array.shell_mode).toList(),
                            values = context.resources.getIntArray(R.array.shell_mode_value).toList(),
                            selectedValue = NyaSettings.getShizukuShellMode(),
                            onSelected = { value ->
                                NyaSettings.preferences.edit { putInt("shizuku_shell_mode", value) }
                            }
                        )
                    )
                    add(
                        SettingsRow.Action(
                            title = getString(R.string.about_user_service),
                            onClick = ::showUserServiceInfo
                        )
                    )
                    add(
                        SettingsRow.Switch(
                            title = getString(R.string.hide_unavailable),
                            summary = getString(R.string.hide_unavailable_summary),
                            checked = NyaSettings.getIsHideUnavailableOptions(),
                            onCheckedChange = { checked ->
                                NyaSettings.preferences.edit {
                                    putBoolean("hide_unavailable_options", checked)
                                }
                            }
                        )
                    )
                }
            }
        )
    }

    private fun buildAdvancedSection(): SettingsSection {
        return SettingsSection(
            title = getString(R.string.advanced),
            rows = listOf(SettingsRow.Command)
        )
    }

    private fun buildAppearanceSection(): SettingsSection {
        val context = requireContext()
        return SettingsSection(
            title = getString(R.string.interface_appearance),
            rows = listOf(
                SettingsRow.Choice(
                    title = getString(R.string.dark_theme),
                    entries = context.resources.getStringArray(R.array.night_mode).toList(),
                    values = context.resources.getIntArray(R.array.night_mode_value).toList(),
                    selectedValue = NyaSettings.getNightMode(context),
                    onSelected = { value ->
                        if (NyaSettings.getNightMode(context) != value) {
                            NyaSettings.preferences.edit { putInt("night_mode", value) }
                            AppCompatDelegate.setDefaultNightMode(value)
                            (activity as? BaseActivity)?.recreateWithFade() ?: activity?.recreate()
                        }
                    }
                ),
                SettingsRow.Switch(
                    title = getString(R.string.dynamic_color),
                    summary = getString(R.string.require_a12),
                    checked = NyaSettings.isUsingSystemColor(),
                    enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                    disableAnimation = true,
                    onCheckedChange = { checked ->
                        if (NyaSettings.isUsingSystemColor() != checked) {
                            NyaSettings.preferences.edit { putBoolean("dynamic_color", checked) }
                            (activity as? BaseActivity)?.recreateWithFade() ?: activity?.recreate()
                        }
                    }
                ),
                SettingsRow.Choice(
                    title = getString(R.string.main_interface_style),
                    entries = context.resources.getStringArray(R.array.main_interface_style).toList(),
                    values = context.resources.getIntArray(R.array.main_interface_style_value).toList(),
                    selectedValue = NyaSettings.getMainInterfaceStyle(),
                    onSelected = { value ->
                        NyaSettings.preferences.edit { putInt("main_interface_style", value) }
                    }
                )
            )
        )
    }

    private fun buildOtherSection(): SettingsSection {
        val context = requireContext()
        return SettingsSection(
            title = getString(R.string.others),
            rows = listOf(
                SettingsRow.Action(
                    title = getString(R.string.quick_add_tile),
                    summary = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) null
                    else getString(R.string.require_a13),
                    enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            showQuickAddTileDialog(context)
                        }
                    }
                ),
                SettingsRow.Action(
                    title = getString(R.string.request_pin_shortcuts),
                    onClick = ::showPinShortcutDialog
                ),
                SettingsRow.Action(
                    title = getString(R.string.clear_created_shortcuts),
                    summary = getString(R.string.create_next_launch),
                    onClick = {
                        ShortcutManagerCompat.removeAllDynamicShortcuts(context)
                        NyaSettings.preferences.edit { putBoolean("isShortcutCreated", false) }
                        Toast.makeText(context, R.string.cleared, Toast.LENGTH_SHORT).show()
                    }
                )
            )
        )
    }

    private fun buildLanguageSection(): SettingsSection {
        val context = requireContext()
        return SettingsSection(
            title = getString(R.string.language),
            rows = listOf(
                SettingsRow.StringChoice(
                    title = getString(R.string.language),
                    entries = buildLocalizedLocales(),
                    values = AppLocales.LOCALES.map { it.toString() },
                    selectedValue = getCurrentLanguageValue(),
                    onSelected = ::setLanguage
                ),
                SettingsRow.Action(
                    title = getString(R.string.participate_translation),
                    summary = getString(
                        R.string.translation_summary,
                        getString(R.string.app_name)
                    ),
                    onClick = { openUrlLink(context, "https://crowdin.com/project/rebootnya") }
                )
            )
        )
    }

    private fun buildAboutSection(): SettingsSection {
        val context = requireContext()
        return SettingsSection(
            title = getString(R.string.about),
            rows = listOf(
                SettingsRow.Action(
                    title = getString(R.string.developer),
                    summary = "Github@daisukiKaffuChino",
                    onClick = ::showDeveloperDialog
                ),
                SettingsRow.Action(
                    title = "RebootNya Open-Source Project",
                    summary = getString(R.string.license),
                    onClick = {
                        openUrlLink(context, "https://github.com/daisukiKaffuChino/RebootNya")
                    }
                ),
                SettingsRow.Action(
                    title = getString(R.string.open_source_license),
                    onClick = {
                        (activity as? SettingsActivity)?.openLicenseFragment()
                    }
                )
            )
        )
    }

    private fun showUserServiceInfo() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.about_user_service)
            .setMessage(R.string.about_user_service_content)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun showPinShortcutDialog() {
        val context = requireContext()
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.request_pin_shortcuts)
            .setItems(
                arrayOf(
                    getString(R.string.lock_screen),
                    getString(R.string.power_off),
                    getString(R.string.reboot)
                )
            ) { _, index ->
                val shortcutHelper = ShortcutHelper(context)
                shortcutHelper.requestPinShortcut(shortcutHelper.items[index])
            }
            .show()
    }

    private fun showDeveloperDialog() {
        val context = requireContext()
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.contact_me)
            .setItems(
                arrayOf(
                    getString(R.string.email),
                    getString(R.string.bilibili)
                )
            ) { _, index ->
                when (index) {
                    0 -> sendEmail(context)
                    1 -> openUrlLink(context, "https://space.bilibili.com/178423358")
                }
            }
            .show()
    }

    private fun buildLocalizedLocales(): List<CharSequence> {
        val displayLocaleTags = AppLocales.DISPLAY_LOCALES
        val currentLocale = NyaSettings.getLocale()
        return displayLocaleTags.mapIndexed { index, displayLocale ->
            if (index == 0) {
                getString(R.string.follow_system)
            } else {
                val locale = Locale.forLanguageTag(displayLocale.toString())
                if (!TextUtils.isEmpty(locale.script)) {
                    locale.getDisplayScript(currentLocale)
                } else {
                    locale.getDisplayName(currentLocale)
                }
            }
        }
    }

    private fun getCurrentLanguageValue(): String {
        val language = NyaSettings.preferences.getString("language", "SYSTEM")
        return if (TextUtils.isEmpty(language)) "SYSTEM" else language.toString()
    }

    private fun setLanguage(newValue: String) {
        if (NyaSettings.preferences.getString("language", "SYSTEM") == newValue) return
        NyaSettings.preferences.edit { putString("language", newValue) }
        LocaleDelegate.defaultLocale = if ("SYSTEM" == newValue) {
            LocaleDelegate.systemLocale
        } else {
            Locale.forLanguageTag(newValue)
        }
        (activity as? BaseActivity)?.recreateWithFade() ?: activity?.recreate()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun showQuickAddTileDialog(context: Context) {
        val tileOptions = listOf(
            QuickTileOption(
                R.string.tile_power_menu,
                R.drawable.ic_tile_power_menu,
                PowerMenuTileService::class.java
            ),
            QuickTileOption(
                R.string.lock_screen,
                R.drawable.ic_tile_lock_screen,
                LockScreenTileService::class.java
            )
        )

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.quick_add_tile)
            .setItems(tileOptions.map { getString(it.labelResId) }.toTypedArray()) { _, which ->
                requestAddTile(tileOptions[which])
            }
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestAddTile(option: QuickTileOption) {
        val activity = activity ?: return
        val statusBarManager = activity.getSystemService(StatusBarManager::class.java)
        if (statusBarManager == null) {
            Toast.makeText(activity, R.string.not_support, Toast.LENGTH_SHORT).show()
            return
        }

        statusBarManager.requestAddTileService(
            ComponentName(activity, option.serviceClass),
            getString(option.labelResId),
            Icon.createWithResource(activity, option.iconResId),
            activity.mainExecutor
        ) { result ->
            val messageResId = when (result) {
                StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED -> R.string.tile_add_success
                StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED -> R.string.tile_already_added
                StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_NOT_ADDED -> R.string.tile_add_cancelled
                else -> R.string.tile_add_failed
            }
            Toast.makeText(activity, messageResId, Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearCommand() {
        commandText = null
        settingsAdapter?.clearCommandText()
    }

    private data class QuickTileOption(
        val labelResId: Int,
        val iconResId: Int,
        val serviceClass: Class<*>
    )

    companion object {
        private const val STATE_COMMAND_TEXT = "command_text"
        private const val STATE_RECYCLER_LAYOUT = "recycler_layout"
    }
}
