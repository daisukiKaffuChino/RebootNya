package github.daisukikaffuchino.rebootnya.adapter

import android.content.Context
import android.content.res.ColorStateList
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


class HomeListAdapter(
    private val context: Context,
    private val items: List<String>,
    clickListenerInterface: OnItemClickListener
) : BaseAdapter() {
    val translucentColor: Int
    val clickListener: OnItemClickListener = clickListenerInterface

    init {
        val button = MaterialButton(context)
        val baseColor =
            MaterialColors.getColor(button, com.google.android.material.R.attr.colorSecondaryContainer)

        translucentColor = ColorUtils.setAlphaComponent(
            baseColor,
            150
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
            view = LayoutInflater.from(context).inflate(R.layout.item_home_list, parent, false)
            holder = ViewHolder(view)
            view.tag = holder
        } else {
            view = convertView
            holder = view.tag as ViewHolder
        }

        val title = getItem(position)
        holder.btn.text = title
        holder.btn.backgroundTintList = ColorStateList.valueOf(translucentColor)
        //holder.btn.setTextColor(translucentTextColor)
        holder.btn.setOnClickListener { view -> clickListener.onClick(position) }

        when (position) {
            0 -> {
                holder.btn.shapeAppearanceModel = ShapeAppearanceModel()
                    .toBuilder()
                    .setTopLeftCorner(CornerFamily.ROUNDED, 48f)
                    .setTopRightCorner(CornerFamily.ROUNDED, 48f)
                    .setBottomLeftCorner(CornerFamily.ROUNDED, 12f)
                    .setBottomRightCorner(CornerFamily.ROUNDED, 12f)
                    .build()
            }
            items.size - 1 -> {
                holder.btn.shapeAppearanceModel = ShapeAppearanceModel()
                    .toBuilder()
                    .setTopLeftCorner(CornerFamily.ROUNDED, 12f)
                    .setTopRightCorner(CornerFamily.ROUNDED, 12f)
                    .setBottomLeftCorner(CornerFamily.ROUNDED, 48f)
                    .setBottomRightCorner(CornerFamily.ROUNDED, 48f)
                    .build()
            }
            else -> {
                holder.btn.shapeAppearanceModel = ShapeAppearanceModel()
                    .toBuilder()
                    .setAllCorners(CornerFamily.ROUNDED, 12f)
                    .build()
            }
        }

        return view
    }

    interface OnItemClickListener {
        fun onClick(pos: Int)
    }
}