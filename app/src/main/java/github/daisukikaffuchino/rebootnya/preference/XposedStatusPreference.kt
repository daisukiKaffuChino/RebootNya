package github.daisukikaffuchino.rebootnya.preference

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.google.android.material.card.MaterialCardView
import github.daisukikaffuchino.rebootnya.R

class XposedStatusPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : Preference(context, attrs) {

    var isModuleRunning: Boolean = false
        set(value) {
            field = value
            notifyChanged()
        }

    var statusMessage: CharSequence? = null
        set(value) {
            field = value
            notifyChanged()
        }

    init {
        layoutResource = R.layout.preference_xposed_status
        isSelectable = false
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        val cardView = holder.itemView.findViewById<MaterialCardView>(R.id.xposed_status_card)
        val iconView = holder.itemView.findViewById<ImageView>(R.id.xposed_status_icon)
        val badgeView = holder.itemView.findViewById<TextView>(R.id.xposed_status_badge)
        val titleView = holder.itemView.findViewById<TextView>(R.id.xposed_status_title)
        val summaryView = holder.itemView.findViewById<TextView>(R.id.xposed_status_summary)

        titleView.text = title
        summaryView.text = statusMessage ?: summary

        if (isModuleRunning) {
            cardView.setCardBackgroundColor(resolveColor(R.color.md_theme_secondaryContainer))
            iconView.setImageResource(R.drawable.taffy_ok)
            badgeView.text = context.getString(R.string.xposed_status_running)
            badgeView.setBackgroundColor(resolveColor(R.color.md_theme_primary))
            badgeView.setTextColor(resolveColor(R.color.md_theme_onPrimary))
        } else {
            cardView.setCardBackgroundColor(resolveColor(R.color.md_theme_errorContainer))
            iconView.setImageResource(R.drawable.taffy_no)
            badgeView.text = context.getString(R.string.xposed_status_stopped)
            badgeView.setBackgroundColor(resolveColor(R.color.md_theme_error))
            badgeView.setTextColor(resolveColor(R.color.md_theme_onError))
        }
    }

    private fun resolveColor(colorResId: Int): Int {
        return ContextCompat.getColor(context, colorResId)
    }
}
