package github.daisukikaffuchino.rebootnya.fragment;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.ColorUtils;
import androidx.fragment.app.DialogFragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.shape.CornerFamily;
import com.google.android.material.shape.ShapeAppearanceModel;

import java.io.IOException;

import github.daisukikaffuchino.rebootnya.NyaApplication;
import github.daisukikaffuchino.rebootnya.R;
import rikka.shizuku.Shizuku;

public class HomeFragment extends DialogFragment {
    private Context context;
    private View view;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        context = requireActivity();

        final String[] items = {getString(R.string.lock_screen), getString(R.string.reboot), getString(R.string.soft_reboot), getString(R.string.system_ui),
                "Recovery", "Bootloader", getString(R.string.safe_mode), getString(R.string.power_off)};

        ScrollView scrollView = new ScrollView(context);
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPaddingRelative(104, 48, 104, 0);

        for (int i = 0; i < items.length; i++) {
            MaterialButton button = new MaterialButton(context);
            int baseColor = MaterialColors.getColor(button, com.google.android.material.R.attr.colorPrimaryFixedDim);
            int textColor = MaterialColors.getColor(button, com.google.android.material.R.attr.colorOnPrimaryContainer);
            int translucentColor = ColorUtils.setAlphaComponent(baseColor, (int)(0.08 * 255));
            button.setText(items[i]);
            button.setBackgroundTintList(ColorStateList.valueOf(translucentColor));
            button.setTextColor(textColor);
            button.setCornerRadius(6);
            button.setInsetTop(2);
            button.setInsetBottom(2);
            button.setHeight(144);
            button.setTextSize(15);
            button.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));

            if (i == 0) {
                button.setShapeAppearanceModel(new ShapeAppearanceModel()
                        .toBuilder()
                        .setTopLeftCorner(CornerFamily.ROUNDED, 32)
                        .setTopRightCorner(CornerFamily.ROUNDED, 32)
                        .setBottomLeftCorner(CornerFamily.ROUNDED, 6)
                        .setBottomRightCorner(CornerFamily.ROUNDED, 6)
                        .build());
            } else if (i == items.length - 1) {
                button.setShapeAppearanceModel(new ShapeAppearanceModel()
                        .toBuilder()
                        .setTopLeftCorner(CornerFamily.ROUNDED, 6)
                        .setTopRightCorner(CornerFamily.ROUNDED, 6)
                        .setBottomLeftCorner(CornerFamily.ROUNDED, 32)
                        .setBottomRightCorner(CornerFamily.ROUNDED, 32)
                        .build());
            }

            int finalI = i;
            button.setOnClickListener(v -> handleButtonAction(finalI));

            layout.addView(button);
        }

        scrollView.addView(layout);
        scrollView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        this.view = scrollView;
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle(R.string.app_name);
        builder.setView(scrollView);
        builder.setNegativeButton(R.string.close, null);
        builder.setNeutralButton(R.string.setting, null);

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            Button neutralButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            // 不调用dismiss()对话框就不会关闭
            neutralButton.setOnClickListener(v -> {
                NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
                navController.navigate(R.id.nav_settings);
            });
        });
        return dialog;
    }

    private void handleButtonAction(int actionId) {
        SharedPreferences sp = context.getSharedPreferences("Nya", Context.MODE_PRIVATE);
        updateAllMaterialButtons(view, false);
        if (sp.getString("work_mode", "Root").equals("Root"))
            funcRoot(actionId);
        else {
            try {
                funcShizuku(actionId);
            } catch (IOException e) {
                e.fillInStackTrace();
                Toast.makeText(context, R.string.exec_fail, Toast.LENGTH_SHORT).show();
            }
        }
        updateAllMaterialButtons(view, true);
    }

    private void updateAllMaterialButtons(View view, boolean status) {
        if (view == null) return;
        if (view instanceof MaterialButton) {
            view.setEnabled(status);
        } else if (view instanceof ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) {
                updateAllMaterialButtons(group.getChildAt(i), status);
            }
        }
    }

    private void runRootCommand(String cmd) {
        if (NyaApplication.rootUtil.runRootCommandWithResult(cmd))
            dismiss();
        else
            Toast.makeText(context, R.string.exec_fail, Toast.LENGTH_SHORT).show();
    }

    private void funcRoot(int actionId) {
        switch (actionId) {
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

    private void funcShizuku(int actionId) throws IOException {
        if (!Shizuku.pingBinder()) {
            Toast.makeText(context, R.string.shizuku_not_run, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!NyaApplication.shizukuUtil.checkShizukuPermission()) {
            Toast.makeText(context, R.string.shizuku_denied, Toast.LENGTH_SHORT).show();
            return;
        }
        switch (actionId) {
            case 0:
                int exitCode = NyaApplication.shizukuUtil.shizukuProcess(new String[]{"input", "keyevent", "KEYCODE_POWER"});
                if (exitCode == 0)
                    dismiss();
                else
                    Toast.makeText(context, R.string.exec_fail, Toast.LENGTH_SHORT).show();
                break;
            case 1:
                NyaApplication.shizukuUtil.shizukuReboot(false, null);
                break;
            case 2:
                NyaApplication.shizukuUtil.shizukuReboot(false, "userspace");
                break;
            case 3:
                int exitCode2 = NyaApplication.shizukuUtil.shizukuProcess(new String[]{"pkill", "-f", "com.android.systemui"});
                if (exitCode2 == 0)
                    dismiss();
                else
                    Toast.makeText(context, R.string.exec_fail, Toast.LENGTH_SHORT).show();
                break;
            case 4:
                NyaApplication.shizukuUtil.shizukuReboot(false, "recovery");
                break;
            case 5:
                NyaApplication.shizukuUtil.shizukuReboot(false, "bootloader");
                break;
            case 6:
                NyaApplication.shizukuUtil.shizukuReboot(true, "safemode");
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
