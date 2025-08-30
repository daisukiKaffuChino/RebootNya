package github.daisukikaffuchino.rebootnya.fragment

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import github.daisukikaffuchino.rebootnya.MainActivity
import github.daisukikaffuchino.rebootnya.R
import github.daisukikaffuchino.rebootnya.SettingsActivity
import github.daisukikaffuchino.rebootnya.adapter.HomeListAdapter
import github.daisukikaffuchino.rebootnya.shizuku.NyaShellManager
import github.daisukikaffuchino.rebootnya.utils.ListItemEnum
import github.daisukikaffuchino.rebootnya.utils.NyaSettings
import github.daisukikaffuchino.rebootnya.utils.RootUtil
import github.daisukikaffuchino.rebootnya.utils.ShizukuUtil
import github.daisukikaffuchino.rebootnya.utils.exclude
import java.io.IOException
import kotlin.system.exitProcess

class HomeFragment : DialogFragment() {
    private lateinit var mContext: Context
    private var checkedItem = 0
    private lateinit var rootUtil: RootUtil
    private lateinit var shizukuUtil: ShizukuUtil
    private lateinit var listMap: LinkedHashMap<String, ListItemEnum>

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val intent = requireActivity().intent
        if (intent != null && Intent.ACTION_RUN == intent.action) {
            return createLoadingDialog(intent)
        }

        return if (NyaSettings.getMainInterfaceStyle() == NyaSettings.STYLE.MATERIAL_BUTTON)
            createMaterialButtonsDialog()
         else
            createClassicListDialog()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mContext = requireActivity()
        rootUtil = RootUtil(mContext)
        shizukuUtil = ShizukuUtil(mContext)
        listMap = createListItemMap()
    }

    override fun onResume() {
        super.onResume()
        if (NyaSettings.getShizukuShellMode() == NyaSettings.MODE.USER_SERVICE)
            NyaShellManager.bindService(shizukuUtil) { exitCode, message ->
                Log.d("main", "bind $exitCode")
            }
    }

    private fun createClassicListDialog(): Dialog {
        val builder = MaterialAlertDialogBuilder(mContext)
        val items = getDisplayItems()

        builder.setTitle(R.string.app_name)
        builder.setSingleChoiceItems(items, checkedItem) { _, which -> checkedItem = which }

        builder.setPositiveButton(R.string.confirm, null)
        builder.setNegativeButton(R.string.close, null)
        builder.setNeutralButton(R.string.setting, null)

        val dialog = builder.create()
        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val neutralButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
            positiveButton.setOnClickListener {
                doAction(ListItemEnum.fromLocalizedDisplayName(mContext, items[checkedItem]))
            }
            neutralButton.setOnClickListener {
                val intent = Intent(mContext, SettingsActivity::class.java)
                mContext.startActivity(intent)
            }
        }
        return dialog
    }

    @SuppressLint("InflateParams")
    private fun createMaterialButtonsDialog(): Dialog {
        val builder = MaterialAlertDialogBuilder(mContext)
        val dialogView = layoutInflater.inflate(R.layout.fragment_home, null)
        val listView = dialogView.findViewById<ListView>(R.id.home_list_view)
        val items = getDisplayItems()
        val adapter = HomeListAdapter(
            mContext,
            items.toList(),
            object : HomeListAdapter.OnItemClickListener { // Explicitly implement the interface
                override fun onClick(pos: Int) {
                    doAction(ListItemEnum.fromLocalizedDisplayName(mContext, items[pos]))
                }
            }
        )

        listView.divider = null
        listView.adapter = adapter

        builder.setCustomTitle(layoutInflater.inflate(R.layout.dialog_custom_title, null))
        builder.setView(dialogView)

        val dialog = builder.setPositiveButton(R.string.close, null)
            .setNegativeButton(R.string.setting, null)
            .create()

        dialog.setOnShowListener {
            val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            negativeButton.setOnClickListener {
                val settingsIntent = Intent(mContext, SettingsActivity::class.java)
                mContext.startActivity(settingsIntent)
            }
        }
        return dialog
    }

    @SuppressLint("InflateParams")
    private fun createLoadingDialog(intent: Intent): Dialog {
        val loadingDialog = MaterialAlertDialogBuilder(mContext)
        loadingDialog.setTitle(R.string.app_name)
            .setView(layoutInflater.inflate(R.layout.dialog_progress, null))
            .setCancelable(false)

        val data = intent.getStringExtra("extra")
        if (data != null)
            Handler(Looper.getMainLooper()).postDelayed({
                doAction(ListItemEnum.fromDisplayName(data)) // Safe call for fromDisplayName
                dismiss()
            }, 1000)
        return loadingDialog.create()
    }

    private fun getDisplayItems(): Array<String> {
        var itemList = ArrayList(listMap.keys)
        if (MainActivity.checkListFilterStatus()) {
            itemList = exclude(
                itemList,
                ListItemEnum.SAFE_MODE.getLocalizedDisplayName(mContext),
                ListItemEnum.SOFT_REBOOT.getLocalizedDisplayName(mContext)
            ) as ArrayList<String> // Cast needed if NyaUtilKt.exclude returns List
        }
        return itemList.toTypedArray()
    }

    private fun createListItemMap(): LinkedHashMap<String, ListItemEnum> {
        val map = LinkedHashMap<String, ListItemEnum>()
        for (item in ListItemEnum.entries) { // Use .entries for enums in Kotlin
            map[item.getLocalizedDisplayName(mContext)] = item
        }
        return map
    }

    private fun doAction(listItemEnum: ListItemEnum?) { // Made listItemEnum nullable
        if (listItemEnum == null) {
            Log.e("HomeFragment", "doAction called with null ListItemEnum")
            Toast.makeText(mContext, R.string.exec_fail, Toast.LENGTH_SHORT).show()
            return
        }
        if (NyaSettings.getWorkMode() == NyaSettings.MODE.ROOT) {
            funcRoot(listItemEnum)
        } else {
            try {
                funcShizuku(listItemEnum)
            } catch (e: IOException) {
                e.fillInStackTrace()
                Toast.makeText(mContext, R.string.exec_fail, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun runRootCommand(cmd: String) {
        if (rootUtil.runRootCommandWithResult(cmd))
            dismiss()
         else
            Toast.makeText(mContext, R.string.exec_fail, Toast.LENGTH_SHORT).show()
    }

    private fun funcRoot(listItemEnum: ListItemEnum) {
        when (listItemEnum) {
            ListItemEnum.LOCK_SCREEN -> runRootCommand("input keyevent KEYCODE_POWER")
            ListItemEnum.REBOOT -> runRootCommand("svc power reboot")
            ListItemEnum.SOFT_REBOOT -> runRootCommand("setprop ctl.restart zygote")
            ListItemEnum.SYSTEM_UI -> runRootCommand("pkill -f com.android.systemui")
            ListItemEnum.RECOVERY -> runRootCommand("svc power reboot recovery")
            ListItemEnum.BOOTLOADER -> runRootCommand("svc power reboot bootloader")
            ListItemEnum.SAFE_MODE -> {
                if (rootUtil.runRootCommandWithResult("setprop persist.sys.safemode 1"))
                    runRootCommand("svc power reboot")
            }
            ListItemEnum.POWER_OFF -> runRootCommand("reboot -p")
        }
    }

    private fun funcShizuku(listItemEnum: ListItemEnum) {
        if (!shizukuUtil.checkShizukuPermission()) {
            Toast.makeText(mContext, R.string.shizuku_denied, Toast.LENGTH_SHORT).show()
            return
        }
        when (listItemEnum) {
            ListItemEnum.LOCK_SCREEN -> {
                val lockExitCode = shizukuUtil.runShizukuCommand(
                    arrayOf("input", "keyevent", "KEYCODE_POWER"),
                    false
                )
                if (lockExitCode == 0) dismiss()
            }

            ListItemEnum.REBOOT -> shizukuUtil.shizukuReboot(null)
            ListItemEnum.SOFT_REBOOT -> shizukuUtil.runShizukuCommand(
                arrayOf(
                    "setprop",
                    "ctl.restart",
                    "zygote"
                ), true
            )

            ListItemEnum.SYSTEM_UI -> {
                val stopUiCode = shizukuUtil.runShizukuCommand(
                    arrayOf(
                        "am",
                        "force-stop",
                        "com.android.systemui"
                    ), false
                )
                if (stopUiCode == 0) {
                    Toast.makeText(mContext, R.string.stop_system_ui_tip, Toast.LENGTH_LONG)
                        .show()
                    dismiss()
                }
            }

            ListItemEnum.RECOVERY -> shizukuUtil.runShizukuCommand(
                arrayOf("reboot", "recovery"),
                false
            )

            ListItemEnum.BOOTLOADER -> shizukuUtil.shizukuReboot("bootloader")
            ListItemEnum.SAFE_MODE -> {
                val exitCode = shizukuUtil.runShizukuCommand(
                    arrayOf("setprop", "persist.sys.safemode", "1"),
                    true
                )
                if (exitCode == 0)
                    shizukuUtil.shizukuReboot(null)
                 else
                    Toast.makeText(mContext, R.string.exec_fail, Toast.LENGTH_SHORT).show()
            }

            ListItemEnum.POWER_OFF -> shizukuUtil.runShizukuCommand(arrayOf("reboot", "-p"), false)
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        /*
        为什么使用 System.exit(0) ?
        因为 KernelSU 授权后需要杀死并重启进程使权限生效。
        通常 activity.finish() 仅限于关闭活动，进程由系统决定回收。
        对于像本项目这样的单线程应用，这种做法是安全的。
         */
        NyaShellManager.unbindService()
        if (!requireActivity().isChangingConfigurations)
            exitProcess(0)
    }

}