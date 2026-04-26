package github.daisukikaffuchino.rebootnya.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import com.google.android.material.listitem.ListItemViewHolder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.radiobutton.MaterialRadioButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import github.daisukikaffuchino.rebootnya.R

class SettingsAdapter(
    sections: List<SettingsSection>,
    initialCommandText: String?,
    private val onCommandEditTextBound: (TextInputEditText) -> Unit,
    private val onCommandSubmit: (String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val items = sections.flatMap { section ->
        listOf(SettingsListItem.Header(section.title)) +
                section.rows.mapIndexed { index, row ->
                    SettingsListItem.Row(row, index, section.rows.size)
                }
    }
    var commandText: String? = initialCommandText
        private set
    private var commandEditText: TextInputEditText? = null

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is SettingsListItem.Header -> VIEW_TYPE_HEADER
            is SettingsListItem.Row -> when ((items[position] as SettingsListItem.Row).row) {
                SettingsRow.Command -> VIEW_TYPE_COMMAND
                is SettingsRow.Switch -> VIEW_TYPE_SWITCH
                else -> VIEW_TYPE_ROW
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_HEADER -> HeaderViewHolder(
                inflater.inflate(R.layout.item_settings_section_header, parent, false)
            )

            VIEW_TYPE_SWITCH -> SwitchViewHolder(
                inflater.inflate(R.layout.item_settings_switch_segmented, parent, false)
            )

            VIEW_TYPE_COMMAND -> CommandViewHolder(
                inflater.inflate(R.layout.item_settings_command_segmented, parent, false)
            )

            else -> RowViewHolder(
                inflater.inflate(R.layout.item_settings_segmented, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is SettingsListItem.Header -> (holder as HeaderViewHolder).bind(item)
            is SettingsListItem.Row -> when (holder) {
                is RowViewHolder -> holder.bind(item)
                is SwitchViewHolder -> holder.bind(item)
                is CommandViewHolder -> holder.bind(item)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun isRowWithNextRowInSameSection(position: Int): Boolean {
        val item = items.getOrNull(position) as? SettingsListItem.Row ?: return false
        val next = items.getOrNull(position + 1) as? SettingsListItem.Row ?: return false
        return item.indexInSection + 1 == next.indexInSection
    }

    fun clearCommandText() {
        commandText = null
        commandEditText?.text = null
    }

    private class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.settings_section_title)

        fun bind(item: SettingsListItem.Header) {
            title.text = item.title
        }
    }

    private open class BaseRowViewHolder(itemView: View) : ListItemViewHolder(itemView) {
        protected val card: MaterialCardView = itemView.findViewById(R.id.settings_item_card)
        protected val title: TextView = itemView.findViewById(R.id.settings_item_title)
        protected val summary: TextView? = itemView.findViewById(R.id.settings_item_summary)

        protected fun bindSegment(item: SettingsListItem.Row) {
            super.bind(item.indexInSection, item.sectionCount)
        }

        protected fun bindTexts(titleText: String, summaryText: CharSequence?, enabled: Boolean) {
            title.text = titleText
            summary?.let {
                it.text = summaryText
                it.visibility = if (summaryText.isNullOrBlank()) View.GONE else View.VISIBLE
            }
            itemView.isEnabled = enabled
            card.isEnabled = enabled
            card.alpha = if (enabled) 1f else 0.52f
        }
    }

    private class RowViewHolder(itemView: View) : BaseRowViewHolder(itemView) {
        fun bind(item: SettingsListItem.Row) {
            bindSegment(item)
            when (val row = item.row) {
                is SettingsRow.Choice -> bindChoice(row)
                is SettingsRow.StringChoice -> bindStringChoice(row)
                is SettingsRow.Action -> bindAction(row)
                else -> Unit
            }
        }

        private fun bindChoice(row: SettingsRow.Choice) {
            val selectedIndex = row.values.indexOf(row.selectedValue)
            val selectedEntry = row.entries.getOrNull(selectedIndex)
            bindTexts(row.title, selectedEntry, true)
            card.setOnClickListener {
                showChoiceSheet(
                    title = row.title,
                    entries = row.entries,
                    selectedIndex = selectedIndex,
                    onSelected = { index -> row.onSelected(row.values[index]) }
                )
            }
        }

        private fun bindStringChoice(row: SettingsRow.StringChoice) {
            val selectedIndex = row.values.indexOf(row.selectedValue)
            val selectedEntry = row.entries.getOrNull(selectedIndex)
            bindTexts(row.title, selectedEntry, true)
            card.setOnClickListener {
                showChoiceSheet(
                    title = row.title,
                    entries = row.entries,
                    selectedIndex = selectedIndex,
                    onSelected = { index -> row.onSelected(row.values[index]) }
                )
            }
        }

        private fun bindAction(row: SettingsRow.Action) {
            bindTexts(row.title, row.summary, row.enabled)
            card.setOnClickListener {
                if (row.enabled) row.onClick()
            }
        }

        private fun showChoiceSheet(
            title: String,
            entries: List<CharSequence>,
            selectedIndex: Int,
            onSelected: (Int) -> Unit
        ) {
            val context = itemView.context
            val dialog = BottomSheetDialog(context)
            val container = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(context.dp(24), context.dp(20), context.dp(24), context.dp(20))
            }

            container.addView(TextView(context).apply {
                text = title
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleLarge)
                setPadding(0, 0, 0, context.dp(12))
            })

            entries.forEachIndexed { index, entry ->
                val option = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    isClickable = true
                    isFocusable = true
                    setBackgroundResource(R.drawable.settings_bottom_sheet_option_background)
                    setPadding(0, context.dp(8), 0, context.dp(8))
                }
                val radioButton = MaterialRadioButton(context).apply {
                    text = entry
                    isChecked = index == selectedIndex
                    isClickable = false
                    setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge)
                }
                option.addView(radioButton)
                option.setOnClickListener {
                    onSelected(index)
                    summary?.text = entry
                    dialog.dismiss()
                }
                container.addView(option)
            }

            dialog.setContentView(container)
            dialog.show()
        }
    }

    private class SwitchViewHolder(itemView: View) : BaseRowViewHolder(itemView) {
        private val switch: MaterialSwitch = itemView.findViewById(R.id.settings_item_switch)
        private val staticSwitch: FrameLayout = itemView.findViewById(R.id.settings_item_switch_static)
        private val staticThumb: View = itemView.findViewById(R.id.settings_item_switch_static_thumb)

        fun bind(item: SettingsListItem.Row) {
            val row = item.row as SettingsRow.Switch
            bindSegment(item)
            bindTexts(row.title, row.summary, row.enabled)
            switch.setOnCheckedChangeListener(null)
            if (row.disableAnimation) {
                bindStaticSwitch(row)
            } else {
                staticSwitch.visibility = View.GONE
                switch.visibility = View.VISIBLE
                switch.isEnabled = row.enabled
                switch.isClickable = false
                switch.isFocusable = false
                switch.isChecked = row.checked
                switch.setOnCheckedChangeListener { _, checked -> row.onCheckedChange(checked) }
            }
            card.setOnClickListener {
                if (!row.enabled) return@setOnClickListener
                if (row.disableAnimation) {
                    row.onCheckedChange(!row.checked)
                } else {
                    switch.isChecked = !switch.isChecked
                }
            }
        }

        private fun bindStaticSwitch(row: SettingsRow.Switch) {
            switch.visibility = View.GONE
            staticSwitch.visibility = View.VISIBLE
            staticSwitch.alpha = if (row.enabled) 1f else 0.52f
            val context = itemView.context
            val track = staticSwitch.background.mutate() as? GradientDrawable
            track?.setColor(
                context.resolveThemeColor(
                    if (row.checked) {
                        androidx.appcompat.R.attr.colorPrimary
                    } else {
                        com.google.android.material.R.attr.colorSurfaceContainerHighest
                    }
                )
            )
            track?.setStroke(
                context.dp(2),
                if (row.checked) {
                    Color.TRANSPARENT
                } else {
                    context.resolveThemeColor(com.google.android.material.R.attr.colorOutline)
                }
            )
            staticSwitch.background = track

            val thumbParams = staticThumb.layoutParams as FrameLayout.LayoutParams
            thumbParams.gravity = if (row.checked) {
                Gravity.END or Gravity.CENTER_VERTICAL
            } else {
                Gravity.START or Gravity.CENTER_VERTICAL
            }
            staticThumb.layoutParams = thumbParams
            staticThumb.setBackgroundResource(
                if (row.checked) {
                    R.drawable.settings_switch_static_thumb
                } else {
                    R.drawable.settings_switch_static_thumb_unchecked
                }
            )
            staticThumb.backgroundTintList = ColorStateList.valueOf(
                context.resolveThemeColor(
                    if (row.checked) {
                        com.google.android.material.R.attr.colorOnPrimary
                    } else {
                        com.google.android.material.R.attr.colorOutline
                    }
                )
            )
        }
    }

    private inner class CommandViewHolder(itemView: View) : ListItemViewHolder(itemView) {
        private val inputLayout: TextInputLayout =
            itemView.findViewById(R.id.item_cmd_text_input_layout)
        private val editText: TextInputEditText =
            itemView.findViewById(R.id.item_cmd_text_input_edit)
        private var watcher: TextWatcher? = null

        fun bind(item: SettingsListItem.Row) {
            super.bind(item.indexInSection, item.sectionCount)
            commandEditText = editText
            onCommandEditTextBound(editText)
            inputLayout.setEndIconOnClickListener {
                val command = editText.text.toString()
                if (command.isNotBlank()) onCommandSubmit(command)
            }
            inputLayout.setEndIconOnLongClickListener {
                clearCommandText()
                true
            }
            watcher?.let(editText::removeTextChangedListener)
            editText.setText(commandText)
            watcher = object : TextWatcher {
                override fun afterTextChanged(editable: Editable?) {
                    commandText = editable?.toString()
                }

                override fun beforeTextChanged(
                    text: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) = Unit

                override fun onTextChanged(
                    text: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) = Unit
            }
            editText.addTextChangedListener(watcher)
        }
    }

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_ROW = 1
        private const val VIEW_TYPE_SWITCH = 2
        private const val VIEW_TYPE_COMMAND = 3
    }
}

class SettingsItemSpacingDecoration(context: Context) : RecyclerView.ItemDecoration() {
    private val itemSpacing = (context.resources.displayMetrics.density).toInt().coerceAtLeast(1)

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val adapter = parent.adapter as? SettingsAdapter ?: return
        val position = parent.getChildAdapterPosition(view)
        if (position == RecyclerView.NO_POSITION) return
        if (adapter.isRowWithNextRowInSameSection(position)) {
            outRect.bottom = itemSpacing
        }
    }
}

private fun Context.dp(value: Int): Int {
    return (value * resources.displayMetrics.density).toInt()
}

private fun Context.resolveThemeColor(attr: Int): Int {
    val typedArray = obtainStyledAttributes(intArrayOf(attr))
    val color = typedArray.getColor(0, 0)
    typedArray.recycle()
    return color
}

private sealed class SettingsListItem {
    data class Header(val title: String) : SettingsListItem()
    data class Row(
        val row: SettingsRow,
        val indexInSection: Int,
        val sectionCount: Int
    ) : SettingsListItem()
}
