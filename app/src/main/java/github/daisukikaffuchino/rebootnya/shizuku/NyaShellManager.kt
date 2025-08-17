package github.daisukikaffuchino.rebootnya.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import github.daisukikaffuchino.rebootnya.IShellService
import rikka.shizuku.Shizuku
import rikka.shizuku.Shizuku.UserServiceArgs

object NyaShellManager {

    private var service: IShellService? = null

    private val conn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            Log.d("xxxxxx",service.toString())
            service = IShellService.Stub.asInterface(binder)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
        }
    }

    fun bind(context: Context) {
        val args = UserServiceArgs(
            ComponentName(context.packageName, NyaShellService::class.java.name)
        ).daemon(false).processNameSuffix("shell").debuggable(true)

        Shizuku.bindUserService(args, conn)
    }

    fun exec(cmd: String): ShellResult? {
        Log.d("xxx",service.toString())
        val s = service ?: return null
        return s.exec(cmd)
    }
}
