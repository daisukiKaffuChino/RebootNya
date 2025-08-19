package github.daisukikaffuchino.rebootnya.fragment;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
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
import github.daisukikaffuchino.rebootnya.shizuku.NyaShellManager;
import github.daisukikaffuchino.rebootnya.utils.RootUtilKt;
import github.daisukikaffuchino.rebootnya.utils.ShizukuUtilKt;

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

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        NyaShellManager.INSTANCE.bindService((exitCode, message) -> Log.d("main", "bind "+exitCode));
        getParentFragmentManager().setFragmentResultListener("requestKey", this, (requestKey, result) -> {
            boolean isShizukuActive = result.getBoolean("isShizukuActive");
            if (isShizukuActive) NyaShellManager.INSTANCE.bindService((exitCode, message) -> Log.d("main", "bind "+exitCode));
        });
    }

    private void runRootCommand(String cmd) {
        if (RootUtilKt.runRootCommandWithResult(cmd))
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
                runRootCommand("setprop ctl.restart zygote");
                break;
            case 3:
                runRootCommand("pkill -f com.android.systemui");
                break;
            case 4:
                runRootCommand("svc power reboot recovery");
                break;
            case 5:
                runRootCommand("svc power reboot bootloader");
                break;
            case 6:
                if (RootUtilKt.runRootCommandWithResult("setprop persist.sys.safemode 1"))
                    runRootCommand("svc power reboot");
                break;
            case 7:
                runRootCommand("reboot -p");
                break;
        }
    }

    private void funcShizuku() throws IOException {
        if (!ShizukuUtilKt.checkShizukuPermission()) {
            Toast.makeText(context, R.string.shizuku_denied, Toast.LENGTH_SHORT).show();
            return;
        }
        switch (checkedItem) {
            case 0:
                int lockExitCode = ShizukuUtilKt.runShizukuCommand(new String[]{"input", "keyevent", "KEYCODE_POWER"}, false);
                if (lockExitCode == 0) dismiss();
                break;
            case 1:
                ShizukuUtilKt.shizukuReboot(null);
                break;
            case 2:
                ShizukuUtilKt.runShizukuCommand(new String[]{"setprop", "ctl.restart", "zygote"}, true);
                break;
            case 3:
                ShizukuUtilKt.runShizukuCommand(new String[]{"pkill", "-f", "com.android.systemui"}, true);
                break;
            case 4:
                ShizukuUtilKt.runShizukuCommand(new String[]{"reboot", "recovery"}, false);
                break;
            case 5:
                ShizukuUtilKt.shizukuReboot("bootloader");
                break;
            case 6:
                int exitCode = ShizukuUtilKt.runShizukuCommand(new String[]{"setprop", "persist.sys.safemode", "1"}, true);
                if (exitCode == 0) ShizukuUtilKt.shizukuReboot(null);
                else Toast.makeText(context, R.string.exec_fail, Toast.LENGTH_SHORT).show();
                break;
            case 7:
                ShizukuUtilKt.runShizukuCommand(new String[]{"reboot", "-p"}, false);
                break;
        }
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        /*
        为什么使用 System.exit(0) ?
        因为 KernelSU 授权后需要杀死并重启进程使权限生效。
        通常 activity.finish() 仅限于关闭活动，进程由系统决定回收。
        对于像本项目这样的单线程应用，这种做法是安全的。
         */
        NyaShellManager.INSTANCE.unbindService();
        if (!requireActivity().isChangingConfigurations())
            System.exit(0);
    }

}
