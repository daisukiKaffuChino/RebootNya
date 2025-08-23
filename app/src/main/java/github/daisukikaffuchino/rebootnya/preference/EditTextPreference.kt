package github.daisukikaffuchino.rebootnya.preference

import android.content.Context
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

    }

    fun getTextInputEditText(): TextInputEditText? {
        return editText
    }

}