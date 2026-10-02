package com.colorgame.app

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.colorgame.app.model.LevelData
import com.colorgame.app.model.RegionItem
import com.colorgame.app.ui.ColoringCanvasView
import com.colorgame.app.ui.PaletteAdapter
import com.colorgame.app.ui.StoryDialog
import com.colorgame.app.utils.GamePreferences
import com.colorgame.app.utils.LevelLoader
import com.colorgame.app.utils.SoundHelper
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity(), ColoringCanvasView.OnColoringListener {

    private lateinit var canvasView: ColoringCanvasView
    private lateinit var rvPalette: RecyclerView
    private lateinit var paletteAdapter: PaletteAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var tvProgressPercent: TextView
    private lateinit var tvLevelTitle: TextView
    private lateinit var tvChapterSubtitle: TextView
    private lateinit var btnHint: MaterialButton
    private lateinit var btnBack: ImageButton
    private lateinit var btnZoomIn: FloatingActionButton
    private lateinit var btnZoomOut: FloatingActionButton

    private lateinit var gamePrefs: GamePreferences
    private lateinit var soundHelper: SoundHelper

    private var levelFolder: String = "bakery_01"
    private var levelData: LevelData? = null
    private var linesBitmap: Bitmap? = null
    private var regionsBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        gamePrefs = GamePreferences(this)
        soundHelper = SoundHelper(this)

        levelFolder = intent.getStringExtra(EXTRA_LEVEL_FOLDER) ?: "bakery_01"

        initViews()
        loadLevel()
    }

    private fun initViews() {
        canvasView = findViewById(R.id.coloringCanvas)
        rvPalette = findViewById(R.id.rvPalette)
        progressBar = findViewById(R.id.progressBar)
        tvProgressPercent = findViewById(R.id.tvProgressPercent)
        tvLevelTitle = findViewById(R.id.tvLevelTitle)
        tvChapterSubtitle = findViewById(R.id.tvChapterSubtitle)
        btnHint = findViewById(R.id.btnHint)
        btnBack = findViewById(R.id.btnBack)
        btnZoomIn = findViewById(R.id.btnZoomIn)
        btnZoomOut = findViewById(R.id.btnZoomOut)

        canvasView.listener = this

        btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        btnHint.setOnClickListener { view ->
            soundHelper.playClickHaptic(view)
            val found = canvasView.focusOnMissingRegion()
            if (!found) {
                Toast.makeText(this, "Color already completed! Select another number.", Toast.LENGTH_SHORT).show()
            }
        }

        btnZoomIn.setOnClickListener {
            canvasView.zoomIn()
        }

        btnZoomOut.setOnClickListener {
            canvasView.zoomOut()
        }
    }

    private fun loadLevel() {
        try {
            val data = LevelLoader.loadLevelData(this, levelFolder)
            val lines = LevelLoader.loadLinesBitmap(this, levelFolder)
            val regions = LevelLoader.loadRegionsBitmap(this, levelFolder)

            levelData = data
            linesBitmap = lines
            regionsBitmap = regions

            tvLevelTitle.text = data.title
            tvChapterSubtitle.text = data.chapterTitle

            val alreadyColored = gamePrefs.getColoredRegions(data.id)

            // Setup palette adapter
            paletteAdapter = PaletteAdapter(data.palette) { selectedColor ->
                canvasView.selectedColorId = selectedColor.id
            }
            rvPalette.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
            rvPalette.adapter = paletteAdapter

            // Select first incomplete color
            val firstIncomplete = data.palette.find { it.remainingRegions > 0 }?.id ?: 1
            paletteAdapter.setSelectedColor(firstIncomplete)
            canvasView.selectedColorId = firstIncomplete

            // Initialize canvas
            canvasView.initLevel(data, lines, regions, alreadyColored)

            updateProgressDisplay(data.totalRegions - alreadyColored.size, data.totalRegions)

            // Show story intro if not yet completed
            if (!gamePrefs.isLevelCompleted(data.id) && alreadyColored.isEmpty()) {
                StoryDialog.show(
                    context = this,
                    chapterText = "CHAPTER ${data.chapter}",
                    titleText = data.title,
                    storyBodyText = data.storyIntro,
                    buttonText = "Start Coloring"
                ) {}
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to load level: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun updateProgressDisplay(remainingCount: Int, totalCount: Int) {
        val colored = totalCount - remainingCount
        val percent = if (totalCount > 0) (colored * 100) / totalCount else 0
        progressBar.progress = percent
        tvProgressPercent.text = "$percent%"
    }

    override fun onRegionColored(region: RegionItem, remainingInColor: Int, totalRemaining: Int) {
        soundHelper.playClickHaptic(canvasView)

        levelData?.let { data ->
            gamePrefs.saveColoredRegion(data.id, region.id)
            paletteAdapter.notifyColorUpdated(region.colorId)
            updateProgressDisplay(totalRemaining, data.totalRegions)
        }
    }

    override fun onWrongColorTapped(tappedColorId: Int) {
        // Soft prompt indicating the correct number for the tapped region
        paletteAdapter.setSelectedColor(tappedColorId)
        canvasView.selectedColorId = tappedColorId
        scrollToPaletteColor(tappedColorId)
    }

    override fun onColorCompleted(colorId: Int) {
        soundHelper.playSuccessHaptic()
        paletteAdapter.notifyColorUpdated(colorId)

        // Automatically switch to the next incomplete color (Happy Color style!)
        val nextId = paletteAdapter.getNextIncompleteColorId(colorId)
        if (nextId != null) {
            paletteAdapter.setSelectedColor(nextId)
            canvasView.selectedColorId = nextId
            scrollToPaletteColor(nextId)
        }
    }

    override fun onLevelCompleted() {
        soundHelper.playSuccessHaptic()
        val data = levelData ?: return

        gamePrefs.setLevelCompleted(data.id, true)

        // Unlock next level (if available)
        gamePrefs.setLevelUnlocked("clockmaker_02", true)

        StoryDialog.show(
            context = this,
            chapterText = "CHAPTER ${data.chapter} COMPLETE!",
            titleText = data.title,
            storyBodyText = data.storyOutro,
            buttonText = "Continue Chapter"
        ) {
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

    private fun scrollToPaletteColor(colorId: Int) {
        levelData?.palette?.indexOfFirst { it.id == colorId }?.let { index ->
            if (index != -1) {
                rvPalette.smoothScrollToPosition(index)
            }
        }
    }

    companion object {
        const val EXTRA_LEVEL_FOLDER = "extra_level_folder"

        fun launch(activity: Activity, levelFolder: String) {
            val intent = Intent(activity, MainActivity::class.java).apply {
                putExtra(EXTRA_LEVEL_FOLDER, levelFolder)
            }
            activity.startActivity(intent)
        }
    }
}
