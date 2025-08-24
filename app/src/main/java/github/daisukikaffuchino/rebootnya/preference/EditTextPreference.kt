package github.daisukikaffuchino.rebootnya.preference

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.topjohnwu.superuser.Shell
import github.daisukikaffuchino.rebootnya.R
import github.daisukikaffuchino.rebootnya.utils.NyaSettings
import github.daisukikaffuchino.rebootnya.utils.RootUtil
import github.daisukikaffuchino.rebootnya.utils.ShizukuUtil

class EditTextPreference(private val context: Context, attrs: AttributeSet?) : Preference(
    context, attrs
) {
    init {
        layoutResource = R.layout.preference_edittext
    }

    private var savedText: String? = null
    private var editText: TextInputEditText? = null

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val inputLayout =
            holder.itemView.findViewById<TextInputLayout>(R.id.item_cmd_text_input_layout)
        editText =
            holder.itemView.findViewById(R.id.item_cmd_text_input_edit)

        inputLayout.setEndIconOnClickListener {
            val command = editText?.text.toString()
            if (command.isBlank()) {
                return@setEndIconOnClickListener
            }

            val shizukuUtil = ShizukuUtil(context)
            val rootUtil = RootUtil(context)
            val workingMode = NyaSettings.getWorkMode()

            when (workingMode) {
                NyaSettings.STORE.ROOT -> {
                    if (Shell.isAppGrantedRoot() != false) {
                        if (rootUtil.runRootCommandWithResult(command)) {
                            Toast.makeText(context, "Success!", Toast.LENGTH_SHORT).show()
                            editText?.text = null
                        } else {
                            Toast.makeText(context, R.string.exec_fail, Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, R.string.no_permission, Toast.LENGTH_SHORT).show()
                    }
                }

                NyaSettings.STORE.SHIZUKU -> {
                    if (shizukuUtil.checkShizukuPermission()) {
                        val exitCode = shizukuUtil.runShizukuCommand(
                            command.split("\\s+".toRegex()).toTypedArray(), false
                        )
                        if (exitCode == 0) {
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

        editText?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(editable: Editable?) {
                savedText = editText?.text.toString()
            }

            override fun beforeTextChanged(charSequence: CharSequence?, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence?, i: Int, i1: Int, i2: Int) {}
        })

        if (savedText != null)
            editText?.setText(savedText)

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

    fun getTextInputEditText(): TextInputEditText? {
        return editText
    }

}