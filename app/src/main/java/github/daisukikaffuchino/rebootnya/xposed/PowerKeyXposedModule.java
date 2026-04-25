package github.daisukikaffuchino.rebootnya.xposed;

import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.os.UserHandle;
import android.util.Log;

import androidx.annotation.NonNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;
import io.github.libxposed.api.XposedInterface;

public final class PowerKeyXposedModule extends XposedModule {

    private static final String TAG = "RebootNyaXposed";

    private static final String TARGET_PACKAGE = "github.daisukikaffuchino.rebootnya";
    private static final String TARGET_ACTIVITY = "github.daisukikaffuchino.rebootnya.MainActivity";
    private static final String TARGET_ACTION = "github.daisukikaffuchino.rebootnya.action.LAUNCH";

    private static final String CLASS_PHONE_WINDOW_MANAGER =
            "com.android.server.policy.PhoneWindowManager";
    private static final String CLASS_POWER_KEY_RULE =
            "com.android.server.policy.PhoneWindowManager$PowerKeyRule";

    @Override
    public void onModuleLoaded(@NonNull XposedModuleInterface.ModuleLoadedParam param) {
        log(Log.INFO, TAG, "Module loaded in process: " + param.getProcessName()
                + ", systemServer=" + param.isSystemServer());
    }

    @Override
    public void onSystemServerStarting(@NonNull XposedModuleInterface.SystemServerStartingParam param) {
        log(Log.INFO, TAG, "System server starting, installing power key hooks");
        final ClassLoader classLoader = param.getClassLoader();
        hookLongPress(classLoader);
        hookVeryLongPress(classLoader);
    }

    private void hookLongPress(@NonNull ClassLoader classLoader) {
        final Class<?> pwmClass = findClass(CLASS_PHONE_WINDOW_MANAGER, classLoader);
        if (pwmClass == null) {
            log(Log.WARN, TAG, CLASS_PHONE_WINDOW_MANAGER + " not found");
            return;
        }

        final Method longPressMethod = findMethod(pwmClass, "powerLongPress", 1);
        if (longPressMethod != null) {
            installIntercept(longPressMethod, "powerLongPress");
            tryDeoptimize(longPressMethod, "powerLongPress");
            log(Log.INFO, TAG, "Hooked " + longPressMethod.getName());
        }

        final Class<?> powerRuleClass = findClass(CLASS_POWER_KEY_RULE, classLoader);
        final Method onLongPress = findMethod(powerRuleClass, "onLongPress", 1);
        if (onLongPress != null) {
            installIntercept(onLongPress, "PowerKeyRule.onLongPress");
            tryDeoptimize(onLongPress, "onLongPress");
            log(Log.INFO, TAG, "Hooked fallback " + onLongPress.getName());
        }

        if (longPressMethod == null && onLongPress == null) {
            log(Log.WARN, TAG, "No long press hook point found");
        }
    }

    private void hookVeryLongPress(@NonNull ClassLoader classLoader) {
        final Class<?> pwmClass = findClass(CLASS_PHONE_WINDOW_MANAGER, classLoader);
        if (pwmClass == null) {
            return;
        }

        Method veryLongPressMethod = findMethod(pwmClass, "powerVeryLongPress", 0);
        if (veryLongPressMethod != null) {
            installVeryLongPressIntercept(veryLongPressMethod, "powerVeryLongPress");
            tryDeoptimize(veryLongPressMethod, "powerVeryLongPress");
            log(Log.INFO, TAG, "Hooked " + veryLongPressMethod.getName());
        }

        final Class<?> powerRuleClass = findClass(CLASS_POWER_KEY_RULE, classLoader);
        final Method onVeryLongPress = findMethod(powerRuleClass, "onVeryLongPress", 1);
        if (onVeryLongPress != null) {
            installVeryLongPressIntercept(onVeryLongPress, "PowerKeyRule.onVeryLongPress");
            tryDeoptimize(onVeryLongPress, "onVeryLongPress");
            log(Log.INFO, TAG, "Hooked fallback " + onVeryLongPress.getName());
        }

        if (veryLongPressMethod == null && onVeryLongPress == null) {
            log(Log.WARN, TAG, "No very long press hook point found");
        }
    }

    private void installIntercept(@NonNull Method method, @NonNull String label) {
        hook(method)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept(new XposedInterface.Hooker() {
                    @Override
                    public Object intercept(@NonNull XposedInterface.Chain chain) throws Throwable {
                        final Object policyOwner = getPolicyOwner(chain.getThisObject());
                        log(Log.DEBUG, TAG, "Long press hook hit: " + label);
                        if (!handlePowerLongPress(policyOwner)) {
                            return chain.proceed();
                        }
                        markPowerKeyHandled(policyOwner);
                        return null;
                    }
                });
    }

    private void installVeryLongPressIntercept(@NonNull Method method, @NonNull String label) {
        hook(method)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept(new XposedInterface.Hooker() {
                    @Override
                    public Object intercept(@NonNull XposedInterface.Chain chain) throws Throwable {
                        final Config config = readConfig();
                        log(Log.DEBUG, TAG, "Very long press hook hit: "
                                + label
                                + ", keepVeryLongPress=" + config.keepVeryLongPress);
                        if (!config.enabled || config.keepVeryLongPress) {
                            return chain.proceed();
                        }
                        markPowerKeyHandled(getPolicyOwner(chain.getThisObject()));
                        return null;
                    }
                });
    }

    private boolean handlePowerLongPress(Object policyOwner) {
        final Config config = readConfig();
        if (!config.enabled) {
            log(Log.INFO, TAG, "Power key hook is active, but feature is disabled in config");
            return false;
        }

        final Context context = getPolicyContext(policyOwner);
        if (context == null) {
            return false;
        }

        if (isKeyguardLocked(context) && config.fallbackWhenLocked) {
            log(Log.DEBUG, TAG, "Fallback to original behavior because device is locked");
            return false;
        }

        if (!isTargetInstalled(context)) {
            log(Log.WARN, TAG, "Target app is not installed");
            return !config.fallbackWhenAppMissing;
        }

        final boolean launched = launchTargetActivity(context);
        if (!launched) {
            log(Log.ERROR, TAG, "Failed to launch target activity");
            return !config.fallbackWhenStartFailed;
        }
        log(Log.INFO, TAG, "Launched target activity from power key long press");
        return true;
    }

    private Config readConfig() {
        final SharedPreferences preferences = getRemotePreferencesSafely();
        if (preferences == null) {
            return Config.defaults();
        }

        return new Config(
                preferences.getBoolean(XposedConfig.KEY_ENABLED, XposedConfig.DEFAULT_ENABLED),
                preferences.getBoolean(
                        XposedConfig.KEY_FALLBACK_WHEN_APP_MISSING,
                        XposedConfig.DEFAULT_FALLBACK_WHEN_APP_MISSING
                ),
                preferences.getBoolean(
                        XposedConfig.KEY_FALLBACK_WHEN_START_FAILED,
                        XposedConfig.DEFAULT_FALLBACK_WHEN_START_FAILED
                ),
                preferences.getBoolean(
                        XposedConfig.KEY_FALLBACK_WHEN_LOCKED,
                        XposedConfig.DEFAULT_FALLBACK_WHEN_LOCKED
                ),
                preferences.getBoolean(
                        XposedConfig.KEY_KEEP_VERY_LONG_PRESS,
                        XposedConfig.DEFAULT_KEEP_VERY_LONG_PRESS
                )
        );
    }

    private SharedPreferences getRemotePreferencesSafely() {
        try {
            return getRemotePreferences(XposedConfig.REMOTE_GROUP);
        } catch (Throwable throwable) {
            return null;
        }
    }

    private boolean isTargetInstalled(@NonNull Context context) {
        try {
            context.getPackageManager().getApplicationInfo(TARGET_PACKAGE, 0);
            return true;
        } catch (PackageManager.NameNotFoundException ignored) {
            return false;
        }
    }

    private boolean launchTargetActivity(@NonNull Context context) {
        final Intent intent = new Intent(TARGET_ACTION)
                .setComponent(new ComponentName(TARGET_PACKAGE, TARGET_ACTIVITY))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        try {
            if (startAsCurrentUser(context, intent)) {
                return true;
            }
            context.startActivity(intent);
            return true;
        } catch (Throwable throwable) {
            log(Log.ERROR, TAG, "Failed to start target activity", throwable);
            return false;
        }
    }

    private boolean startAsCurrentUser(@NonNull Context context, @NonNull Intent intent) {
        try {
            final Method getCurrentUser = ActivityManager.class.getDeclaredMethod("getCurrentUser");
            getCurrentUser.setAccessible(true);
            final int userId = (Integer) getCurrentUser.invoke(null);
            final Method ofMethod = UserHandle.class.getDeclaredMethod("of", int.class);
            ofMethod.setAccessible(true);
            final UserHandle userHandle = (UserHandle) ofMethod.invoke(null, userId);

            final Method startActivityAsUser = context.getClass()
                    .getMethod("startActivityAsUser", Intent.class, UserHandle.class);
            startActivityAsUser.setAccessible(true);
            startActivityAsUser.invoke(context, intent, userHandle);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private boolean isKeyguardLocked(@NonNull Context context) {
        final KeyguardManager keyguardManager = context.getSystemService(KeyguardManager.class);
        return keyguardManager != null && keyguardManager.isKeyguardLocked();
    }

    private void markPowerKeyHandled(Object policyOwner) {
        if (policyOwner == null) {
            return;
        }
        try {
            final Field field = findField(policyOwner.getClass(), "mPowerKeyHandled");
            if (field != null) {
                field.setAccessible(true);
                field.setBoolean(policyOwner, true);
            }
        } catch (Throwable throwable) {
            log(Log.ERROR, TAG, "Failed to set mPowerKeyHandled", throwable);
        }
    }

    private Context getPolicyContext(Object policyOwner) {
        if (policyOwner == null) {
            return null;
        }
        try {
            final Field contextField = findField(policyOwner.getClass(), "mContext");
            if (contextField == null) {
                return null;
            }
            contextField.setAccessible(true);
            final Object value = contextField.get(policyOwner);
            return value instanceof Context ? (Context) value : null;
        } catch (Throwable throwable) {
            log(Log.ERROR, TAG, "Failed to get policy context", throwable);
            return null;
        }
    }

    private Object getPolicyOwner(Object hookedObject) {
        if (hookedObject == null) {
            return null;
        }
        final Class<?> hookedClass = hookedObject.getClass();
        if (CLASS_PHONE_WINDOW_MANAGER.equals(hookedClass.getName())) {
            return hookedObject;
        }
        try {
            final Field outerField = findField(hookedClass, "this$0");
            if (outerField == null) {
                return hookedObject;
            }
            outerField.setAccessible(true);
            final Object outer = outerField.get(hookedObject);
            if (outer != null && CLASS_PHONE_WINDOW_MANAGER.equals(outer.getClass().getName())) {
                return outer;
            }
        } catch (Throwable ignored) {
            // Ignore and fallback to hookedObject.
        }
        return hookedObject;
    }

    private Field findField(Class<?> clazz, String name) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private Class<?> findClass(String name, ClassLoader classLoader) {
        if (name == null) {
            return null;
        }
        try {
            return Class.forName(name, false, classLoader);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private Method findMethod(Class<?> clazz, String name, int parameterCount) {
        if (clazz == null) {
            return null;
        }
        Class<?> current = clazz;
        while (current != null) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.getName().equals(name) && method.getParameterCount() == parameterCount) {
                    method.setAccessible(true);
                    return method;
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private void tryDeoptimize(@NonNull Method method, @NonNull String label) {
        try {
            final boolean result = deoptimize(method);
            log(Log.INFO, TAG, "Deoptimize " + label + ": " + result);
        } catch (Throwable throwable) {
            log(Log.WARN, TAG, "Failed to deoptimize " + label, throwable);
        }
    }

    private static final class Config {
        final boolean enabled;
        final boolean fallbackWhenAppMissing;
        final boolean fallbackWhenStartFailed;
        final boolean fallbackWhenLocked;
        final boolean keepVeryLongPress;

        Config(
                boolean enabled,
                boolean fallbackWhenAppMissing,
                boolean fallbackWhenStartFailed,
                boolean fallbackWhenLocked,
                boolean keepVeryLongPress
        ) {
            this.enabled = enabled;
            this.fallbackWhenAppMissing = fallbackWhenAppMissing;
            this.fallbackWhenStartFailed = fallbackWhenStartFailed;
            this.fallbackWhenLocked = fallbackWhenLocked;
            this.keepVeryLongPress = keepVeryLongPress;
        }

        static Config defaults() {
            return new Config(
                    XposedConfig.DEFAULT_ENABLED,
                    XposedConfig.DEFAULT_FALLBACK_WHEN_APP_MISSING,
                    XposedConfig.DEFAULT_FALLBACK_WHEN_START_FAILED,
                    XposedConfig.DEFAULT_FALLBACK_WHEN_LOCKED,
                    XposedConfig.DEFAULT_KEEP_VERY_LONG_PRESS
            );
        }
    }
}
