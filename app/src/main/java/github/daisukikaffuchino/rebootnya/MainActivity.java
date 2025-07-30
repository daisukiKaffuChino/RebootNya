package github.daisukikaffuchino.rebootnya;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Window;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.color.DynamicColors;

import github.daisukikaffuchino.rebootnya.databinding.ActivityMainBinding;
import rikka.shizuku.Shizuku;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        if (window != null)
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        SharedPreferences sp = getSharedPreferences("Nya", Context.MODE_PRIVATE);
        if (sp.getBoolean("monet", false))
            DynamicColors.applyToActivityIfAvailable(this);

        ActivityMainBinding binding;
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

    }

    public static boolean checkShizukuPermission() {
        if (!Shizuku.pingBinder())
            return false;

        if (Shizuku.isPreV11()) {
            Toast.makeText(NyaApplication.context, R.string.shizuku_too_old, Toast.LENGTH_SHORT).show();
            return false;
        }

        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else if (Shizuku.shouldShowRequestPermissionRationale()) {
            Toast.makeText(NyaApplication.context, R.string.shizuku_denied, Toast.LENGTH_SHORT).show();
            return false;
        } else {
            return false;
        }
    }
}