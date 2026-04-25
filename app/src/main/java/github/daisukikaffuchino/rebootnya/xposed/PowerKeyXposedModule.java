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
import java.util.HashSet;
import java.util.Set;

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
    private static final String[] POLICY_CLASS_CANDIDATES = new String[] {
            "com.android.server.policy.PhoneWindowManager",
            "com.android.server.policy.OplusPhoneWindowManager",
            "com.android.server.policy.PhoneWindowManagerExtImpl",
            "com.android.server.policy.OplusPhoneWindowManagerEx"
    };
    private static final String[] POWER_RULE_CLASS_CANDIDATES = new String[] {
            "com.android.server.policy.PhoneWindowManager$PowerKeyRule",
            "com.android.server.policy.OplusPhoneWindowManager$PowerKeyRule"
    };
    private static final int[] LONG_PRESS_PARAMETER_COUNTS = new int[] {0, 1, 2, 3};

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
        boolean hooked = false;
        final Set<String> hookedMethods = new HashSet<>();

        for (String className : POLICY_CLASS_CANDIDATES) {
            final Class<?> clazz = findClass(className, classLoader);
            if (clazz == null) {
                continue;
            }
            for (int parameterCount : LONG_PRESS_PARAMETER_COUNTS) {
                final Method method = findMethod(clazz, "powerLongPress", parameterCount);
                if (method == null || !hookedMethods.add(method.toGenericString())) {
                    continue;
                }
                installIntercept(method, className + "#powerLongPress/" + parameterCount);
                tryDeoptimize(method, className + "#powerLongPress/" + parameterCount);
                log(Log.INFO, TAG, "Hooked " + method.toGenericString());
                hooked = true;
            }
        }

        for (String className : POWER_RULE_CLASS_CANDIDATES) {
            final Class<?> clazz = findClass(className, classLoader);
            if (clazz == null) {
                continue;
            }
            for (int parameterCount : LONG_PRESS_PARAMETER_COUNTS) {
                final Method method = findMethod(clazz, "onLongPress", parameterCount);
                if (method == null || !hookedMethods.add(method.toGenericString())) {
                    continue;
                }
                installIntercept(method, className + "#onLongPress/" + parameterCount);
                tryDeoptimize(method, className + "#onLongPress/" + parameterCount);
                log(Log.INFO, TAG, "Hooked fallback " + method.toGenericString());
                hooked = true;
            }
        }

        if (!hooked) {
            log(Log.WARN, TAG, "No long press hook point found");
        }
    }

    private void hookVeryLongPress(@NonNull ClassLoader classLoader) {
        boolean hooked = false;
        final Set<String> hookedMethods = new HashSet<>();

        for (String className : POLICY_CLASS_CANDIDATES) {
            final Class<?> clazz = findClass(className, classLoader);
            if (clazz == null) {
                continue;
            }
            for (int parameterCount : LONG_PRESS_PARAMETER_COUNTS) {
                final Method method = findMethod(clazz, "powerVeryLongPress", parameterCount);
                if (method == null || !hookedMethods.add(method.toGenericString())) {
                    continue;
                }
                installVeryLongPressIntercept(method, className + "#powerVeryLongPress/" + parameterCount);
                tryDeoptimize(method, className + "#powerVeryLongPress/" + parameterCount);
                log(Log.INFO, TAG, "Hooked " + method.toGenericString());
                hooked = true;
            }
        }

        for (String className : POWER_RULE_CLASS_CANDIDATES) {
            final Class<?> clazz = findClass(className, classLoader);
            if (clazz == null) {
                continue;
            }
            for (int parameterCount : LONG_PRESS_PARAMETER_COUNTS) {
                final Method method = findMethod(clazz, "onVeryLongPress", parameterCount);
                if (method == null || !hookedMethods.add(method.toGenericString())) {
                    continue;
                }
                installVeryLongPressIntercept(method, className + "#onVeryLongPress/" + parameterCount);
                tryDeoptimize(method, className + "#onVeryLongPress/" + parameterCount);
                log(Log.INFO, TAG, "Hooked fallback " + method.toGenericString());
                hooked = true;
            }
        }

        if (!hooked) {
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
        log(Log.DEBUG, TAG, "handlePowerLongPress config: enabled=" + config.enabled
                + ", fallbackWhenAppMissing=" + config.fallbackWhenAppMissing
                + ", fallbackWhenStartFailed=" + config.fallbackWhenStartFailed
                + ", fallbackWhenLocked=" + config.fallbackWhenLocked
                + ", keepVeryLongPress=" + config.keepVeryLongPress);
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
            log(Log.WARN, TAG, "Remote preferences unavailable, using defaults");
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
        closeSystemDialogs(context);

        final Intent explicitIntent = new Intent(TARGET_ACTION)
                .setComponent(new ComponentName(TARGET_PACKAGE, TARGET_ACTIVITY))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        if (tryLaunchIntent(context, explicitIntent, "explicitLaunchIntent")) {
            return true;
        }

        final Intent packageLaunchIntent = context.getPackageManager().getLaunchIntentForPackage(TARGET_PACKAGE);
        if (packageLaunchIntent != null) {
            packageLaunchIntent.setAction(TARGET_ACTION);
            packageLaunchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            if (tryLaunchIntent(context, packageLaunchIntent, "packageLaunchIntent")) {
                return true;
            }
        }

        return false;
    }

    private boolean tryLaunchIntent(@NonNull Context context, @NonNull Intent intent, @NonNull String label) {
        try {
            if (startAsCurrentUser(context, intent)) {
                log(Log.INFO, TAG, "Launch success via startActivityAsUser: " + label);
                return true;
            }

            context.startActivity(intent);
            log(Log.INFO, TAG, "Launch success via startActivity: " + label);
            return true;
        } catch (Throwable throwable) {
            log(Log.WARN, TAG, "Launch strategy failed: " + label, throwable);
            return false;
        }
    }

    private boolean startAsCurrentUser(@NonNull Context context, @NonNull Intent intent) {
        try {
            final Method getCurrentUser = ActivityManager.class.getDeclaredMethod("getCurrentUser");
            getCurrentUser.setAccessible(true);
            final int userId = (Integer) getCurrentUser.invoke(null);
            final UserHandle userHandle = createUserHandle(userId);
            final Method startActivityAsUser = findMethodInHierarchy(
                    context.getClass(),
                    "startActivityAsUser",
                    Intent.class,
                    UserHandle.class
            );
            if (startActivityAsUser == null) {
                log(Log.WARN, TAG, "startActivityAsUser not found on " + context.getClass().getName());
                return false;
            }
            startActivityAsUser.setAccessible(true);
            startActivityAsUser.invoke(context, intent, userHandle);
            return true;
        } catch (Throwable throwable) {
            log(Log.WARN, TAG, "startAsCurrentUser failed", throwable);
            return false;
        }
    }

    private UserHandle createUserHandle(int userId) throws Throwable {
        try {
            final Method ofMethod = UserHandle.class.getDeclaredMethod("of", int.class);
            ofMethod.setAccessible(true);
            return (UserHandle) ofMethod.invoke(null, userId);
        } catch (Throwable ignored) {
            final java.lang.reflect.Constructor<UserHandle> constructor =
                    UserHandle.class.getDeclaredConstructor(int.class);
            constructor.setAccessible(true);
            return constructor.newInstance(userId);
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

    private Method findMethodInHierarchy(Class<?> clazz, String name, Class<?>... parameterTypes) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredMethod(name, parameterTypes);
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private void closeSystemDialogs(@NonNull Context context) {
        try {
            context.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
        } catch (Throwable throwable) {
            log(Log.WARN, TAG, "Failed to close system dialogs before launch", throwable);
        }
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
