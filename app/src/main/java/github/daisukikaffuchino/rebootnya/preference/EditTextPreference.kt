package github.daisukikaffuchino.rebootnya.preference

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import com.topjohnwu.superuser.Shell
import github.daisukikaffuchino.rebootnya.R
import github.daisukikaffuchino.rebootnya.utils.NyaSettings
import github.daisukikaffuchino.rebootnya.utils.RootUtil
import github.daisukikaffuchino.rebootnya.utils.ShizukuUtil
import androidx.core.content.edit

class EditTextPreference(private val context: Context, attrs: AttributeSet?) : Preference(
    context, attrs
) {
    companion object {
        private const val HISTORY_KEY = "custom_shell_history"
        private const val HISTORY_DELIMITER = "\n"
        private const val HISTORY_LIMIT = 3
    }

    init {
        layoutResource = R.layout.preference_edit_text
    }

    private var savedText: String? = null
    private var editText: MaterialAutoCompleteTextView? = null
    private var textWatcher: TextWatcher? = null
    private var historyAdapter: ArrayAdapter<String>? = null

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val inputLayout =
            holder.itemView.findViewById<TextInputLayout>(R.id.item_cmd_text_input_layout)
        editText =
            holder.itemView.findViewById(R.id.item_cmd_text_input_edit)

        setupHistoryDropdown()

        inputLayout.setEndIconOnClickListener {
            val command = editText?.text.toString().trim()
            if (command.isBlank()) {
                return@setEndIconOnClickListener
            }

            val shizukuUtil = ShizukuUtil(context)
            val rootUtil = RootUtil(context)
            val workingMode = NyaSettings.getWorkMode()

            when (workingMode) {
                NyaSettings.MODE.ROOT -> {
                    if (Shell.isAppGrantedRoot() != false) {
                        if (rootUtil.runRootCommandWithResult(command)) {
                            saveCommandToHistory(command)
                            Toast.makeText(context, "Success!", Toast.LENGTH_SHORT).show()
                            editText?.text = null
                        } else {
                            Toast.makeText(context, R.string.exec_fail, Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, R.string.no_permission, Toast.LENGTH_SHORT).show()
                    }
                }

                NyaSettings.MODE.SHIZUKU -> {
                    if (shizukuUtil.checkShizukuPermission()) {
                        val exitCode = shizukuUtil.runShizukuCommand(
                            command.split("\\s+".toRegex()).toTypedArray(), false
                        )
                        if (exitCode == 0) {
                            saveCommandToHistory(command)
                            Toast.makeText(context, "Success!\nExit code: 0", Toast.LENGTH_SHORT)
                                .show()
                            editText?.text = null
                        } else {
                            val errorMsg =
                                "${context.getString(R.string.exec_fail)}\nExit code: $exitCode"
                            Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, R.string.no_permission, Toast.LENGTH_SHORT).show()
                    }
                }

            }
        }

        inputLayout.setEndIconOnLongClickListener {
            editText?.setText(null)
            true
        }

        textWatcher?.let { editText?.removeTextChangedListener(it) }
        textWatcher = object : TextWatcher {
            override fun afterTextChanged(editable: Editable?) {
                savedText = editText?.text.toString()
            }

            override fun beforeTextChanged(charSequence: CharSequence?, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence?, i: Int, i1: Int, i2: Int) {}
        }
        editText?.addTextChangedListener(textWatcher)

        if (savedText != null)
            editText?.setText(savedText)

    }

    private fun setupHistoryDropdown() {
        val history = loadCommandHistory()
        historyAdapter = ArrayAdapter(
            context,
            android.R.layout.simple_dropdown_item_1line,
            history.toMutableList()
        )
        editText?.apply {
            setAdapter(historyAdapter)
            threshold = 0
            setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus && (historyAdapter?.count ?: 0) > 0) {
                    post { showDropDown() }
                }
            }
            setOnClickListener {
                if ((historyAdapter?.count ?: 0) > 0) {
                    showDropDown()
                }
            }
            setOnItemClickListener { _, _, _, _ ->
                setSelection(text?.length ?: 0)
            }
        }
    }

    private fun loadCommandHistory(): List<String> {
        val rawValue = NyaSettings.preferences.getString(HISTORY_KEY, null).orEmpty()
        if (rawValue.isBlank()) {
            return emptyList()
        }
        return rawValue.split(HISTORY_DELIMITER)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .take(HISTORY_LIMIT)
    }

    private fun saveCommandToHistory(command: String) {
        val updatedHistory = buildList {
            add(command)
            addAll(loadCommandHistory().filter { it != command })
        }.take(HISTORY_LIMIT)

        NyaSettings.preferences.edit {
            putString(HISTORY_KEY, updatedHistory.joinToString(HISTORY_DELIMITER))
        }
        updateHistoryAdapter(updatedHistory)
    }

    private fun updateHistoryAdapter(history: List<String>) {
        historyAdapter?.apply {
            clear()
            addAll(history)
            notifyDataSetChanged()
        }
        editText?.apply {
            dismissDropDown()
            val hasHistory = (historyAdapter?.count ?: 0) > 0
            if (!hasHistory) {
                setAdapter(null)
            } else {
                setAdapter(historyAdapter)
                threshold = 0
                if (hasFocus()) {
                    post { showDropDown() }
                }
            }
        }
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val myState = SavedState(superState)
        myState.customValue = savedText
        return myState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state !is SavedState) {
            super.onRestoreInstanceState(state)
            return
        }
        super.onRestoreInstanceState(state.superState)
        savedText = state.customValue
    }

    private class SavedState : BaseSavedState {
        var customValue: String? = null

        constructor(superState: Parcelable?) : super(superState)

        private constructor(source: Parcel) : super(source) {
            customValue = source.readString()
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            super.writeToParcel(dest, flags)
            dest.writeString(customValue)
        }

        companion object CREATOR : Parcelable.Creator<SavedState> {
            override fun createFromParcel(source: Parcel): SavedState = SavedState(source)
            override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
        }
    }

    fun getTextInputEditText(): MaterialAutoCompleteTextView? {
        return editText
    }

}
