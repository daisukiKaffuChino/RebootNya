package github.daisukikaffuchino.rebootnya.shizuku

import android.content.Context
import android.os.RemoteException
import android.util.Log
import androidx.annotation.Keep
import github.daisukikaffuchino.rebootnya.IShellService
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException


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
        //var process: Process? = null
        return executeCommandWithTimeout(cmd, 3000)
    }

    fun executeCommandWithTimeout(cmd: String, timeoutMs: Long): Int {
        val executor = Executors.newSingleThreadExecutor()
        val future = executor.submit(Callable {
            Runtime.getRuntime().exec(cmd).waitFor()
        })

        return try {
            future.get(timeoutMs, TimeUnit.MILLISECONDS)
        } catch (e: TimeoutException) {
            Log.d("ShellService", "Timeout:\n" + e.message.toString())
            future.cancel(true)
            -2 //超时
        } catch (e: Exception) {
            Log.d("ShellService", "Error:\n" + e.message.toString())
            -1
        } finally {
            executor.shutdown()
        }
    }

}