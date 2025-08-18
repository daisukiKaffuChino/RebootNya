package github.daisukikaffuchino.rebootnya.utils

import android.content.pm.PackageManager
import android.os.Handler
import android.os.IPowerManager
import android.os.Looper
import android.os.RemoteException
import android.widget.Toast
import github.daisukikaffuchino.rebootnya.NyaApplication
import github.daisukikaffuchino.rebootnya.R
import github.daisukikaffuchino.rebootnya.fragment.HomeFragment.userServiceStatus
import github.daisukikaffuchino.rebootnya.shizuku.NyaRemoteProcess
import github.daisukikaffuchino.rebootnya.shizuku.NyaShellManager.bindService
import github.daisukikaffuchino.rebootnya.shizuku.NyaShellManager.executeCommand
import moe.shizuku.server.IShizukuService
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import java.util.concurrent.atomic.AtomicInteger

class ShizukuUtil {

    fun checkShizukuPermission(): Boolean {
        if (!Shizuku.pingBinder()) {
            Toast.makeText(NyaApplication.context, R.string.shizuku_not_run, Toast.LENGTH_SHORT)
                .show()
            return false
        }

        if (Shizuku.isPreV11()) {
            Toast.makeText(NyaApplication.context, R.string.shizuku_too_old, Toast.LENGTH_SHORT)
                .show()
            return false
        }

        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            return true
        } else if (Shizuku.shouldShowRequestPermissionRationale()) {
            Toast.makeText(NyaApplication.context, R.string.shizuku_denied, Toast.LENGTH_SHORT)
                .show()
            return false
        } else {
            return false
        }
    }

    fun shizukuReboot(reason: String?) {
        val powerManager = IPowerManager.Stub.asInterface(
            ShizukuBinderWrapper(SystemServiceHelper.getSystemService("power"))
        )
        try {
            powerManager.reboot(false, reason, false)
        } catch (e: Exception) {
            if (e.message?.contains("lock must not be null") == true && reason == null) {
                try {
                    powerManager.reboot(false, "nya", false)
                } catch (e: Exception) {
                    e.fillInStackTrace()
                    Toast.makeText(NyaApplication.context, "Error:" + e.message, Toast.LENGTH_LONG)
                        .show()
                }
                return
            }
            e.fillInStackTrace()
            Toast.makeText(NyaApplication.context, "Error:" + e.message, Toast.LENGTH_LONG).show()
        }
    }

    fun shizukuProcess(cmd: Array<String?>?): Int {
        try {
            val privateField = Shizuku::class.java.getDeclaredField("service")
            privateField.isAccessible = true
            val value = privateField.get(null) as IShizukuService
            try {
                checkNotNull(value)
                val process: Process = NyaRemoteProcess(value.newProcess(cmd, null, null))
                return process.waitFor()
            } catch (e: RemoteException) {
                e.fillInStackTrace()
                Toast.makeText(NyaApplication.context, "Error:" + e.message, Toast.LENGTH_LONG)
                    .show()
            }
        } catch (e: Exception) {
            e.fillInStackTrace()
            Toast.makeText(NyaApplication.context, "Error:" + e.message, Toast.LENGTH_LONG).show()
        }
        return 100
    }

    fun runShizukuCommand(cmd: Array<String?>, checkUid: Boolean): Int {
        val cmdString = cmd.joinToString(" ")
        if (checkUid && Shizuku.getUid() == 2000) Toast.makeText(
            NyaApplication.context,
            R.string.shizuku_permission_insufficient,
            Toast.LENGTH_SHORT
        ).show()
        if (NyaApplication.sp.getString("shizuku_shell_exec_mode", "Process") == "Process") {
            val exitCode = shizukuProcess(cmd)
            if (exitCode != 0) Toast.makeText(NyaApplication.context, R.string.exec_fail, Toast.LENGTH_SHORT)
                .show()
            return exitCode
        } else {
            val exitCode2 = AtomicInteger()
            if (userServiceStatus != 0) bindService({ exitCode: Int, message: String? ->
                userServiceStatus = exitCode
                val handler = Handler(Looper.getMainLooper())
                handler.postDelayed({
                    executeCommand(cmdString,  { exitCode1: Int, message1: String? ->
                        exitCode2.set(exitCode1)
                        if (exitCode != 0) Toast.makeText(NyaApplication.context, message1, Toast.LENGTH_SHORT)
                            .show()
                    })
                }, 100)
            })
            else executeCommand(cmdString,  { exitCode: Int, message: String? ->
                exitCode2.set(exitCode)
                if (exitCode != 0) Toast.makeText(NyaApplication.context, message, Toast.LENGTH_SHORT).show()
            })
            //Log.d("exit", exitCode2.get().toString()+" kt\n"+cmdString)
            return exitCode2.get()
        }
    }

}