package github.daisukikaffuchino.rebootnya.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.IPowerManager
import android.os.RemoteException
import android.util.Log
import android.widget.Toast
import github.daisukikaffuchino.rebootnya.R
import github.daisukikaffuchino.rebootnya.shizuku.NyaRemoteProcess
import github.daisukikaffuchino.rebootnya.shizuku.NyaShellManager
import moe.shizuku.server.IShizukuService
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

class ShizukuUtil(private val context: Context) {
    fun checkShizukuPermission(): Boolean {
        if (!Shizuku.pingBinder()) return false

        if (Shizuku.isPreV11()) {
            Toast.makeText(context, R.string.shizuku_too_old, Toast.LENGTH_SHORT)
                .show()
            return false
        }

        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            return true
        } else if (Shizuku.shouldShowRequestPermissionRationale()) {
            Toast.makeText(context, R.string.shizuku_denied, Toast.LENGTH_SHORT)
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
            Log.e("ShizukuUtil","reboot", e)
            Toast.makeText(context, "Error:" + e.message, Toast.LENGTH_LONG).show()
        }
    }

    private fun shizukuProcess(cmd: Array<String?>?): Int {
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
                Toast.makeText(context, "Error:" + e.message, Toast.LENGTH_LONG)
                    .show()
            }
        } catch (e: Exception) {
            e.fillInStackTrace()
            Toast.makeText(context, "Error:" + e.message, Toast.LENGTH_LONG).show()
        }
        return -3
    }

    fun runShizukuCommand(cmd: Array<String?>, checkUid: Boolean): Int {
        val cmdString = cmd.joinToString(" ")
        if (checkUid && Shizuku.getUid() == 2000) Toast.makeText(
            context,
            R.string.shizuku_permission_insufficient,
            Toast.LENGTH_SHORT
        ).show()

        val mode = NyaSettings.getShizukuShellMode()
        when (mode) {
            NyaSettings.MODE.PROCESS -> {
                val exitCode = shizukuProcess(cmd)
                if (exitCode != 0) Toast.makeText(
                    context,
                    R.string.exec_fail,
                    Toast.LENGTH_SHORT
                )
                    .show()
                return exitCode
            }

            else -> {
                if (NyaShellManager.mService == null) {
                    Toast.makeText(
                        context,
                        R.string.user_service_not_initialized,
                        Toast.LENGTH_SHORT
                    ).show()
                    return -2
                }
                return executeCommandSync(cmdString)
            }
        }

    }

    private fun executeCommandSync(cmdString: String): Int {
        val latch = CountDownLatch(1)
        val result = AtomicInteger(-1)

        NyaShellManager.executeCommand(cmdString) { exitCode, message ->
            if (exitCode != 0) Toast.makeText(
                context,
                "Message: $message",
                Toast.LENGTH_SHORT
            )
                .show()
            result.set(exitCode)
            latch.countDown() //完成
        }

        try {
            latch.await() //阻塞
            return result.get()
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            Log.e("ShizukuUtil","interruptExec", e)
            return -2
        }
    }
}




