package github.daisukikaffuchino.rebootnya

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toDrawable
import com.google.android.material.color.DynamicColors
import github.daisukikaffuchino.rebootnya.databinding.ActivityMainBinding
import github.daisukikaffuchino.rebootnya.shizuku.NyaShellManager

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val window = getWindow()
        window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

        if (NyaApplication.sp.getBoolean("monet", false))
            DynamicColors.applyToActivityIfAvailable(this)

        val binding: ActivityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.getRoot())

        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            NyaShellManager.bindService { exitCode, message ->

            }
        }, 5000)

    }
}