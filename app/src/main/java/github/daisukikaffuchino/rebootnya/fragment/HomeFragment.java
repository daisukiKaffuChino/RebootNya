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

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import github.daisukikaffuchino.rebootnya.MainActivity;
import github.daisukikaffuchino.rebootnya.R;
import github.daisukikaffuchino.rebootnya.SettingsActivity;
import github.daisukikaffuchino.rebootnya.shizuku.NyaShellManager;
import github.daisukikaffuchino.rebootnya.utils.ListItemEnum;
import github.daisukikaffuchino.rebootnya.utils.NyaSettings;
import github.daisukikaffuchino.rebootnya.utils.NyaUtilKt;
import github.daisukikaffuchino.rebootnya.utils.RootUtil;
import github.daisukikaffuchino.rebootnya.utils.ShizukuUtil;

public class HomeFragment extends DialogFragment {
    private Context context;
    private int checkedItem = 0;
    private RootUtil rootUtil;
    private ShizukuUtil shizukuUtil;

    @SuppressLint("InflateParams")
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        LinkedHashMap<String, ListItemEnum> listMap = createListItemMap();
        List<String> _itemList;
        _itemList = new ArrayList<>(listMap.keySet());

        if (MainActivity.Companion.getListFilterStatus()) {
            _itemList = NyaUtilKt.exclude(_itemList, ListItemEnum.SAFE_MODE.getLocalizedDisplayName(context),
                    ListItemEnum.SOFT_REBOOT.getLocalizedDisplayName(context));
        }

        final String[] items = _itemList.toArray(new String[0]);

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
                    doAction(ListItemEnum.Companion.fromLocalizedDisplayName(context, items[checkedItem])));
            neutralButton.setOnClickListener(v -> {
//                NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
//                navController.navigate(R.id.nav_settings);
                Intent intent = new Intent(context, SettingsActivity.class);
                context.startActivity(intent);
            });
        });

        MaterialAlertDialogBuilder loadingDialog = new MaterialAlertDialogBuilder(context);
        loadingDialog.setTitle(R.string.app_name)
                .setView(getLayoutInflater()
                        .inflate(R.layout.dialog_progress, null))
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
        context = requireActivity();
        rootUtil = new RootUtil(context);
        shizukuUtil = new ShizukuUtil(context);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (NyaSettings.getShizukuShellMode() == NyaSettings.STORE.USER_SERVICE)
            NyaShellManager.INSTANCE.bindService(shizukuUtil, (exitCode, message) -> Log.d("main", "bind " + exitCode));
    }

    private LinkedHashMap<String, ListItemEnum> createListItemMap() {
        LinkedHashMap<String, ListItemEnum> map = new LinkedHashMap<>();
        for (ListItemEnum item : ListItemEnum.getEntries()) {
            map.put(item.getLocalizedDisplayName(context), item);
        }
        return map;
    }

    private void doAction(ListItemEnum listItemEnum) {
        if (NyaSettings.getWorkMode() == NyaSettings.STORE.ROOT)
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
        if (rootUtil.runRootCommandWithResult(cmd))
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
                if (rootUtil.runRootCommandWithResult("setprop persist.sys.safemode 1"))
                    runRootCommand("svc power reboot");
                break;
            case POWER_OFF:
                runRootCommand("reboot -p");
                break;
        }
    }

    private void funcShizuku(ListItemEnum listItemEnum) throws IOException {
        if (!shizukuUtil.checkShizukuPermission()) {
            Toast.makeText(context, R.string.shizuku_denied, Toast.LENGTH_SHORT).show();
            return;
        }
        switch (listItemEnum) {
            case LOCK_SCREEN:
                int lockExitCode = shizukuUtil.runShizukuCommand(new String[]{"input", "keyevent", "KEYCODE_POWER"}, false);
                if (lockExitCode == 0) dismiss();
                break;
            case REBOOT:
                shizukuUtil.shizukuReboot(null);
                break;
            case SOFT_REBOOT:
                shizukuUtil.runShizukuCommand(new String[]{"setprop", "ctl.restart", "zygote"}, true);
                break;
            case SYSTEM_UI:
                int stopUiCode = shizukuUtil.runShizukuCommand(new String[]{"am", "force-stop", "com.android.systemui"}, false);
                if (stopUiCode == 0) {
                    Toast.makeText(context, R.string.stop_system_ui_tip, Toast.LENGTH_LONG).show();
                    dismiss();
                }
                break;
            case RECOVERY:
                shizukuUtil.runShizukuCommand(new String[]{"reboot", "recovery"}, false);
                break;
            case BOOTLOADER:
                shizukuUtil.shizukuReboot("bootloader");
                break;
            case SAFE_MODE:
                int exitCode = shizukuUtil.runShizukuCommand(new String[]{"setprop", "persist.sys.safemode", "1"}, true);
                if (exitCode == 0) shizukuUtil.shizukuReboot(null);
                else Toast.makeText(context, R.string.exec_fail, Toast.LENGTH_SHORT).show();
                break;
            case POWER_OFF:
                shizukuUtil.runShizukuCommand(new String[]{"reboot", "-p"}, false);
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
