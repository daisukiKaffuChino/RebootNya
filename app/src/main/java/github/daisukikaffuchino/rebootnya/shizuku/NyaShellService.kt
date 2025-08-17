package github.daisukikaffuchino.rebootnya.shizuku

import android.os.RemoteException
import github.daisukikaffuchino.rebootnya.IShellService
import java.io.BufferedReader
import java.io.InputStreamReader

class NyaShellService : IShellService.Stub() {
    override fun exec(cmd: String): ShellResult {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))

            val output = StringBuilder()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            while (errorReader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }

            val exitCode = process.waitFor()
            ShellResult(exitCode, output.toString())
        } catch (e: Exception) {
            throw RemoteException("Exec failed: ${e.message}")
        }
    }
}