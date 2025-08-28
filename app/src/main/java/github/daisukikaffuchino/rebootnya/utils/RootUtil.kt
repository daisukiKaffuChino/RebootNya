package github.daisukikaffuchino.rebootnya.utils

import android.content.Context
import android.widget.Toast
import com.topjohnwu.superuser.Shell
import github.daisukikaffuchino.rebootnya.R
import java.lang.Boolean
import kotlin.Exception
import kotlin.String

class RootUtil(private val context: Context){
    fun runRootCommandWithResult(cmd: String): kotlin.Boolean {
        if (Boolean.FALSE == Shell.isAppGrantedRoot()) {
            Toast.makeText(context, R.string.no_root, Toast.LENGTH_SHORT).show()
            return false
        } else {
            val result = Shell.cmd(cmd).exec()
            return result.isSuccess
        }
    }

    fun requestRoot() {
        Toast.makeText(context, R.string.ksu_tip, Toast.LENGTH_SHORT).show()
        var process: Process? = null
        try {
            process = Runtime.getRuntime().exec("su")
        } catch (e: Exception) {
            e.fillInStackTrace()
        } finally {
            process?.destroy()
        }
    }
}
