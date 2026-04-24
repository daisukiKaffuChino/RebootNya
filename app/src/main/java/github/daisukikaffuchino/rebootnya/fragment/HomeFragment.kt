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
import github.daisukikaffuchino.rebootnya.MainActivity.Companion.EXTRA_ACTION_ITEM
import github.daisukikaffuchino.rebootnya.R
import github.daisukikaffuchino.rebootnya.SettingsActivity
import github.daisukikaffuchino.rebootnya.adapter.HomeRecyclerAdapter
import github.daisukikaffuchino.rebootnya.data.HomeListItemData
import github.daisukikaffuchino.rebootnya.data.ListItemEnum
import github.daisukikaffuchino.rebootnya.shizuku.NyaShellManager
import github.daisukikaffuchino.rebootnya.utils.NyaSettings
import github.daisukikaffuchino.rebootnya.utils.RootUtil
import github.daisukikaffuchino.rebootnya.utils.ShizukuUtil
import github.daisukikaffuchino.rebootnya.utils.isSamsung
import kotlin.system.exitProcess

class HomeFragment : DialogFragment() {
    companion object {
        private val HIDDEN_FOR_LIMITED_SHIZUKU = setOf(
            ListItemEnum.SAFE_MODE,
            ListItemEnum.SOFT_REBOOT
        )
    }

    private lateinit var mContext: Context
    private var checkedItem = 0
    private lateinit var rootUtil: RootUtil
    private lateinit var shizukuUtil: ShizukuUtil

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val intent = requireActivity().intent
        if (intent != null && Intent.ACTION_RUN == intent.action) {
            return createLoadingDialog(intent)
        }

        return if (NyaSettings.getMainInterfaceStyle() == NyaSettings.STYLE.MATERIAL_BUTTON) {
            createMaterialButtonsDialog()
        } else {
            createClassicListDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mContext = requireActivity()
        rootUtil = RootUtil(mContext)
        shizukuUtil = ShizukuUtil(mContext)
    }

    override fun onResume() {
        super.onResume()
        if (NyaSettings.getShizukuShellMode() == NyaSettings.MODE.USER_SERVICE) {
            NyaShellManager.bindService(shizukuUtil) { exitCode, _ ->
                Log.d("main", "bind $exitCode")
            }
        }
    }

    private fun createClassicListDialog(): Dialog {
        val items = prepareDisplayItems()
        val labels = items.map { it.getLocalizedDisplayName(mContext) }.toTypedArray()

        val dialog = MaterialAlertDialogBuilder(mContext)
            .setTitle(R.string.app_name)
            .setSingleChoiceItems(labels, checkedItem) { _, which -> checkedItem = which }
            .create()

        return setupDialogButtons(dialog, items)
    }

    @SuppressLint("InflateParams", "StringFormatInvalid")
    private fun createMaterialButtonsDialog(): Dialog {
        val items = prepareDisplayItems()
        val recyclerView = layoutInflater.inflate(
            R.layout.fragment_home_recycler_view,
            null,
            false
        ) as RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(mContext)

        val data = items.mapIndexed { index, item ->
            HomeListItemData(
                text = item.getLocalizedDisplayName(mContext),
                indexInSection = index,
                sectionCount = items.size,
                checked = index == checkedItem
            )
        }.toMutableList()

        recyclerView.adapter = HomeRecyclerAdapter(data) { _, item ->
            checkedItem = item.indexInSection
        }
        recyclerView.addItemDecoration(HomeRecyclerAdapter.MarginItemDecoration())

        val dialog = MaterialAlertDialogBuilder(mContext)
            .setCustomTitle(layoutInflater.inflate(R.layout.dialog_custom_title, null))
            .setView(recyclerView)
            .create()

        return setupDialogButtons(dialog, items)
    }

    @SuppressLint("InflateParams")
    private fun createLoadingDialog(intent: Intent): Dialog {
        val loadingDialog = MaterialAlertDialogBuilder(mContext)
            .setTitle(R.string.app_name)
            .setView(layoutInflater.inflate(R.layout.dialog_progress, null))
            .setCancelable(false)

        intent.getStringExtra(EXTRA_ACTION_ITEM)?.let { displayName ->
            Handler(Looper.getMainLooper()).postDelayed({
                doAction(ListItemEnum.fromDisplayName(displayName))
                dismiss()
            }, 1000)
        }

        return loadingDialog.create()
    }

    @SuppressLint("RestrictedApi")
    private fun setupDialogButtons(
        dialog: AlertDialog,
        items: List<ListItemEnum>
    ): AlertDialog {
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.confirm)) { _, _ -> }
        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.close)) { _, _ -> }
        dialog.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.setting)) { _, _ -> }

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val neutralButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL)

            if (NyaSettings.getMainInterfaceStyle() == NyaSettings.STYLE.CLASSIC_LIST) {
                dialog.listView.setItemChecked(checkedItem, true)
                dialog.listView.setSelection(checkedItem)
            }

            positiveButton.setOnClickListener {
                val itemEnum = items.getOrNull(checkedItem) ?: return@setOnClickListener
                NyaSettings.setLastSelectedOption(itemEnum.name)
                doAction(itemEnum)
            }
            neutralButton.setOnClickListener {
                mContext.startActivity(Intent(mContext, SettingsActivity::class.java))
            }

            findButtonBarLayout(dialog.window?.decorView)?.setAllowStacking(false)
        }
        return dialog
    }

    @SuppressLint("RestrictedApi")
    private fun findButtonBarLayout(root: View?): ButtonBarLayout? {
        if (root == null) return null
        if (root is ButtonBarLayout) return root
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                findButtonBarLayout(root.getChildAt(i))?.let { return it }
            }
        }
        return null
    }

    private fun prepareDisplayItems(): List<ListItemEnum> {
        val items = getAvailableItems()
        checkedItem = resolveCheckedItem(items)
        return items
    }

    private fun getAvailableItems(): List<ListItemEnum> {
        val shizukuFilterEnabled = MainActivity.checkListFilterStatus()
        val samsungDevice = isSamsung()

        return ListItemEnum.entries.filterNot { item ->
            (shizukuFilterEnabled && item in HIDDEN_FOR_LIMITED_SHIZUKU) ||
                    (samsungDevice && item == ListItemEnum.BOOTLOADER) ||
                    (!samsungDevice && item == ListItemEnum.SAMSUNG_DOWNLOAD)
        }
    }

    private fun resolveCheckedItem(items: List<ListItemEnum>): Int {
        val lastSelected = NyaSettings.getLastSelectedOption()
        val selectedIndex = items.indexOfFirst { it.name == lastSelected }
        if (selectedIndex >= 0) {
            return selectedIndex
        }

        if (lastSelected != null) {
            items.firstOrNull()?.let { NyaSettings.setLastSelectedOption(it.name) }
        }
        return 0
    }

    private fun doAction(listItemEnum: ListItemEnum) {
        if (NyaSettings.getWorkMode() == NyaSettings.MODE.ROOT) {
            funcRoot(listItemEnum)
        } else {
            funcShizuku(listItemEnum)
        }
    }

    private fun runRootCommand(cmd: String) {
        if (rootUtil.runRootCommandWithResult(cmd)) {
            dismiss()
        } else {
            Toast.makeText(mContext, R.string.exec_fail, Toast.LENGTH_SHORT).show()
        }
    }

    private fun funcRoot(listItemEnum: ListItemEnum) {
        when (listItemEnum) {
            ListItemEnum.LOCK_SCREEN -> runRootCommand("input keyevent KEYCODE_POWER")
            ListItemEnum.REBOOT -> runRootCommand("svc power reboot")
            ListItemEnum.SOFT_REBOOT -> runRootCommand("setprop ctl.restart zygote")
            ListItemEnum.SYSTEM_UI -> runRootCommand("pkill -f com.android.systemui")
            ListItemEnum.RECOVERY -> runRootCommand("svc power reboot recovery")
            ListItemEnum.BOOTLOADER -> runRootCommand("svc power reboot bootloader")
            ListItemEnum.SAMSUNG_DOWNLOAD -> runRootCommand("reboot download")
            ListItemEnum.SAFE_MODE -> {
                if (rootUtil.runRootCommandWithResult("setprop persist.sys.safemode 1")) {
                    runRootCommand("svc power reboot")
                }
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
                if (lockExitCode == 0) {
                    dismiss()
                }
            }

            ListItemEnum.REBOOT -> shizukuUtil.shizukuReboot(null)
            ListItemEnum.SOFT_REBOOT -> shizukuUtil.runShizukuCommand(
                arrayOf("setprop", "ctl.restart", "zygote"),
                true
            )

            ListItemEnum.SYSTEM_UI -> {
                val stopUiCode = shizukuUtil.runShizukuCommand(
                    arrayOf("am", "force-stop", "com.android.systemui"),
                    false
                )
                if (stopUiCode == 0) {
                    Toast.makeText(mContext, R.string.stop_system_ui_tip, Toast.LENGTH_LONG).show()
                    dismiss()
                }
            }

            ListItemEnum.RECOVERY -> shizukuUtil.runShizukuCommand(
                arrayOf("reboot", "recovery"),
                false
            )

            ListItemEnum.BOOTLOADER -> shizukuUtil.shizukuReboot("bootloader")

            ListItemEnum.SAMSUNG_DOWNLOAD -> shizukuUtil.runShizukuCommand(
                arrayOf("reboot", "download"),
                false
            )

            ListItemEnum.SAFE_MODE -> {
                val exitCode = shizukuUtil.runShizukuCommand(
                    arrayOf("setprop", "persist.sys.safemode", "1"),
                    true
                )
                if (exitCode == 0) {
                    shizukuUtil.shizukuReboot(null)
                } else {
                    Toast.makeText(mContext, R.string.exec_fail, Toast.LENGTH_SHORT).show()
                }
            }

            ListItemEnum.POWER_OFF -> {
                shizukuUtil.runShizukuCommand(arrayOf("reboot", "-p"), false)
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        NyaShellManager.unbindService()
        if (!requireActivity().isChangingConfigurations) {
            exitProcess(0)
        }
    }
}
