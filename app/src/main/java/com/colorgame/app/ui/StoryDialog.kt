package com.colorgame.app.ui

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.Window
import android.widget.TextView
import com.colorgame.app.R
import com.google.android.material.button.MaterialButton

object StoryDialog {

    fun show(
        context: Context,
        chapterText: String,
        titleText: String,
        storyBodyText: String,
        buttonText: String,
        onAction: () -> Unit
    ) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val view = LayoutInflater.from(context).inflate(R.layout.dialog_story, null)
        dialog.setContentView(view)

        val tvChapter: TextView = view.findViewById(R.id.tvStoryChapter)
        val tvTitle: TextView = view.findViewById(R.id.tvStoryTitle)
        val tvBody: TextView = view.findViewById(R.id.tvStoryBody)
        val btnAction: MaterialButton = view.findViewById(R.id.btnStoryAction)

        tvChapter.text = chapterText
        tvTitle.text = titleText
        tvBody.text = storyBodyText
        btnAction.text = buttonText

        btnAction.setOnClickListener {
            dialog.dismiss()
            onAction()
        }

        dialog.setCancelable(true)
        dialog.show()
    }
}
