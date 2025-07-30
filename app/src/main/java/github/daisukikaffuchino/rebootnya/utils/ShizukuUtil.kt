package github.daisukikaffuchino.rebootnya.utils

import android.content.pm.PackageManager
import android.os.IPowerManager
import android.os.RemoteException
import android.widget.Toast
import github.daisukikaffuchino.rebootnya.NyaApplication
import github.daisukikaffuchino.rebootnya.R
import github.daisukikaffuchino.rebootnya.shizuku.NyaRemoteProcess
import moe.shizuku.server.IShizukuService
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

class ShizukuUtil {

    fun checkShizukuPermission(): Boolean {
        if (!Shizuku.pingBinder()) return false

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

    fun shizukuReboot(confirm: Boolean, reason: String?) {
        try {
            val powerManager = IPowerManager.Stub.asInterface(
                ShizukuBinderWrapper(SystemServiceHelper.getSystemService("power"))
            )
            powerManager.reboot(confirm, reason, false)
        } catch (e: Exception) {
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
                Toast.makeText(NyaApplication.context, "Error:" + e.message, Toast.LENGTH_LONG).show()
            }
        } catch (e: java.lang.Exception) {
            e.fillInStackTrace()
            Toast.makeText(NyaApplication.context, "Error:" + e.message, Toast.LENGTH_LONG).show()
        }
        return 100
    }

}