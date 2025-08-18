package github.daisukikaffuchino.rebootnya.fragment;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.topjohnwu.superuser.Shell;

import java.util.Objects;

import github.daisukikaffuchino.rebootnya.NyaApplication;
import github.daisukikaffuchino.rebootnya.R;
import github.daisukikaffuchino.rebootnya.databinding.FragmentSettingsBinding;
import rikka.shizuku.Shizuku;

public class SettingsFragment extends DialogFragment {
    private static final int SHIZUKU_REQUEST_CODE = 1000;
    private final Shizuku.OnRequestPermissionResultListener REQUEST_PERMISSION_RESULT_LISTENER = this::onRequestPermissionsResult;
    private FragmentSettingsBinding binding;
    Context context;
    SharedPreferences sp = NyaApplication.sp;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(getLayoutInflater());
        context = requireActivity();
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle(R.string.setting);
        builder.setView(binding.getRoot());
        builder.setNegativeButton(R.string.close, null);
        return builder.create();
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onStart() {
        super.onStart();
        Shizuku.addRequestPermissionResultListener(REQUEST_PERMISSION_RESULT_LISTENER);
        String workingMode = sp.getString("work_mode", "Root");
        binding.textWorkMode.setText(workingMode);
        binding.textVerInfo.setText("RebootNya " + getAppVer() + "\n" + getString(R.string.click_request_pm));
        binding.textWorkStatus.setText(getString(R.string.not_working) + " " + workingMode);
        binding.taffy.setImageResource(R.drawable.taffy_no);
        binding.cardStatus.setOnClickListener(v -> {
            if (workingMode.equals("Root"))
                NyaApplication.rootUtil.requestRoot();
            else {
                if (!Shizuku.pingBinder()) {
                    Toast.makeText(context, R.string.shizuku_not_run, Toast.LENGTH_SHORT).show();
                    return;
                }
                Shizuku.requestPermission(SHIZUKU_REQUEST_CODE);
            }
        });
        binding.itemSwitchWorkMode.setOnClickListener(v -> {
            if (workingMode.equals("Root"))
                sp.edit().putString("work_mode", "Shizuku").apply();
            else
                sp.edit().putString("work_mode", "Root").apply();

            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
            int currentDestinationId = Objects.requireNonNull(navController.getCurrentDestination()).getId();
            navController.popBackStack(currentDestinationId, true);
            navController.navigate(currentDestinationId);
        });

        if (workingMode.equals("Root") && !Boolean.FALSE.equals(Shell.isAppGrantedRoot())) {
            setWorkingStatus(workingMode);
        } else if (workingMode.equals("Shizuku") && NyaApplication.shizukuUtil.checkShizukuPermission()) {
            setWorkingStatus(workingMode);
        }

        final String[] shizukuShellExecMode = {sp.getString("shizuku_shell_exec_mode", "Process")};
        binding.textShizukuExecMode.setText(shizukuShellExecMode[0]);
        binding.itemSwitchShizukuExecMode.setOnClickListener(v -> {
            if (shizukuShellExecMode[0].equals("Process"))
                shizukuShellExecMode[0] = "UserService";
            else
                shizukuShellExecMode[0] = "Process";
            binding.textShizukuExecMode.setText(shizukuShellExecMode[0]);
            sp.edit().putString("shizuku_shell_exec_mode", shizukuShellExecMode[0]).apply();
        });

        binding.switchTheme.setChecked(sp.getBoolean("monet", false));
        binding.switchTheme.setOnCheckedChangeListener((compoundButton, b) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                sp.edit().putBoolean("monet", b).apply();
                Toast.makeText(context, R.string.restart_app_effect, Toast.LENGTH_SHORT).show();
            } else {
                compoundButton.setChecked(!b);
                Toast.makeText(context, R.string.require_a12, Toast.LENGTH_SHORT).show();
            }
        });

        binding.itemCmdTextInputLayout.setEndIconOnClickListener(view -> {
            String edtText = Objects.requireNonNull(binding.itemCmdTextInputEdit.getText()).toString();
            if (edtText.isBlank()) return;
            if (workingMode.equals("Root") && !Boolean.FALSE.equals(Shell.isAppGrantedRoot())) {
                if (NyaApplication.rootUtil.runRootCommandWithResult(edtText)) {
                    Toast.makeText(context, "Success!", Toast.LENGTH_SHORT).show();
                    binding.itemCmdTextInputEdit.setText(null);
                } else Toast.makeText(context, R.string.exec_fail, Toast.LENGTH_SHORT).show();
            } else if (workingMode.equals("Shizuku") && NyaApplication.shizukuUtil.checkShizukuPermission()) {
                int exitCode = NyaApplication.shizukuUtil.runShizukuCommand(edtText.split("\\s+"),false);
                if (exitCode == 0) {
                    Toast.makeText(context, "Success!\nExit code: 0", Toast.LENGTH_SHORT).show();
                    binding.itemCmdTextInputEdit.setText(null);
                } else
                    Toast.makeText(context, getString(R.string.exec_fail) + "\nExit code: " + exitCode, Toast.LENGTH_SHORT).show();
            } else Toast.makeText(context, R.string.no_permission, Toast.LENGTH_SHORT).show();
        });

        String info = "<b>RebootNya Open-Source Project</b><br>" +
                "github@daisukiKaffuChino<br>" +
                "Apache-2.0 Licensed";
        binding.textAppInfo.setText(Html.fromHtml(info, Html.FROM_HTML_MODE_COMPACT));
        binding.itemAppInfo.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/daisukiKaffuChino/RebootNya"));
            startActivity(intent);
        });
    }

    private void onRequestPermissionsResult(int requestCode, int grantResult) {
        boolean granted = grantResult == PackageManager.PERMISSION_GRANTED;
        if (granted && sp.getString("work_mode", "Root").equals("Shizuku")
                && requestCode == SHIZUKU_REQUEST_CODE) {
            setWorkingStatus("Shizuku");
        }
    }

    private String getAppVer() {
        PackageManager manager = context.getPackageManager();
        try {
            PackageInfo info = manager.getPackageInfo(context.getPackageName(), 0);
            String versionName = info.versionName;
            long versionCode = info.getLongVersionCode();
            return versionName + " (" + versionCode + ")";
        } catch (PackageManager.NameNotFoundException e) {
            e.fillInStackTrace();
        }
        return "err";
    }

    @SuppressLint("SetTextI18n")
    private void setWorkingStatus(String workingMode) {
        String ver = getString(R.string.working) + " " + workingMode;
        if (workingMode.equals("Shizuku")) ver = ver + " " + Shizuku.getVersion();
        binding.textWorkStatus.setText(ver);
        binding.textVerInfo.setText("RebootNya " + getAppVer());
        binding.taffy.setImageResource(R.drawable.taffy_ok);
        binding.cardStatus.setClickable(false);
        binding.cardStatus.setOnClickListener(null);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Shizuku.removeRequestPermissionResultListener(REQUEST_PERMISSION_RESULT_LISTENER);
        binding = null;
    }
}