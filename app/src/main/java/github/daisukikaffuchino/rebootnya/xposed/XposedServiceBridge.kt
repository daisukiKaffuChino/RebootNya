package github.daisukikaffuchino.rebootnya.xposed

import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper

object XposedServiceBridge {
    @Volatile
    private var initialized = false

    @Volatile
    private var service: XposedService? = null

    private val listener = object : XposedServiceHelper.OnServiceListener {
        override fun onServiceBind(service: XposedService) {
            this@XposedServiceBridge.service = service
            syncLocalConfigToRemote()
        }

        override fun onServiceDied(service: XposedService) {
            if (this@XposedServiceBridge.service === service) {
                this@XposedServiceBridge.service = null
            }
        }
    }

    fun initialize() {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            try {
                XposedServiceHelper.registerListener(listener)
            } catch (_: Throwable) {
                service = null
            }
            initialized = true
        }
    }

    fun isConnected(): Boolean = service != null

    fun syncLocalConfigToRemote(): Boolean {
        val xposedService = service ?: return false
        return try {
            val remotePrefs = xposedService.getRemotePreferences(XposedConfig.REMOTE_GROUP)
            XposedConfig.write(remotePrefs, XposedConfig.readLocal())
            true
        } catch (_: Throwable) {
            false
        }
    }
}
