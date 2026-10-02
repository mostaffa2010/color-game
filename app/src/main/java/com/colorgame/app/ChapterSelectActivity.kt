package com.colorgame.app

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.colorgame.app.model.LevelsManifest
import com.colorgame.app.ui.ChapterAdapter
import com.colorgame.app.utils.GamePreferences
import com.colorgame.app.utils.LevelLoader

class ChapterSelectActivity : AppCompatActivity() {

    private lateinit var rvChapters: RecyclerView
    private lateinit var gamePrefs: GamePreferences
    private lateinit var manifest: LevelsManifest

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chapter_select)

        gamePrefs = GamePreferences(this)
        rvChapters = findViewById(R.id.rvChapters)
        rvChapters.layoutManager = LinearLayoutManager(this)

        loadChapters()
    }

    override fun onResume() {
        super.onResume()
        // Refresh chapter/level progress status on return
        loadChapters()
    }

    private fun loadChapters() {
        try {
            manifest = LevelLoader.loadManifest(this)
            val adapter = ChapterAdapter(manifest.chapters, gamePrefs) { level ->
                MainActivity.launch(this, level.folder)
            }
            rvChapters.adapter = adapter
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to load chapters: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }
}
