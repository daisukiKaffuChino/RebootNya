package github.daisukikaffuchino.rebootnya.preference


import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import android.view.View
import androidx.annotation.ArrayRes
import androidx.annotation.StyleableRes
import androidx.core.content.res.TypedArrayUtils
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import rikka.preference.simplemenu.R
import rikka.preference.simplemenu.SimpleMenuPopupWindow

/**
 * a [rikka.preference.SimpleMenuPreference] to implement night mode in user interface settings.
 * a [rikka.preference.SimpleMenuPreference] which use integer values array as entryValues.
 *
 * @author Haruue Icymoon haruue@caoyue.com.cn
 */
@SuppressLint("RestrictedApi", "UseKtx")
class IntegerSimpleMenuPreference @SuppressLint("RestrictedApi") constructor(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int,
    defStyleRes: Int
) : Preference(context, attrs, defStyleAttr, defStyleRes) {
    private val mPopupWindow: SimpleMenuPopupWindow?
    private var mAnchor: View? = null
    private var mItemView: View? = null

    private var mEntries: Array<CharSequence?>?
    /**
     * Returns the array of values to be saved for the preference.
     *
     * @return The array of values.
     */
    /**
     * The array to find the value to save for a preference when an entry from
     * entries is selected. If a user clicks on the second item in entries, the
     * second item in this array will be saved to the preference.
     *
     * @param entryValues The array to be used as values to save for the preference.
     */
    var entryValues: IntArray?
    private var mValue = 0
    private var mSummary: String?
    private var mValueSet = false

    init {
        var a = context.obtainStyledAttributes(
            attrs, R.styleable.ListPreference, defStyleAttr, defStyleRes
        )

        mEntries = TypedArrayUtils.getTextArray(
            a, R.styleable.ListPreference_entries,
            R.styleable.ListPreference_android_entries
        )

        this.entryValues = getIntArray(
            a, R.styleable.ListPreference_entryValues,
            R.styleable.ListPreference_android_entryValues
        )

        a.recycle()

        /* Retrieve the Preference summary attribute since it's private
         * in the Preference class.
         */
        a = context.obtainStyledAttributes(
            attrs,
            R.styleable.Preference, defStyleAttr, defStyleRes
        )

        mSummary = TypedArrayUtils.getString(
            a, R.styleable.Preference_summary,
            R.styleable.Preference_android_summary
        )

        a.recycle()

        a = context.obtainStyledAttributes(
            attrs, R.styleable.SimpleMenuPreference, defStyleAttr, defStyleRes
        )

        val popupStyle = a.getResourceId(
            R.styleable.SimpleMenuPreference_android_popupMenuStyle,
            R.style.Widget_Preference_SimpleMenuPreference_PopupMenu
        )
        val popupTheme = a.getResourceId(
            R.styleable.SimpleMenuPreference_android_popupTheme,
            R.style.ThemeOverlay_Preference_SimpleMenuPreference_PopupMenu
        )
        val popupContext = if (popupTheme != 0) {
            ContextThemeWrapper(context, popupTheme)
        } else {
            context
        }
        mPopupWindow = SimpleMenuPopupWindow(
            popupContext,
            attrs,
            R.styleable.SimpleMenuPreference_android_popupMenuStyle,
            popupStyle
        )
        mPopupWindow.onItemClickListener = SimpleMenuPopupWindow.OnItemClickListener { i: Int ->
            val value = this.entryValues!![i]
            if (callChangeListener(value)) this.value = value
        }

        a.recycle()
    }

    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = R.attr.simpleMenuPreferenceStyle
    ) : this(context, attrs, defStyleAttr, R.style.Preference_SimpleMenuPreference)

    override fun onClick() {
        if (this.entries == null || this.entries!!.isEmpty()) return

        if (mPopupWindow == null) return

        mPopupWindow.setEntries(this.entries)
        mPopupWindow.setSelectedIndex(findIndexOfValue(this.value))

        val container = mItemView!! // itemView
            .parent as View? // -> list (RecyclerView)

        mPopupWindow.show(mItemView, container, mAnchor!!.x.toInt())
    }

    /**
     * @param entriesResId The entries array as a resource.
     * @see .setEntries
     */
    fun setEntries(@ArrayRes entriesResId: Int) {
        this.entries = context.resources.getTextArray(entriesResId)
    }

    var entries: Array<CharSequence?>?
        /**
         * The list of entries to be shown in the list in subsequent dialogs.
         *
         * @return The list as an array.
         */
        get() = mEntries
        /**
         * Sets the human-readable entries to be shown in the list. This will be
         * shown in subsequent dialogs.
         *
         *
         * Each entry must have a corresponding index in
         * [.setEntryValues].
         *
         * @param entries The entries.
         * @see .setEntryValues
         */
        set(entries) {
            mEntries = entries

            mPopupWindow!!.requestMeasure()
        }

    /**
     * @param entryValuesResId The entry values array as a resource.
     * @see .setEntryValues
     */
    fun setEntryValues(@ArrayRes entryValuesResId: Int) {
        this.entryValues = context.resources.getIntArray(entryValuesResId)
    }

    /**
     * Returns the summary of this ListPreference. If the summary
     * has a [String formatting][java.lang.String.format]
     * marker in it (i.e. "%s" or "%1$s"), then the current entry
     * value will be substituted in its place.
     *
     * @return the summary with appropriate string substitution
     */
    override fun getSummary(): CharSequence? {
        val entry = this.entry
        return if (mSummary == null)
            super.getSummary()
        else
            String.format(mSummary!!, entry ?: "")
    }

    /**
     * Sets the summary for this Preference with a CharSequence.
     * If the summary has a
     * [String formatting][java.lang.String.format]
     * marker in it (i.e. "%s" or "%1$s"), then the current entry
     * value will be substituted in its place when it's retrieved.
     *
     * @param summary The summary for the preference.
     */
    override fun setSummary(summary: CharSequence?) {
        super.setSummary(summary)
        if (summary == null && mSummary != null)
            mSummary = null
        else if (summary != null && summary != mSummary)
            mSummary = summary.toString()
    }

    var value: Int
        /**
         * Returns the value of the key. This should be one of the entries in
         * [.getEntryValues].
         *
         * @return The value of the key.
         */
        get() = mValue
        /**
         * Sets the value of the key. This should be one of the entries in
         * [.getEntryValues].
         *
         * @param value The value to set for the key.
         */
        set(value) {
            // Always persist/notify the first time.
            val changed = mValue != value
            if (changed || !mValueSet) {
                mValue = value
                mValueSet = true
                persistInt(value)
                if (changed) notifyChanged()
            }
        }

    val entry: CharSequence?
        /**
         * Returns the entry corresponding to the current value.
         *
         * @return The entry corresponding to the current value, or null.
         */
        get() {
            val index = this.valueIndex
            return if (index >= 0 && mEntries != null) mEntries!![index] else null
        }

    /**
     * Returns the index of the given value (in the entry values array).
     *
     * @param value The value whose index should be returned.
     * @return The index of the value, or -1 if not found.
     */
    fun findIndexOfValue(value: Int): Int {
        if (this.entryValues != null) {
            for (i in entryValues!!.indices.reversed()) {
                val entryValue = this.entryValues!![i]
                if (entryValue == value) return i
            }
        }
        return -1
    }

    private var valueIndex: Int
        get() = findIndexOfValue(mValue)
        set(index) {
            if (this.entryValues != null) this.value = this.entryValues!![index]
        }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any? {
        return a.getInt(index, 1)
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        var defaultValue = defaultValue
        if (defaultValue == null) {
            defaultValue = 0
        }
        this.value = getPersistedInt(defaultValue as Int)
    }

    override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState()
        if (isPersistent) {
            // No need to save instance state since it's persistent
            return superState
        }

        val myState = SavedState(superState)
        myState.value = this.value
        return myState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state == null || state.javaClass != SavedState::class.java) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state)
            return
        }

        val myState = state as SavedState
        super.onRestoreInstanceState(myState.superState)
        this.value = myState.value
    }

    private class SavedState : BaseSavedState {
        var value: Int = 0

        override fun writeToParcel(dest: Parcel, flags: Int) {
            super.writeToParcel(dest, flags)
            dest.writeInt(value)
        }

        constructor(superState: Parcelable?) : super(superState)

    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        mItemView = holder.itemView
        mAnchor = holder.itemView.findViewById(android.R.id.empty)

        checkNotNull(mAnchor) {
            "SimpleMenuPreference item layout must contain" +
                    "a view id is android.R.id.empty to support iconSpaceReserved"
        }
    }

    companion object {
        @SuppressLint("RestrictedApi")
        private fun getIntArray(
            a: TypedArray, @StyleableRes index: Int,
            @StyleableRes fallbackIndex: Int
        ): IntArray {
            val resourceId = TypedArrayUtils.getResourceId(a, index, fallbackIndex, 0)
            return a.resources.getIntArray(resourceId)
        }
    }
}