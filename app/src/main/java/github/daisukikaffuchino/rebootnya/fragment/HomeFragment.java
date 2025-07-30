package github.daisukikaffuchino.rebootnya.fragment;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IPowerManager;
import android.os.RemoteException;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.topjohnwu.superuser.Shell;

import java.io.IOException;
import java.lang.reflect.Field;

import github.daisukikaffuchino.rebootnya.MainActivity;
import github.daisukikaffuchino.rebootnya.R;
import github.daisukikaffuchino.rebootnya.shizuku.NyaRemoteProcess;
import moe.shizuku.server.IShizukuService;
import rikka.shizuku.Shizuku;
import rikka.shizuku.ShizukuBinderWrapper;
import rikka.shizuku.SystemServiceHelper;

public class HomeFragment extends DialogFragment {
    private Context context;
    private int checkedItem = 0;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        context = requireActivity();

        final String[] items = {getString(R.string.lock_screen), getString(R.string.reboot), getString(R.string.soft_reboot), getString(R.string.system_ui),
                "Recovery", "Bootloader", getString(R.string.safe_mode), getString(R.string.power_off)};

        SharedPreferences sp = context.getSharedPreferences("Nya", Context.MODE_PRIVATE);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle(R.string.app_name);
        builder.setSingleChoiceItems(items, checkedItem, (dialog, which) -> checkedItem = which);
        builder.setPositiveButton(R.string.confirm, null);
        builder.setNegativeButton(R.string.close, null);
        builder.setNeutralButton(R.string.setting, null);

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button neutralButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            // 不调用dismiss()对话框就不会关闭
            positiveButton.setOnClickListener(v -> {
                if (sp.getString("work_mode", "Root").equals("Root"))
                    funcRoot();
                else {
                    try {
                        funcShizuku();
                    } catch (IOException e) {
                        e.fillInStackTrace();
                        Toast.makeText(context, R.string.exec_fail, Toast.LENGTH_SHORT).show();
                    }
                }
            });
            neutralButton.setOnClickListener(v -> {
                NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
                navController.navigate(R.id.nav_settings);
            });
        });
        return dialog;
    }

    private void runRootCommand(String cmd) {
        if (runRootCommandWithResult(cmd))
            dismiss();
        else
            Toast.makeText(context, R.string.exec_fail, Toast.LENGTH_SHORT).show();
    }

    private boolean runRootCommandWithResult(String cmd) {
        if (Boolean.FALSE.equals(Shell.isAppGrantedRoot())) {
            Toast.makeText(context, R.string.no_root, Toast.LENGTH_SHORT).show();
            return false;
        } else {
            Shell.Result result;
            result = Shell.cmd(cmd).exec();
            return result.isSuccess();
        }
    }

    private void shizukuReboot(boolean confirm, @Nullable String reason) {
        try {
            IPowerManager powerManager = IPowerManager.Stub.asInterface(
                    new ShizukuBinderWrapper(SystemServiceHelper.getSystemService("power"))
            );
            powerManager.reboot(confirm, reason, false);
        } catch (Exception e) {
            e.fillInStackTrace();
            Toast.makeText(context, "Error:" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private int shizukuProcess(String[] cmd) {
        try {
            Field privateField = Shizuku.class.getDeclaredField("service");
            privateField.setAccessible(true);
            IShizukuService value = (IShizukuService) privateField.get(null);
            try {
                assert value != null;
                Process process = new NyaRemoteProcess(value.newProcess(cmd, null, null));
                return process.waitFor();
            } catch (RemoteException e) {
                e.fillInStackTrace();
                Toast.makeText(context, "Error:" + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            e.fillInStackTrace();
            Toast.makeText(context, "Error:" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
        return 100;
    }

    private void funcRoot() {
        switch (checkedItem) {
            case 0:
                runRootCommand("input keyevent KEYCODE_POWER");
                break;
            case 1:
                runRootCommand("svc power reboot");
                break;
            case 2:
                runRootCommand("busybox killall system_server");
                break;
            case 3:
                runRootCommand("busybox killall com.android.systemui");
                break;
            case 4:
                runRootCommand("svc power reboot recovery");
                break;
            case 5:
                runRootCommand("svc power reboot bootloader");
                break;
            case 6:
                if (runRootCommandWithResult("setprop persist.sys.safemode"))
                    runRootCommand("svc power reboot");
                break;
            case 7:
                runRootCommand("reboot -p");
                break;
        }
    }

    private void funcShizuku() throws IOException {
        if (!MainActivity.checkShizukuPermission()) {
            Toast.makeText(context, R.string.shizuku_denied, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!Shizuku.pingBinder()) {
            Toast.makeText(context, R.string.shizuku_not_run, Toast.LENGTH_SHORT).show();
            return;
        }
        switch (checkedItem) {
            case 0:
                int exitCode = shizukuProcess(new String[]{"input", "keyevent", "KEYCODE_POWER"});
                if (exitCode == 0)
                    dismiss();
                else
                    Toast.makeText(context, R.string.exec_fail, Toast.LENGTH_SHORT).show();
                break;
            case 1:
                shizukuReboot(false, null);
                break;
            case 2:
                shizukuReboot(false, "userspace");
                break;
            case 3:
                int exitCode2 = shizukuProcess(new String[]{"pkill", "-f", "com.android.systemui"});
                if (exitCode2 == 0)
                    dismiss();
                else
                    Toast.makeText(context, R.string.exec_fail, Toast.LENGTH_SHORT).show();
                break;
            case 4:
                shizukuReboot(false, "recovery");
                break;
            case 5:
                shizukuReboot(false, "bootloader");
                break;
            case 6:
                shizukuReboot(true, "safemode");
                break;
            case 7:
                shizukuProcess(new String[]{"reboot", "-p"});
                break;
        }
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        System.exit(0);
    }

}
