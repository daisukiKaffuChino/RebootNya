package github.daisukikaffuchino.rebootnya

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import com.topjohnwu.superuser.Shell
import github.daisukikaffuchino.rebootnya.databinding.ActivitySettingsBinding
import github.daisukikaffuchino.rebootnya.fragment.SettingsFragment
import github.daisukikaffuchino.rebootnya.utils.NyaSettings
import github.daisukikaffuchino.rebootnya.utils.RootUtil
import github.daisukikaffuchino.rebootnya.utils.ShizukuUtil
import github.daisukikaffuchino.rebootnya.utils.getAppVer
import rikka.shizuku.Shizuku
import rikka.shizuku.Shizuku.OnRequestPermissionResultListener
import rikka.sui.Sui


class SettingsActivity : BaseActivity() {
    lateinit var binding: ActivitySettingsBinding
    private val requestPermissionResultListener =
        OnRequestPermissionResultListener { requestCode: Int, grantResult: Int ->
            this.onRequestPermissionsResult(
                requestCode,
                grantResult
            )
        }

    lateinit var context: Context
    //val sp: SharedPreferences = NyaSettings.getPreferences()

    companion object {
        const val SHIZUKU_REQUEST_CODE: Int = 1000
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        this.theme.applyStyle(
            rikka.material.preference.R.style.ThemeOverlay_Rikka_Material3_Preference,
            true
        )

        context = this
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.settingsToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.settings_fragment_container, SettingsFragment())
                .commit()
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onStart() {
        super.onStart()
        Shizuku.addRequestPermissionResultListener(requestPermissionResultListener)

        //val workingMode: String = sp.getString("work_mode", "Root")!!
        val workingMode = NyaSettings.getWorkMode()
        val isPermitted = when (workingMode) {
            NyaSettings.STORE.ROOT -> Shell.isAppGrantedRoot() != false
            NyaSettings.STORE.SHIZUKU -> ShizukuUtil(context).checkShizukuPermission()
            else -> false
        }
        if (isPermitted)
            setWorkingStatus(workingMode)
        else {
            binding.run {
                textVerInfo.text =
                    "RebootNya ${getAppVer(context)}\n${getString(R.string.click_request_pm)}"
                textWorkStatus.text = getString(R.string.not_working)
                taffy.setImageResource(R.drawable.taffy_no)

                cardStatus.setOnClickListener {
                    when (workingMode) {
                        NyaSettings.STORE.ROOT -> RootUtil(context).requestRoot()
                        NyaSettings.STORE.SHIZUKU -> {
                            if (!Shizuku.pingBinder()) {
                                Toast.makeText(
                                    context,
                                    R.string.shizuku_not_run,
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@setOnClickListener
                            }
                            Shizuku.requestPermission(SHIZUKU_REQUEST_CODE)
                        }
                    }
                }
            }
        }

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun onRequestPermissionsResult(requestCode: Int, grantResult: Int) {
        val granted = grantResult == PackageManager.PERMISSION_GRANTED
        if (granted && NyaSettings.getWorkMode()== NyaSettings.STORE.SHIZUKU
            && requestCode == SHIZUKU_REQUEST_CODE
        ) {
            setWorkingStatus(NyaSettings.STORE.SHIZUKU)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setWorkingStatus(workingMode: Int) {

        val statusText = when (workingMode) {
            NyaSettings.STORE.SHIZUKU -> {
                val shizukuVersion = Shizuku.getVersion()
                if (Sui.init(context.packageName)) {
                    "${getString(R.string.working)} <Sui - $shizukuVersion>"
                } else {
                    "${getString(R.string.working)} <Shizuku - $shizukuVersion>"
                }
            }

            else -> "${getString(R.string.working)} <Root>"
        }

        binding.apply {
            textWorkStatus.text = statusText
            textVerInfo.text = "RebootNya ${getAppVer(context)}"
            taffy.setImageResource(R.drawable.taffy_ok)
            cardStatus.setOnClickListener(null)
        }
    }

}