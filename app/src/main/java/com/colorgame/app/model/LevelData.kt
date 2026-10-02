package com.colorgame.app.model

import android.graphics.Color
import com.google.gson.annotations.SerializedName

data class LevelData(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("chapter") val chapter: Int,
    @SerializedName("chapterTitle") val chapterTitle: String,
    @SerializedName("storyIntro") val storyIntro: String,
    @SerializedName("storyOutro") val storyOutro: String,
    @SerializedName("width") val width: Int,
    @SerializedName("height") val height: Int,
    @SerializedName("totalRegions") val totalRegions: Int,
    @SerializedName("totalColors") val totalColors: Int,
    @SerializedName("palette") val palette: List<PaletteColor>,
    @SerializedName("regions") val regions: List<RegionItem>
)

data class PaletteColor(
    @SerializedName("id") val id: Int,
    @SerializedName("hex") val hex: String,
    @SerializedName("r") val r: Int,
    @SerializedName("g") val g: Int,
    @SerializedName("b") val b: Int,
    @SerializedName("totalRegions") val totalRegions: Int,
    var remainingRegions: Int = 0
) {
    val colorInt: Int
        get() = try {
            Color.parseColor(hex)
        } catch (e: Exception) {
            Color.rgb(r, g, b)
        }

    val isCompleted: Boolean
        get() = remainingRegions <= 0

    // Calculates contrasting text color (white or black) for the number display
    val textColor: Int
        get() {
            val luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
            return if (luminance > 0.6) Color.BLACK else Color.WHITE
        }
}

data class RegionItem(
    @SerializedName("id") val id: Int,
    @SerializedName("labelX") val labelX: Int,
    @SerializedName("labelY") val labelY: Int,
    @SerializedName("maxRadius") val maxRadius: Float,
    @SerializedName("area") val area: Int,
    @SerializedName("colorId") val colorId: Int,
    var isColored: Boolean = false
)

data class LevelsManifest(
    @SerializedName("chapters") val chapters: List<Chapter>
)

data class Chapter(
    @SerializedName("id") val id: String,
    @SerializedName("number") val number: Int,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("levels") val levels: List<ChapterLevel>
)

data class ChapterLevel(
    @SerializedName("id") val id: String,
    @SerializedName("folder") val folder: String,
    @SerializedName("title") val title: String,
    @SerializedName("previewColor") val previewColor: String,
    @SerializedName("totalColors") val totalColors: Int,
    @SerializedName("isUnlocked") var isUnlocked: Boolean,
    @SerializedName("storyIntro") val storyIntro: String,
    @SerializedName("storyOutro") val storyOutro: String
)
