package github.daisukikaffuchino.rebootnya.adapter

import android.content.Context
import android.graphics.Rect
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.listitem.ListItemViewHolder
import github.daisukikaffuchino.rebootnya.NyaApplication
import github.daisukikaffuchino.rebootnya.R
import github.daisukikaffuchino.rebootnya.data.HomeListItemData

class HomeRecyclerAdapter(
    private val items: MutableList<HomeListItemData>,
    private val listener: OnItemSelectedListener? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var selectedPosition = 0
    private var recyclerView: RecyclerView? = null

    init {
        items.forEachIndexed { index, item ->
            item.checked = (index == selectedPosition)
        }
    }

    override fun onAttachedToRecyclerView(rv: RecyclerView) {
        super.onAttachedToRecyclerView(rv)
        recyclerView = rv
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_list_segmented_viewholder, parent, false)
        return CustomItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val data = getItemAt(position)
        (holder as CustomItemViewHolder).bind(data)
    }

    override fun getItemCount(): Int = items.size

    fun getItemAt(i: Int): HomeListItemData = items[i]

    private inner class CustomItemViewHolder(itemView: View) :
        ListItemViewHolder(itemView) {

        private val textView: TextView = itemView.findViewById(R.id.home_list_item_text)
        val cardView: MaterialCardView = itemView.findViewById(R.id.home_list_item_card)

        fun bind(data: HomeListItemData) {
            super.bind(data.indexInSection, data.sectionCount)
            textView.text = data.text

            cardView.isChecked = data.checked

            cardView.setOnClickListener {
                val old = selectedPosition
                val now = bindingAdapterPosition
                if (now == RecyclerView.NO_POSITION) return@setOnClickListener
                if (old == now) return@setOnClickListener // 点击同一个不处理

                // 1) 更新数据模型
                items[old].checked = false
                items[now].checked = true
                selectedPosition = now

                // 2) 当前点击的项：播放选中动画
                cardView.toggle()

                // 3) 旧的选中项：可见时用 toggle() 播放取消动画；否则刷新
                recyclerView?.let { rv ->
                    val oldVH = rv.findViewHolderForAdapterPosition(old)
                    if (oldVH is CustomItemViewHolder) {
                        oldVH.cardView.toggle()
                    } else {
                        notifyItemChanged(old)
                    }
                } ?: notifyItemChanged(old)

                // 4) 通知外部监听器
                listener?.onItemSelected(now, items[now])
            }
        }
    }

    class MarginItemDecoration() : RecyclerView.ItemDecoration() {
        private val itemMargin: Int = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            2f,
            NyaApplication.context.resources.displayMetrics
        ).toInt()

        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            val position = parent.getChildAdapterPosition(view)
            if (position != state.itemCount - 1) {
                outRect.bottom = itemMargin
            }
        }
    }

    fun interface OnItemSelectedListener {
        fun onItemSelected(position: Int, item: HomeListItemData)
    }
}
