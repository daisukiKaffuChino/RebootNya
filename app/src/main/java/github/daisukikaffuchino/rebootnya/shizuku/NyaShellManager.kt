package github.daisukikaffuchino.rebootnya.shizuku

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import github.daisukikaffuchino.rebootnya.IShellService
import github.daisukikaffuchino.rebootnya.utils.ShizukuUtil
import rikka.shizuku.Shizuku
import rikka.shizuku.Shizuku.UserServiceArgs


object NyaShellManager {
    const val TAG: String = "NyaShellManager"
    var mService: IShellService? = null
    private lateinit var userServiceConnection: ServiceConnection

    fun interface ControlCallback {
        fun onResult(exitCode: Int, message: String?)
    }

    private val userServiceArgs: UserServiceArgs = UserServiceArgs(
        ComponentName(
            "github.daisukikaffuchino.rebootnya",
            NyaShellService::class.java.name
        )
    )
        .daemon(false)
        .processNameSuffix("service")
        .debuggable(false)
        .version(1)


    fun bindService(shizukuUtil: ShizukuUtil, callback: ControlCallback) {
        if (!shizukuUtil.checkShizukuPermission() || mService != null) return
        userServiceConnection = object : ServiceConnection {
            override fun onServiceConnected(componentName: ComponentName, binder: IBinder?) {
                callback.onResult(0, null)

                val res = StringBuilder()
                res.append("onServiceConnected: ").append(componentName.className).append('\n')

                if (binder != null && binder.pingBinder())
                    mService = IShellService.Stub.asInterface(binder)
                else
                    res.append("invalid binder for ").append(componentName).append(" received")

                Log.i(TAG, res.toString().trim())

            }

            override fun onServiceDisconnected(componentName: ComponentName) {
                Log.i(TAG, "onServiceDisconnected: \n" + componentName.className)
            }
        }
        Shizuku.bindUserService(userServiceArgs, userServiceConnection)
    }

    fun executeCommand(cmd: String?, callback: ControlCallback) {
//        val serviceVersion = Shizuku.peekUserService(userServiceArgs, userServiceConnection)
//        Log.i(TAG, "V$serviceVersion")
        mService?.let { service ->
            try {
                val exitCode = service.exec(cmd)
                callback.onResult(exitCode, if (exitCode == 0) "Success" else "Fail: $exitCode")
            } catch (e: RemoteException) {
                callback.onResult(-1, e.message.toString())
            }
        } ?: run {
            callback.onResult(-1, "waiting?")//没准备完毕
        }
    }

    fun unbindService() {
        if (Shizuku.pingBinder() && ::userServiceConnection.isInitialized && mService != null) {
            Shizuku.unbindUserService(userServiceArgs, userServiceConnection, true)
            mService = null
        }
    }
}