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
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.ButtonBarLayout
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import github.daisukikaffuchino.rebootnya.MainActivity
import github.daisukikaffuchino.rebootnya.R
import github.daisukikaffuchino.rebootnya.SettingsActivity
import github.daisukikaffuchino.rebootnya.adapter.HomeRecyclerAdapter
import github.daisukikaffuchino.rebootnya.data.HomeListItemData
import github.daisukikaffuchino.rebootnya.data.ListItemEnum
import github.daisukikaffuchino.rebootnya.shizuku.NyaShellManager
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
        // Restore last selected item
        val lastSelected = NyaSettings.getLastSelectedOption()
        if (lastSelected != null) {
            val index = items.indexOfFirst {
                ListItemEnum.fromLocalizedDisplayName(mContext, it).name == lastSelected
            }
            if (index != -1) {
                checkedItem = index
            }
        }
        builder.setSingleChoiceItems(items, checkedItem) { _, which -> checkedItem = which }

        val dialog = builder.create()
        return setupDialogButtons(dialog, items)
    }

    @SuppressLint("InflateParams", "StringFormatInvalid")
    private fun createMaterialButtonsDialog(): Dialog {
        val builder = MaterialAlertDialogBuilder(mContext)
        builder.setCustomTitle(layoutInflater.inflate(R.layout.dialog_custom_title, null))

        val recyclerView =
            layoutInflater.inflate(
                R.layout.fragment_home_recycler_view,
                null,
                false
            ) as RecyclerView
        recyclerView.setLayoutManager(LinearLayoutManager(mContext))

        val data: MutableList<HomeListItemData> = ArrayList()
        val items = getDisplayItems()

        // Restore last selected item
        val lastSelected = NyaSettings.getLastSelectedOption()
        if (lastSelected != null) {
            val index = items.indexOfFirst {
                ListItemEnum.fromLocalizedDisplayName(mContext, it).name == lastSelected
            }
            if (index != -1) {
                checkedItem = index
            }
        }

        for (i in 0..items.size - 1) {
            val itemData = HomeListItemData(
                items[i],
                i,
                items.size,
                i == checkedItem
            )
            data.add(itemData)
        }

        val adapter = HomeRecyclerAdapter(data) { position, item ->
            checkedItem = item.indexInSection
        }
        recyclerView.setAdapter(adapter)
        builder.setView(recyclerView)
        recyclerView.addItemDecoration(HomeRecyclerAdapter.MarginItemDecoration())

        val dialog = builder.create()
        return setupDialogButtons(dialog, items)
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
                doAction(ListItemEnum.fromDisplayName(data))
                dismiss()
            }, 1000)
        return loadingDialog.create()
    }

    @SuppressLint("RestrictedApi")
    private fun setupDialogButtons(dialog: AlertDialog, items: Array<String>): AlertDialog {
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.confirm))
        { dialogInterface, i -> }
        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.close))
        { dialogInterface, i -> }
        dialog.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.setting))
        { dialogInterface, i -> }

        dialog.setOnShowListener { dialogInterface ->
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val neutralButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
            positiveButton.setOnClickListener { v: View? ->
                val itemEnum = ListItemEnum.fromLocalizedDisplayName(
                    mContext,
                    items[checkedItem]
                )
                NyaSettings.setLastSelectedOption(itemEnum.name)
                doAction(itemEnum)
            }
            neutralButton.setOnClickListener { v: View? ->
                val intent = Intent(mContext, SettingsActivity::class.java)
                mContext.startActivity(intent)
            }
            val decor = dialog.window?.decorView
            val buttonBar = findButtonBarLayout(decor)
            buttonBar?.setAllowStacking(false)
        }
        return dialog
    }

    @SuppressLint("RestrictedApi")
    fun findButtonBarLayout(root: View?): ButtonBarLayout? {
        if (root == null) return null
        if (root is ButtonBarLayout) return root
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                findButtonBarLayout(root.getChildAt(i))?.let { return it }
            }
        }
        return null
    }

    private fun getDisplayItems(): Array<String> {
        var itemList = ArrayList(listMap.keys)
        if (MainActivity.checkListFilterStatus()) {
            itemList = exclude(
                itemList,
                ListItemEnum.SAFE_MODE.getLocalizedDisplayName(mContext),
                ListItemEnum.SOFT_REBOOT.getLocalizedDisplayName(mContext)
            ) as ArrayList<String>
        }
        return itemList.toTypedArray()
    }

    private fun createListItemMap(): LinkedHashMap<String, ListItemEnum> {
        val map = LinkedHashMap<String, ListItemEnum>()
        for (item in ListItemEnum.entries) {
            map[item.getLocalizedDisplayName(mContext)] = item
        }
        return map
    }

    private fun doAction(listItemEnum: ListItemEnum?) {
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