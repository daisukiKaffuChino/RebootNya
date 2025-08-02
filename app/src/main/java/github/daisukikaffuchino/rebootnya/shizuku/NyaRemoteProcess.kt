package github.daisukikaffuchino.rebootnya.shizuku

import android.annotation.SuppressLint
import android.os.Parcel
import android.os.ParcelFileDescriptor
import android.os.Parcelable
import android.os.RemoteException
import android.util.ArraySet
import android.util.Log
import moe.shizuku.server.IRemoteProcess
import java.io.InputStream
import java.io.OutputStream
import java.util.Collections

@SuppressLint("ParcelCreator")
class NyaRemoteProcess : Process, Parcelable {
    private var remote: IRemoteProcess?
    private var os: OutputStream? = null
    private var `is`: InputStream? = null

    constructor(remote: IRemoteProcess?) {
        this.remote = remote
        try {
            this.remote!!.asBinder().linkToDeath({
                this.remote = null
                Log.v(TAG, "remote process is dead")
                CACHE.remove(this)
            }, 0)
        } catch (e: RemoteException) {
            Log.e(TAG, "linkToDeath", e)
        }

        // The reference to the binder object must be hold
        CACHE.add(this)
    }

    override fun getOutputStream(): OutputStream {
        if (os == null) {
            try {
                os = ParcelFileDescriptor.AutoCloseOutputStream(remote!!.outputStream)
            } catch (e: RemoteException) {
                throw RuntimeException(e)
            }
        }
        return os!!
    }

    override fun getInputStream(): InputStream {
        if (`is` == null) {
            try {
                `is` = ParcelFileDescriptor.AutoCloseInputStream(remote!!.inputStream)
            } catch (e: RemoteException) {
                throw RuntimeException(e)
            }
        }
        return `is`!!
    }

    override fun getErrorStream(): InputStream {
        try {
            return ParcelFileDescriptor.AutoCloseInputStream(remote!!.errorStream)
        } catch (e: RemoteException) {
            throw RuntimeException(e)
        }
    }

    @Throws(InterruptedException::class)
    override fun waitFor(): Int {
        try {
            return remote!!.waitFor()
        } catch (e: RemoteException) {
            throw RuntimeException(e)
        }
    }

    override fun exitValue(): Int {
        try {
            return remote!!.exitValue()
        } catch (e: RemoteException) {
            throw RuntimeException(e)
        }
    }

    override fun destroy() {
        try {
            remote!!.destroy()
        } catch (e: RemoteException) {
            throw RuntimeException(e)
        }
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeStrongBinder(remote!!.asBinder())
    }

    companion object {
        private val CACHE: MutableSet<NyaRemoteProcess?> =
            Collections.synchronizedSet<NyaRemoteProcess?>(
                ArraySet<NyaRemoteProcess?>()
            )

        private const val TAG = "NyaRemoteProcess"

    }
}