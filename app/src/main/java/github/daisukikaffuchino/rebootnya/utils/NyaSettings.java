package github.daisukikaffuchino.rebootnya.utils;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.annotation.SuppressLint;
import android.app.UiModeManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;

import java.lang.annotation.Retention;
import java.util.Locale;

public class NyaSettings {
    public static final String NAME = "settings";

    private static SharedPreferences sPreferences;

    public static SharedPreferences getPreferences() {
        return sPreferences;
    }

    @NonNull
    private static Context getSettingsStorageContext(@NonNull Context context) {
        Context storageContext;
        storageContext = context.createDeviceProtectedStorageContext();

        storageContext = new ContextWrapper(storageContext) {
            @Override
            public SharedPreferences getSharedPreferences(String name, int mode) {
                try {
                    return super.getSharedPreferences(name, mode);
                } catch (IllegalStateException e) {
                    return new EmptySharedPreferencesImpl();
                }
            }
        };

        return storageContext;
    }

    public static void initialize(Context context) {
        if (sPreferences == null) {
            sPreferences = getSettingsStorageContext(context)
                    .getSharedPreferences(NAME, Context.MODE_PRIVATE);
        }
    }

    @SuppressLint("UniqueConstants")
    @IntDef({
            STORE.SHIZUKU,
            STORE.ROOT,
            STORE.PROCESS,
            STORE.USER_SERVICE
    })
    @Retention(SOURCE)
    public @interface STORE {
        int SHIZUKU = 1;
        int ROOT = 2;
        int PROCESS = 1;
        int USER_SERVICE = 2;
    }

    @SuppressLint("UniqueConstants")
    @IntDef({
            STYLE.CLASSIC_LIST,
            STYLE.MODERN_BUTTONS
    })
    @Retention(SOURCE)
    public @interface STYLE {
        int CLASSIC_LIST = 1;
        int MODERN_BUTTONS = 2;
    }

    @STORE
    public static int getWorkMode() {
        return getPreferences().getInt("work_mode", STORE.SHIZUKU);
    }

    @STYLE
    public static int getMainInterfaceStyle() {
        return getPreferences().getInt("main_interface_style", STYLE.CLASSIC_LIST);
    }

    @STORE
    public static int getShizukuShellMode() {
        return getPreferences().getInt("shizuku_shell_mode", STORE.PROCESS);
    }

    @AppCompatDelegate.NightMode
    public static int getNightMode(Context context) {
        int defValue = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        if (context.getSystemService(UiModeManager.class).getCurrentModeType()
                == Configuration.UI_MODE_TYPE_WATCH) {
            defValue = AppCompatDelegate.MODE_NIGHT_YES;
        }
        return getPreferences().getInt("night_mode", defValue);
    }

    public static Locale getLocale() {
        String tag = getPreferences().getString("language", null);
        if (TextUtils.isEmpty(tag) || "SYSTEM".equals(tag)) {
            return Locale.getDefault();
        }
        return Locale.forLanguageTag(tag);
    }

    public static boolean getIsHideUnavailableOptions(){
        return getPreferences().getBoolean("hide_unavailable_options",true);
    }

    public static boolean isUsingSystemColor() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && getPreferences().getBoolean("dynamic_color", false);
    }
}