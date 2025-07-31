package github.daisukikaffuchino.rebootnya

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toDrawable
import com.google.android.material.color.DynamicColors
import github.daisukikaffuchino.rebootnya.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val window = getWindow()
        window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

        val sp = getSharedPreferences("Nya", MODE_PRIVATE)
        if (sp.getBoolean("monet", false)) DynamicColors.applyToActivityIfAvailable(this)

        val binding: ActivityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.getRoot())
    }

}