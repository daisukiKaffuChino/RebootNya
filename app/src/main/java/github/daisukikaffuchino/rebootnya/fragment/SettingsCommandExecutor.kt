package github.daisukikaffuchino.rebootnya.fragment

import android.widget.Toast
import androidx.fragment.app.Fragment
import com.topjohnwu.superuser.Shell
import github.daisukikaffuchino.rebootnya.R
import github.daisukikaffuchino.rebootnya.utils.NyaSettings
import github.daisukikaffuchino.rebootnya.utils.RootUtil
import github.daisukikaffuchino.rebootnya.utils.ShizukuUtil

class SettingsCommandExecutor(
    private val fragment: Fragment,
    private val onCommandSucceeded: () -> Unit
) {
    fun execute(command: String) {
        val context = fragment.requireContext()
        val shizukuUtil = ShizukuUtil(context)
        val rootUtil = RootUtil(context)

        when (NyaSettings.getWorkMode()) {
            NyaSettings.MODE.ROOT -> {
                if (Shell.isAppGrantedRoot() != false) {
                    if (rootUtil.runRootCommandWithResult(command)) {
                        Toast.makeText(context, "Success!", Toast.LENGTH_SHORT).show()
                        onCommandSucceeded()
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
                        command.split("\\s+".toRegex()).toTypedArray(),
                        false
                    )
                    if (exitCode == 0) {
                        Toast.makeText(context, "Success!\nExit code: 0", Toast.LENGTH_SHORT)
                            .show()
                        onCommandSucceeded()
                    } else {
                        val errorMsg =
                            "${fragment.getString(R.string.exec_fail)}\nExit code: $exitCode"
                        Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, R.string.no_permission, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
