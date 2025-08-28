package github.daisukikaffuchino.rebootnya.preference

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import github.daisukikaffuchino.rebootnya.R

class NekoPreference (context: Context, attrs: AttributeSet?) : Preference(
    context, attrs
) {
    init {
        layoutResource = R.layout.preference_neko
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val img =
            holder.itemView.findViewById<ImageView>(R.id.preference_neko_icon)
        img.setOnLongClickListener { view ->
            Toast.makeText(context, "Nya~", Toast.LENGTH_SHORT).show()
            true }
    }
}