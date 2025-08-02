package github.daisukikaffuchino.rebootnya.fragment;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.IOException;

import github.daisukikaffuchino.rebootnya.NyaApplication;
import github.daisukikaffuchino.rebootnya.R;
import rikka.shizuku.Shizuku;

public class HomeFragment extends DialogFragment {
    private Context context;
    private int checkedItem = 0;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        context = requireActivity();

        final String[] items = {getString(R.string.lock_screen), getString(R.string.reboot), getString(R.string.soft_reboot), getString(R.string.system_ui),
                "Recovery", "Bootloader", getString(R.string.safe_mode), getString(R.string.power_off)};

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
                if (NyaApplication.sp.getString("work_mode", "Root").equals("Root"))
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
        if (NyaApplication.rootUtil.runRootCommandWithResult(cmd))
            dismiss();
        else
            Toast.makeText(context, R.string.exec_fail, Toast.LENGTH_SHORT).show();
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
                if (NyaApplication.rootUtil.runRootCommandWithResult("setprop persist.sys.safemode 1"))
                    runRootCommand("svc power reboot");
                break;
            case 7:
                runRootCommand("reboot -p");
                break;
        }
    }

    private void funcShizuku() throws IOException {
        if (!NyaApplication.shizukuUtil.checkShizukuPermission()) {
            Toast.makeText(context, R.string.shizuku_denied, Toast.LENGTH_SHORT).show();
            return;
        }
        switch (checkedItem) {
            case 0:
                int exitCode = NyaApplication.shizukuUtil.shizukuProcess(new String[]{"input", "keyevent", "KEYCODE_POWER"});
                if (exitCode == 0) dismiss();
                else Toast.makeText(context, R.string.exec_fail, Toast.LENGTH_SHORT).show();
                break;
            case 1:
                NyaApplication.shizukuUtil.shizukuReboot(null);
                break;
            case 2:
                NyaApplication.shizukuUtil.shizukuReboot("userspace");
                break;
            case 3:
                if (Shizuku.getUid() == 2000)
                    Toast.makeText(context, R.string.shizuku_permission_insufficient, Toast.LENGTH_SHORT).show();
                int exitCode2 = NyaApplication.shizukuUtil.shizukuProcess(new String[]{"pkill", "-f", "com.android.systemui"});
                if (exitCode2 != 0)
                    Toast.makeText(context, R.string.exec_fail, Toast.LENGTH_SHORT).show();
                break;
            case 4:
                NyaApplication.shizukuUtil.shizukuReboot("recovery");
                break;
            case 5:
                NyaApplication.shizukuUtil.shizukuReboot("bootloader");
                break;
            case 6:
                if (Shizuku.getUid() == 2000)
                    Toast.makeText(context, R.string.shizuku_permission_insufficient, Toast.LENGTH_SHORT).show();
                int exitCode3 = NyaApplication.shizukuUtil.shizukuProcess(new String[]{"setprop", "persist.sys.safemode", "1"});
                //Log.d("xxx", String.valueOf(exitCode3));
                if (exitCode3 == 0) {
                    NyaApplication.shizukuUtil.shizukuReboot(null);
                    dismiss();
                } else Toast.makeText(context, R.string.exec_fail, Toast.LENGTH_SHORT).show();
                break;
            case 7:
                NyaApplication.shizukuUtil.shizukuProcess(new String[]{"reboot", "-p"});
                break;
        }
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        System.exit(0);
    }

}
