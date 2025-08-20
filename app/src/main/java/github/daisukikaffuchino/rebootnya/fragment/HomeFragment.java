package github.daisukikaffuchino.rebootnya.fragment;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import github.daisukikaffuchino.rebootnya.NyaApplication;
import github.daisukikaffuchino.rebootnya.R;
import github.daisukikaffuchino.rebootnya.shizuku.NyaShellManager;
import github.daisukikaffuchino.rebootnya.utils.ListItemEnum;
import github.daisukikaffuchino.rebootnya.utils.RootUtilKt;
import github.daisukikaffuchino.rebootnya.utils.ShizukuUtilKt;

public class HomeFragment extends DialogFragment {
    private Context context;
    private int checkedItem = 0;

    @SuppressLint("InflateParams")
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        context = requireActivity();
        LinkedHashMap<String, ListItemEnum> listMap = createListItemMap();
        List<String> _itemList = new ArrayList<>(listMap.keySet());
        String[] items = _itemList.toArray(new String[0]);
        //Log.d("xxx3",ListItemEnum.Companion.fromDisplayName("system_ui").getLocalizedDisplayName());

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
            positiveButton.setOnClickListener(v ->
                    doAction(ListItemEnum.Companion.fromLocalizedDisplayName(items[checkedItem])));
            neutralButton.setOnClickListener(v -> {
                NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
                navController.navigate(R.id.nav_settings);
            });
        });

        MaterialAlertDialogBuilder loadingDialog = new MaterialAlertDialogBuilder(context);
        loadingDialog.setTitle(R.string.app_name)
                .setView(getLayoutInflater()
                        .inflate(R.layout.dialog_material_progress, null))
                .setCancelable(false);

        Intent intent = requireActivity().getIntent();
        if (intent != null && Intent.ACTION_RUN.equals(intent.getAction())) {
            String data = intent.getStringExtra("extra");
            if (data != null)
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    doAction(ListItemEnum.Companion.fromDisplayName(data));
                    dismiss();
                }, 1000);
            return loadingDialog.create();
        } else return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        NyaShellManager.INSTANCE.bindService((exitCode, message) -> Log.d("main", "bind " + exitCode));
        getParentFragmentManager().setFragmentResultListener("requestKey", this, (requestKey, result) -> {
            boolean isShizukuActive = result.getBoolean("isShizukuActive");
            if (isShizukuActive)
                NyaShellManager.INSTANCE.bindService((exitCode, message) -> Log.d("main", "bind " + exitCode));
        });
    }

    @Override
    public void onStart() {
        super.onStart();

    }

    private LinkedHashMap<String, ListItemEnum> createListItemMap() {
        LinkedHashMap<String, ListItemEnum> map = new LinkedHashMap<>();
        for (ListItemEnum item : ListItemEnum.getEntries()) {
            map.put(item.getLocalizedDisplayName(), item);
        }
        return map;
    }

    private void doAction(ListItemEnum listItemEnum) {
        if (NyaApplication.sp.getString("work_mode", "Root").equals("Root"))
            funcRoot(listItemEnum);
        else {
            try {
                funcShizuku(listItemEnum);
            } catch (IOException e) {
                e.fillInStackTrace();
                Toast.makeText(context, R.string.exec_fail, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void runRootCommand(String cmd) {
        if (RootUtilKt.runRootCommandWithResult(cmd))
            dismiss();
        else
            Toast.makeText(context, R.string.exec_fail, Toast.LENGTH_SHORT).show();
    }

    private void funcRoot(ListItemEnum listItemEnum) {
        switch (listItemEnum) {
            case LOCK_SCREEN:
                runRootCommand("input keyevent KEYCODE_POWER");
                break;
            case REBOOT:
                runRootCommand("svc power reboot");
                break;
            case SOFT_REBOOT:
                runRootCommand("setprop ctl.restart zygote");
                break;
            case SYSTEM_UI:
                runRootCommand("pkill -f com.android.systemui");
                break;
            case RECOVERY:
                runRootCommand("svc power reboot recovery");
                break;
            case BOOTLOADER:
                runRootCommand("svc power reboot bootloader");
                break;
            case SAFE_MODE:
                if (RootUtilKt.runRootCommandWithResult("setprop persist.sys.safemode 1"))
                    runRootCommand("svc power reboot");
                break;
            case POWER_OFF:
                runRootCommand("reboot -p");
                break;
        }
    }

    private void funcShizuku(ListItemEnum listItemEnum) throws IOException {
        if (!ShizukuUtilKt.checkShizukuPermission()) {
            Toast.makeText(context, R.string.shizuku_denied, Toast.LENGTH_SHORT).show();
            return;
        }
        switch (listItemEnum) {
            case LOCK_SCREEN:
                int lockExitCode = ShizukuUtilKt.runShizukuCommand(new String[]{"input", "keyevent", "KEYCODE_POWER"}, false);
                if (lockExitCode == 0) dismiss();
                break;
            case REBOOT:
                ShizukuUtilKt.shizukuReboot(null);
                break;
            case SOFT_REBOOT:
                ShizukuUtilKt.runShizukuCommand(new String[]{"setprop", "ctl.restart", "zygote"}, true);
                break;
            case SYSTEM_UI:
                ShizukuUtilKt.runShizukuCommand(new String[]{"pkill", "-f", "com.android.systemui"}, true);
                break;
            case RECOVERY:
                ShizukuUtilKt.runShizukuCommand(new String[]{"reboot", "recovery"}, false);
                break;
            case BOOTLOADER:
                ShizukuUtilKt.shizukuReboot("bootloader");
                break;
            case SAFE_MODE:
                int exitCode = ShizukuUtilKt.runShizukuCommand(new String[]{"setprop", "persist.sys.safemode", "1"}, true);
                if (exitCode == 0) ShizukuUtilKt.shizukuReboot(null);
                else Toast.makeText(context, R.string.exec_fail, Toast.LENGTH_SHORT).show();
                break;
            case POWER_OFF:
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
