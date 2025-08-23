package github.daisukikaffuchino.rebootnya.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.core.graphics.ColorUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.ShapeAppearanceModel
import github.daisukikaffuchino.rebootnya.R
import kotlin.math.exp


class HomeListAdapter(
    private val context: Context,
    private val items: List<String>,
    private val clickListenerInterface: OnItemClickListener
) : BaseAdapter() {
    val translucentColor: Int
    val translucentTextColor: Int
    val clickListener: OnItemClickListener = clickListenerInterface

    init {
        val button = MaterialButton(context)
        val baseColor =
            MaterialColors.getColor(button, com.google.android.material.R.attr.colorPrimaryFixedDim)
        val textColor = MaterialColors.getColor(
            button,
            com.google.android.material.R.attr.colorOnPrimaryContainer
        )
        val baseLuminance = ColorUtils.calculateLuminance(baseColor)
        val textLuminance = ColorUtils.calculateLuminance(textColor)
        translucentColor = ColorUtils.setAlphaComponent(
            baseColor,
            ((if (isDarkTheme(context)) 0.08 else (0.2 + (baseLuminance - 0.5) * 0.2)) * 255).toInt()
        )
        translucentTextColor = ColorUtils.setAlphaComponent(
            textColor,
            ((if (isDarkTheme(context)) 0.94 else computeSigmoid(textLuminance)) * 255).toInt()
        )
    }

    private class ViewHolder(view: View) {
        val btn: MaterialButton = view.findViewById(R.id.item_home_btn)
    }

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): String = items[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View
        val holder: ViewHolder

        //复用
        if (convertView == null) {
            view = LayoutInflater.from(context).inflate(R.layout.list_home, parent, false)
            holder = ViewHolder(view)
            view.tag = holder
        } else {
            view = convertView
            holder = view.tag as ViewHolder
        }

        val title = getItem(position)
        holder.btn.text = title
        holder.btn.backgroundTintList = ColorStateList.valueOf(translucentColor)
        holder.btn.setTextColor(translucentTextColor)
        holder.btn.setOnClickListener { view -> clickListener!!.onClick(position) }

        if (position == 0) {
            holder.btn.shapeAppearanceModel = ShapeAppearanceModel()
                .toBuilder()
                .setTopLeftCorner(CornerFamily.ROUNDED, 32f)
                .setTopRightCorner(CornerFamily.ROUNDED, 32f)
                .setBottomLeftCorner(CornerFamily.ROUNDED, 6f)
                .setBottomRightCorner(CornerFamily.ROUNDED, 6f)
                .build()
        }

        if (position == items.size - 1) {
            holder.btn.shapeAppearanceModel = ShapeAppearanceModel()
                .toBuilder()
                .setTopLeftCorner(CornerFamily.ROUNDED, 6f)
                .setTopRightCorner(CornerFamily.ROUNDED, 6f)
                .setBottomLeftCorner(CornerFamily.ROUNDED, 32f)
                .setBottomRightCorner(CornerFamily.ROUNDED, 32f)
                .build()
        }

        return view
    }

    private fun isDarkTheme(context: Context): Boolean {
        return ((context.resources
            .configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES)
    }

    private fun computeSigmoid(x: Double): Double {
        val shift = 0.31
        val k = 10.0
        val a = 0.75
        val b = 0.19
        return a + b / (1 + exp(-k * (x - shift)))
    }

    interface OnItemClickListener {
        fun onClick(pos: Int)
    }
}