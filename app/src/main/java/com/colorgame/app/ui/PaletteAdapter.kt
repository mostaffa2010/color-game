package com.colorgame.app.ui

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.colorgame.app.R
import com.colorgame.app.model.PaletteColor

class PaletteAdapter(
    private var colors: List<PaletteColor>,
    private val onColorSelected: (PaletteColor) -> Unit
) : RecyclerView.Adapter<PaletteAdapter.PaletteViewHolder>() {

    var selectedColorId: Int = 1
        private set

    fun updateData(newColors: List<PaletteColor>) {
        colors = newColors
        notifyDataSetChanged()
    }

    fun setSelectedColor(colorId: Int) {
        if (selectedColorId == colorId) return
        val prevId = selectedColorId
        selectedColorId = colorId

        val prevIndex = colors.indexOfFirst { it.id == prevId }
        val newIndex = colors.indexOfFirst { it.id == colorId }

        if (prevIndex != -1) notifyItemChanged(prevIndex)
        if (newIndex != -1) notifyItemChanged(newIndex)
    }

    fun notifyColorUpdated(colorId: Int) {
        val index = colors.indexOfFirst { it.id == colorId }
        if (index != -1) {
            notifyItemChanged(index)
        }
    }

    fun getNextIncompleteColorId(currentId: Int): Int? {
        val currentIndex = colors.indexOfFirst { it.id == currentId }
        if (currentIndex == -1) return null

        // Look forward
        for (i in (currentIndex + 1) until colors.size) {
            if (!colors[i].isCompleted) return colors[i].id
        }
        // Wrap around from beginning
        for (i in 0 until currentIndex) {
            if (!colors[i].isCompleted) return colors[i].id
        }
        return null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaletteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_palette_color, parent, false)
        return PaletteViewHolder(view)
    }

    override fun onBindViewHolder(holder: PaletteViewHolder, position: Int) {
        holder.bind(colors[position])
    }

    override fun getItemCount(): Int = colors.size

    inner class PaletteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val selectionRing: View = itemView.findViewById(R.id.selectionRing)
        private val colorCircle: View = itemView.findViewById(R.id.colorCircle)
        private val tvColorNumber: TextView = itemView.findViewById(R.id.tvColorNumber)
        private val ivCheckmark: ImageView = itemView.findViewById(R.id.ivCheckmark)
        private val tvRemainingCount: TextView = itemView.findViewById(R.id.tvRemainingCount)

        fun bind(paletteColor: PaletteColor) {
            val isSelected = paletteColor.id == selectedColorId
            val isCompleted = paletteColor.isCompleted

            // Outer selection ring
            selectionRing.visibility = if (isSelected) View.VISIBLE else View.GONE

            // Tint the circle with palette hex color
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(paletteColor.colorInt)
                if (isSelected) {
                    setStroke(4, Color.WHITE)
                } else {
                    setStroke(1, Color.parseColor("#BDBDBD"))
                }
            }
            colorCircle.background = drawable

            // Number text and contrasting color
            tvColorNumber.text = paletteColor.id.toString()
            tvColorNumber.setTextColor(paletteColor.textColor)

            if (isCompleted) {
                // Completed state
                tvColorNumber.visibility = View.GONE
                ivCheckmark.visibility = View.VISIBLE
                ivCheckmark.setColorFilter(paletteColor.textColor)
                tvRemainingCount.text = "✓"
                tvRemainingCount.setTextColor(Color.parseColor("#2E7D32"))
            } else {
                tvColorNumber.visibility = View.VISIBLE
                ivCheckmark.visibility = View.GONE
                tvRemainingCount.text = "${paletteColor.remainingRegions}"
                tvRemainingCount.setTextColor(Color.parseColor("#757575"))
            }

            itemView.setOnClickListener {
                if (selectedColorId != paletteColor.id) {
                    setSelectedColor(paletteColor.id)
                    onColorSelected(paletteColor)
                }
            }
        }
    }
}
