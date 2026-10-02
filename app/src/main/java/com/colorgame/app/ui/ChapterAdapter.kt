package com.colorgame.app.ui

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.colorgame.app.R
import com.colorgame.app.model.Chapter
import com.colorgame.app.model.ChapterLevel
import com.colorgame.app.utils.GamePreferences

class ChapterAdapter(
    private val chapters: List<Chapter>,
    private val gamePrefs: GamePreferences,
    private val onLevelClick: (ChapterLevel) -> Unit
) : RecyclerView.Adapter<ChapterAdapter.ChapterViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChapterViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chapter_card, parent, false)
        return ChapterViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChapterViewHolder, position: Int) {
        holder.bind(chapters[position])
    }

    override fun getItemCount(): Int = chapters.size

    inner class ChapterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvChapterBadge: TextView = itemView.findViewById(R.id.tvChapterBadge)
        private val tvChapterTitle: TextView = itemView.findViewById(R.id.tvChapterTitle)
        private val tvChapterDescription: TextView = itemView.findViewById(R.id.tvChapterDescription)
        private val levelsContainer: LinearLayout = itemView.findViewById(R.id.levelsContainer)

        fun bind(chapter: Chapter) {
            tvChapterBadge.text = "CHAPTER ${chapter.number}"
            tvChapterTitle.text = chapter.title
            tvChapterDescription.text = chapter.description

            levelsContainer.removeAllViews()
            val inflater = LayoutInflater.from(itemView.context)

            chapter.levels.forEach { level ->
                val isUnlocked = gamePrefs.isLevelUnlocked(level.id, level.isUnlocked)
                val isCompleted = gamePrefs.isLevelCompleted(level.id)

                val levelView = LinearLayout(itemView.context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 8, 0, 8)
                    }
                    setPadding(12, 12, 12, 12)
                    setBackgroundResource(R.drawable.bg_card)
                    elevation = 2f
                }

                // Level preview circle
                val previewView = View(itemView.context).apply {
                    layoutParams = LinearLayout.LayoutParams(40, 40).apply {
                        setMargins(0, 0, 16, 0)
                    }
                    val drawable = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(try { Color.parseColor(level.previewColor) } catch (e: Exception) { Color.GRAY })
                    }
                    background = drawable
                }
                levelView.addView(previewView)

                // Level title and status
                val infoLayout = LinearLayout(itemView.context).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f)
                }

                val titleTv = TextView(itemView.context).apply {
                    text = level.title
                    textSize = 15f
                    setTextColor(Color.parseColor("#212121"))
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
                infoLayout.addView(titleTv)

                val statusTv = TextView(itemView.context).apply {
                    text = when {
                        isCompleted -> "✓ Completed"
                        isUnlocked -> "${level.totalColors} Colors"
                        else -> "🔒 Locked"
                    }
                    textSize = 12f
                    setTextColor(if (isCompleted) Color.parseColor("#2E7D32") else Color.parseColor("#757575"))
                }
                infoLayout.addView(statusTv)
                levelView.addView(infoLayout)

                // Play / Action Button
                val actionBtn = com.google.android.material.button.MaterialButton(itemView.context).apply {
                    text = if (isCompleted) "Replay" else "Color"
                    isEnabled = isUnlocked
                    textSize = 12f
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    setOnClickListener {
                        if (isUnlocked) {
                            onLevelClick(level)
                        }
                    }
                }
                levelView.addView(actionBtn)

                levelsContainer.addView(levelView)
            }
        }
    }
}
