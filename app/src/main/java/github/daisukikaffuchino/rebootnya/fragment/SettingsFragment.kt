package github.daisukikaffuchino.rebootnya.fragment

import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.TwoStatePreference
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import github.daisukikaffuchino.rebootnya.R
import github.daisukikaffuchino.rebootnya.preference.EditTextPreference
import github.daisukikaffuchino.rebootnya.preference.IntegerSimpleMenuPreference
import github.daisukikaffuchino.rebootnya.data.AppLocales
import github.daisukikaffuchino.rebootnya.utils.NyaSettings
import github.daisukikaffuchino.rebootnya.utils.ShortcutHelper
import github.daisukikaffuchino.rebootnya.utils.openUrlLink
import github.daisukikaffuchino.rebootnya.utils.sendEmail
import github.daisukikaffuchino.rebootnya.utils.toHtml
import rikka.material.app.LocaleDelegate
import rikka.recyclerview.addEdgeSpacing
import rikka.recyclerview.fixEdgeEffect
import rikka.widget.borderview.BorderRecyclerView
import java.util.Locale


class SettingsFragment : PreferenceFragmentCompat() {

    private lateinit var workModePreference: IntegerSimpleMenuPreference
    private lateinit var shellModePreference: IntegerSimpleMenuPreference
    private lateinit var userServiceInfoPreference: Preference
    private lateinit var hideUnavailableOptionsPreference: TwoStatePreference
    private lateinit var dynamicColorPreference: TwoStatePreference
    private lateinit var nightModePreference: IntegerSimpleMenuPreference
    private lateinit var languagePreference: ListPreference
    private lateinit var editTextPreference: EditTextPreference
    private lateinit var pinShortcutsPreference: Preference
    private lateinit var clearShortcutsPreference: Preference
    private lateinit var translationPreference: Preference
    private lateinit var developerPreference: Preference
    private lateinit var projectInfoPreference: Preference

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val context = requireContext()
        preferenceManager.setStorageDeviceProtected()
        preferenceManager.sharedPreferencesName = NyaSettings.NAME
        preferenceManager.sharedPreferencesMode = Context.MODE_PRIVATE
        setPreferencesFromResource(R.xml.preference_settings, null)

        workModePreference = findPreference("work_mode")!!
        shellModePreference = findPreference("shizuku_shell_mode")!!
        userServiceInfoPreference = findPreference("user_service_mode_info")!!
        hideUnavailableOptionsPreference = findPreference("hide_unavailable_options")!!
        dynamicColorPreference = findPreference("dynamic_color")!!
        nightModePreference = findPreference("night_mode")!!
        languagePreference = findPreference("language")!!
        editTextPreference = findPreference("edit_text")!!
        pinShortcutsPreference = findPreference("pin_shortcuts")!!
        clearShortcutsPreference = findPreference("clear_shortcuts")!!
        translationPreference = findPreference("translation")!!
        developerPreference = findPreference("developer")!!
        projectInfoPreference = findPreference("repo")!!

        val work = NyaSettings.getWorkMode()
        workModePreference.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _: Preference?, value: Any? ->
                if (value is Int && work != value)
                    activity?.recreate()
                true
            }

        if (work == NyaSettings.MODE.ROOT) {
            shellModePreference.isVisible = false
            userServiceInfoPreference.isVisible = false
            hideUnavailableOptionsPreference.isVisible = false
        }

        userServiceInfoPreference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            MaterialAlertDialogBuilder(context)
                .setTitle(R.string.about_user_service)
                .setMessage(R.string.about_user_service_content)
                .setPositiveButton(R.string.confirm, null)
                .show()
            true
        }

        nightModePreference.value = NyaSettings.getNightMode(context)
        nightModePreference.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _: Preference?, value: Any? ->
                if (value is Int && NyaSettings.getNightMode(context) != value) {
                    AppCompatDelegate.setDefaultNightMode(value)
                    activity?.recreate()
                }
                true
            }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            dynamicColorPreference.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _: Preference?, value: Any? ->
                    if (value is Boolean && NyaSettings.isUsingSystemColor() != value) {
                        activity?.recreate()
                    }
                    true
                }
        } else {
            dynamicColorPreference.isEnabled = false
        }

        languagePreference.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _: Preference?, newValue: Any ->
                if (newValue is String) {
                    val locale: Locale = if ("SYSTEM" == newValue)
                        LocaleDelegate.systemLocale
                    else
                        Locale.forLanguageTag(newValue)
                    LocaleDelegate.defaultLocale = locale
                    activity?.recreate()
                }
                true
            }
        setupLocalePreference()

        pinShortcutsPreference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            MaterialAlertDialogBuilder(context)
                .setTitle(R.string.request_pin_shortcuts)
                .setItems(
                    arrayOf(
                        getString(R.string.lock_screen),
                        getString(R.string.power_off),
                        getString(R.string.reboot)
                    )
                ) { dialogInterface, i ->
                    val shortcutHelper = ShortcutHelper(context)
                    shortcutHelper.requestPinShortcut(shortcutHelper.items[i])
                }
                .show()
            true
        }

        clearShortcutsPreference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            ShortcutManagerCompat.removeAllDynamicShortcuts(context)
            NyaSettings.preferences.edit { putBoolean("isShortcutCreated", false) }
            Toast.makeText(context, R.string.cleared, Toast.LENGTH_SHORT).show()
            true
        }

        translationPreference.summary =
            context.getString(
                R.string.translation_summary, context.getString(R.string.app_name)
            )
        translationPreference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            openUrlLink(context, "https://crowdin.com/project/rebootnya")
            true
        }

        developerPreference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            MaterialAlertDialogBuilder(context)
                .setTitle(R.string.contact_me)
                .setItems(
                    arrayOf(
                        getString(R.string.email),
                        getString(R.string.bilibili)
                    )
                ) { dialogInterface, i ->
                    when (i) {
                        0 -> sendEmail(context)
                        1 -> openUrlLink(context, "https://space.bilibili.com/178423358")
                    }
                }
                .show()
            true
        }

        projectInfoPreference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            openUrlLink(context, "https://github.com/daisukiKaffuChino/RebootNya")
            true
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val rootView = requireActivity().window.decorView
        rootView.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = Rect()
            rootView.getWindowVisibleDisplayFrame(rect)
            val screenHeight = rootView.rootView.height
            val keypadHeight = screenHeight - rect.bottom
            if (keypadHeight < screenHeight * 0.15)
                editTextPreference.getTextInputEditText()?.clearFocus()
        }
    }

    override fun onCreateRecyclerView(
        inflater: LayoutInflater,
        parent: ViewGroup,
        savedInstanceState: Bundle?
    ): RecyclerView {
        val recyclerView =
            super.onCreateRecyclerView(inflater, parent, savedInstanceState) as BorderRecyclerView
        recyclerView.fixEdgeEffect()
        recyclerView.addEdgeSpacing(bottom = 8f, unit = TypedValue.COMPLEX_UNIT_DIP)

        return recyclerView
    }

    private fun setupLocalePreference() {
        val localeTags = AppLocales.LOCALES
        val displayLocaleTags = AppLocales.DISPLAY_LOCALES

        languagePreference.entries = displayLocaleTags
        languagePreference.entryValues = localeTags

        val currentLocaleTag = languagePreference.value
        val currentLocaleIndex = localeTags.indexOf(currentLocaleTag)
        val currentLocale = NyaSettings.getLocale()
        val localizedLocales = mutableListOf<CharSequence>()

        for ((index, displayLocale) in displayLocaleTags.withIndex()) {
            if (index == 0) {
                localizedLocales.add(getString(R.string.follow_system))
                continue
            }

            val locale = Locale.forLanguageTag(displayLocale.toString())
            val localeName = if (!TextUtils.isEmpty(locale.script))
                locale.getDisplayScript(locale)
            else
                locale.getDisplayName(locale)

            val localizedLocaleName = if (!TextUtils.isEmpty(locale.script))
                locale.getDisplayScript(currentLocale)
            else
                locale.getDisplayName(currentLocale)

            localizedLocales.add(
                if (index != currentLocaleIndex)
                    "$localeName<br><small>$localizedLocaleName<small>".toHtml()
                else
                    localizedLocaleName
            )
        }

        languagePreference.entries = localizedLocales.toTypedArray()

        languagePreference.summary = when {
            TextUtils.isEmpty(currentLocaleTag) || "SYSTEM" == currentLocaleTag ->
                getString(R.string.follow_system)

            currentLocaleIndex != -1 -> {
                val localizedLocale = localizedLocales[currentLocaleIndex]
                val newLineIndex = localizedLocale.indexOf('\n')
                if (newLineIndex == -1)
                    localizedLocale.toString()
                else
                    localizedLocale.subSequence(0, newLineIndex).toString()
            }

            else -> "Error"
        }
    }

}