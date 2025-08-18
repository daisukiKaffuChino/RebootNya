package github.daisukikaffuchino.rebootnya.shizuku

import android.content.Context
import android.os.RemoteException
import android.util.Log
import androidx.annotation.Keep
import github.daisukikaffuchino.rebootnya.IShellService


class NyaShellService : IShellService.Stub {
    constructor() {
        Log.i("UserService", "constructor")
    }

    @Keep
    constructor(context: Context) {
        Log.i("UserService", "constructor with Context: context=$context")
    }

    @Throws(RemoteException::class)
    override fun exec(cmd: String): Int {
        try {
            val process = Runtime.getRuntime().exec(cmd)
            return process.waitFor()
        } catch (e: Exception) {
            e.fillInStackTrace()
            return -1
        }
    }
}