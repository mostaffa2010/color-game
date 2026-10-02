package com.colorgame.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.colorgame.app.model.LevelData
import com.colorgame.app.model.LevelsManifest
import com.google.gson.Gson
import java.io.InputStreamReader

object LevelLoader {

    fun loadManifest(context: Context): LevelsManifest {
        val jsonString = context.assets.open("levels/levels_manifest.json").use { stream ->
            InputStreamReader(stream, Charsets.UTF_8).readText()
        }
        return Gson().fromJson(jsonString, LevelsManifest::class.java)
    }

    fun loadLevelData(context: Context, folderName: String): LevelData {
        val jsonString = context.assets.open("levels/$folderName/level.json").use { stream ->
            InputStreamReader(stream, Charsets.UTF_8).readText()
        }
        return Gson().fromJson(jsonString, LevelData::class.java)
    }

    fun loadLinesBitmap(context: Context, folderName: String): Bitmap {
        context.assets.open("levels/$folderName/lines.png").use { stream ->
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            return BitmapFactory.decodeStream(stream, null, options)
                ?: throw IllegalStateException("Could not decode lines.png")
        }
    }

    fun loadRegionsBitmap(context: Context, folderName: String): Bitmap {
        context.assets.open("levels/$folderName/regions_map.png").use { stream ->
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            return BitmapFactory.decodeStream(stream, null, options)
                ?: throw IllegalStateException("Could not decode regions_map.png")
        }
    }
}
