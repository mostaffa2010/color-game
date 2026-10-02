package com.colorgame.app.ui

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.colorgame.app.model.LevelData
import com.colorgame.app.model.RegionItem
import kotlin.math.max
import kotlin.math.min

class ColoringCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    interface OnColoringListener {
        fun onRegionColored(region: RegionItem, remainingInColor: Int, totalRemaining: Int)
        fun onWrongColorTapped(tappedColorId: Int)
        fun onColorCompleted(colorId: Int)
        fun onLevelCompleted()
    }

    var listener: OnColoringListener? = null

    private var levelData: LevelData? = null
    private var linesBitmap: Bitmap? = null
    private var regionsBitmap: Bitmap? = null
    private var coloredCanvasBitmap: Bitmap? = null

    // Raw pixel arrays for ultra-fast O(1) buffer manipulation
    private var canvasPixels: IntArray? = null
    private var regionsPixels: IntArray? = null

    private var imageWidth = 1024
    private var imageHeight = 1024

    private val coloredRegionIds = mutableSetOf<Int>()
    private val regionLookup = mutableMapOf<Int, RegionItem>()
    private val colorToRegions = mutableMapOf<Int, MutableList<RegionItem>>()

    var selectedColorId: Int = 1
        set(value) {
            field = value
            invalidate()
        }

    // Canvas Transformation & Gestures
    private val currentMatrix = Matrix()
    private val inverseMatrix = Matrix()
    private val matrixValues = FloatArray(9)

    private var minScale = 1.0f
    private var maxScale = 16.0f
    private var baseScale = 1.0f

    private val scaleDetector: ScaleGestureDetector
    private val gestureDetector: GestureDetector

    // Hint Animation
    private var hintRegion: RegionItem? = null
    private var hintPulseAlpha = 0
    private var hintAnimator: ValueAnimator? = null

    // Paints
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    
    private val activeNumberBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        setShadowLayer(4f, 0f, 2f, 0x40000000)
    }

    private val activeNumberRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF6D00")
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val activeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#212121")
        textSize = 28f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    private val passiveTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#808080")
        textSize = 22f
        textAlign = Paint.Align.CENTER
    }

    private val hintGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF9100")
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }

    private val textBounds = Rect()

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)

        val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                currentMatrix.postTranslate(-distanceX, -distanceY)
                clampTranslation()
                invalidate()
                return true
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                handleTap(e.x, e.y)
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                val currentScale = getCurrentScale()
                val targetScale = if (currentScale > baseScale * 2.0f) baseScale else baseScale * 3.5f
                animateZoom(targetScale, e.x, e.y)
                return true
            }
        }

        gestureDetector = GestureDetector(context, gestureListener)
        gestureDetector.setOnDoubleTapListener(gestureListener)

        scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val scaleFactor = detector.scaleFactor
                val currentScale = getCurrentScale()
                val targetScale = currentScale * scaleFactor

                if (targetScale in minScale..maxScale) {
                    currentMatrix.postScale(scaleFactor, scaleFactor, detector.focusX, detector.focusY)
                    clampTranslation()
                    invalidate()
                }
                return true
            }
        })
    }

    fun initLevel(
        data: LevelData,
        lines: Bitmap,
        regions: Bitmap,
        alreadyColored: Set<Int>
    ) {
        levelData = data
        linesBitmap = lines
        regionsBitmap = regions
        imageWidth = data.width
        imageHeight = data.height

        regionLookup.clear()
        colorToRegions.clear()
        data.regions.forEach { r ->
            regionLookup[r.id] = r
            colorToRegions.getOrPut(r.colorId) { mutableListOf() }.add(r)
        }

        // Prepare raw pixel buffers
        val totalPixels = imageWidth * imageHeight
        regionsPixels = IntArray(totalPixels)
        regions.getPixels(regionsPixels, 0, imageWidth, 0, 0, imageWidth, imageHeight)

        // Initialize colored canvas with light paper color (#FAF8F5)
        canvasPixels = IntArray(totalPixels) { Color.parseColor("#FAF8F5") }
        coloredCanvasBitmap = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888)

        // Restore already colored regions
        coloredRegionIds.clear()
        alreadyColored.forEach { rid ->
            colorRegionInternal(rid, updateBitmap = false)
        }

        coloredCanvasBitmap?.setPixels(canvasPixels, 0, imageWidth, 0, 0, imageWidth, imageHeight)

        // Update remaining counts in levelData
        data.palette.forEach { p ->
            val allForColor = colorToRegions[p.id] ?: emptyList()
            p.remainingRegions = allForColor.count { !coloredRegionIds.contains(it.id) }
        }

        requestLayout()
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0 && imageWidth > 0 && imageHeight > 0) {
            resetMatrixToFit()
        }
    }

    fun resetMatrixToFit() {
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        if (viewWidth <= 0 || viewHeight <= 0) return

        val scaleX = viewWidth / imageWidth
        val scaleY = viewHeight / imageHeight
        baseScale = min(scaleX, scaleY) * 0.95f
        minScale = baseScale * 0.8f
        maxScale = baseScale * 14.0f

        currentMatrix.reset()
        currentMatrix.postScale(baseScale, baseScale)

        val scaledWidth = imageWidth * baseScale
        val scaledHeight = imageHeight * baseScale
        val dx = (viewWidth - scaledWidth) / 2f
        val dy = (viewHeight - scaledHeight) / 2f
        currentMatrix.postTranslate(dx, dy)

        invalidate()
    }

    private fun handleTap(screenX: Float, screenY: Float) {
        val pixels = regionsPixels ?: return
        val currentLevel = levelData ?: return

        currentMatrix.invert(inverseMatrix)
        val touchPoints = floatArrayOf(screenX, screenY)
        inverseMatrix.mapPoints(touchPoints)

        val imgX = touchPoints[0].toInt()
        val imgY = touchPoints[1].toInt()

        if (imgX !in 0 until imageWidth || imgY !in 0 until imageHeight) return

        val pixelIndex = imgY * imageWidth + imgX
        val pixel = pixels[pixelIndex]

        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        val regionId = r or (g shl 8)
        val colorId = b

        if (regionId <= 0) return
        val region = regionLookup[regionId] ?: return

        if (coloredRegionIds.contains(regionId)) {
            // Already colored
            return
        }

        if (colorId == selectedColorId) {
            // Correct color!
            colorRegionInternal(regionId, updateBitmap = true)
            coloredRegionIds.add(regionId)
            region.isColored = true

            // Update remaining count
            val paletteColor = currentLevel.palette.find { it.id == colorId }
            val remainingInColor = (colorToRegions[colorId]?.count { !coloredRegionIds.contains(it.id) }) ?: 0
            paletteColor?.remainingRegions = remainingInColor

            val totalRemaining = currentLevel.totalRegions - coloredRegionIds.size
            listener?.onRegionColored(region, remainingInColor, totalRemaining)

            if (remainingInColor == 0) {
                listener?.onColorCompleted(colorId)
            }
            if (totalRemaining == 0) {
                listener?.onLevelCompleted()
            }

            invalidate()
        } else {
            // Player tapped a region belonging to a different color
            listener?.onWrongColorTapped(colorId)
        }
    }

    private fun colorRegionInternal(regionId: Int, updateBitmap: Boolean) {
        val region = regionLookup[regionId] ?: return
        val palette = levelData?.palette ?: return
        val paletteColor = palette.find { it.id == region.colorId } ?: return
        val colorInt = paletteColor.colorInt

        val pixels = regionsPixels ?: return
        val cPixels = canvasPixels ?: return

        // Fast update of all pixels matching this regionId
        val targetR = regionId and 0xFF
        val targetG = (regionId shr 8) and 0xFF

        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            if (r == targetR && g == targetG) {
                cPixels[i] = colorInt
            }
        }

        coloredRegionIds.add(regionId)
        region.isColored = true

        if (updateBitmap) {
            coloredCanvasBitmap?.setPixels(cPixels, 0, imageWidth, 0, 0, imageWidth, imageHeight)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.save()
        canvas.concat(currentMatrix)

        // 1. Draw base colored canvas
        coloredCanvasBitmap?.let {
            canvas.drawBitmap(it, 0f, 0f, bitmapPaint)
        }

        // 2. Draw black boundary outlines
        linesBitmap?.let {
            canvas.drawBitmap(it, 0f, 0f, bitmapPaint)
        }

        // 3. Draw Numbers & Active Badges
        drawRegionNumbers(canvas)

        // 4. Draw Hint pulse if active
        hintRegion?.let { hint ->
            if (hintPulseAlpha > 0) {
                hintGlowPaint.alpha = hintPulseAlpha
                val radius = max(24f, hint.maxRadius * 1.5f)
                canvas.drawCircle(hint.labelX.toFloat(), hint.labelY.toFloat(), radius, hintGlowPaint)
            }
        }

        canvas.restore()
    }

    private fun drawRegionNumbers(canvas: Canvas) {
        val currentScale = getCurrentScale()
        val regions = levelData?.regions ?: return

        // Compute visible screen viewport in image coordinates (frustum culling)
        currentMatrix.invert(inverseMatrix)
        val viewportPoints = floatArrayOf(
            0f, 0f,
            width.toFloat(), 0f,
            width.toFloat(), height.toFloat(),
            0f, height.toFloat()
        )
        inverseMatrix.mapPoints(viewportPoints)
        val minX = minOf(viewportPoints[0], minOf(viewportPoints[2], minOf(viewportPoints[4], viewportPoints[6]))) - 50
        val maxX = maxOf(viewportPoints[0], maxOf(viewportPoints[2], maxOf(viewportPoints[4], viewportPoints[6]))) + 50
        val minY = minOf(viewportPoints[1], minOf(viewportPoints[3], minOf(viewportPoints[5], viewportPoints[7]))) - 50
        val maxY = maxOf(viewportPoints[1], maxOf(viewportPoints[3], maxOf(viewportPoints[5], viewportPoints[7]))) + 50

        // Scale factors for text so numbers remain comfortable and readable
        val textScale = 1f / currentScale
        val badgeRadius = max(16f * textScale, 14f)
        val activeTextSize = max(22f * textScale, 18f)
        val passiveTextSize = max(18f * textScale, 15f)

        activeTextPaint.textSize = activeTextSize
        passiveTextPaint.textSize = passiveTextSize

        val showPassiveNumbers = currentScale > baseScale * 2.2f

        for (region in regions) {
            if (coloredRegionIds.contains(region.id)) continue

            val lx = region.labelX.toFloat()
            val ly = region.labelY.toFloat()

            // Viewport culling
            if (lx !in minX..maxX || ly !in minY..maxY) continue

            val isSelectedColor = region.colorId == selectedColorId

            if (isSelectedColor) {
                // Happy Color signature: Highlighted circular badge for active color regions
                canvas.drawCircle(lx, ly, badgeRadius, activeNumberBadgePaint)
                canvas.drawCircle(lx, ly, badgeRadius, activeNumberRingPaint)

                val text = region.colorId.toString()
                activeTextPaint.getTextBounds(text, 0, text.length, textBounds)
                val textY = ly - textBounds.exactCenterY()
                canvas.drawText(text, lx, textY, activeTextPaint)
            } else if (showPassiveNumbers) {
                // Subtle gray number for other uncolored regions when zoomed in
                val text = region.colorId.toString()
                passiveTextPaint.getTextBounds(text, 0, text.length, textBounds)
                val textY = ly - textBounds.exactCenterY()
                canvas.drawText(text, lx, textY, passiveTextPaint)
            }
        }
    }

    fun focusOnMissingRegion(): Boolean {
        val uncolored = colorToRegions[selectedColorId]?.filter { !coloredRegionIds.contains(it.id) }
        if (uncolored.isNullOrEmpty()) return false

        // Pick the largest remaining uncolored region of this color
        val target = uncolored.maxByOrNull { it.area } ?: uncolored.first()
        hintRegion = target

        // Zoom and center camera onto target
        val targetScale = baseScale * 4.0f
        val viewCenterX = width / 2f
        val viewCenterY = height / 2f

        val startMatrix = Matrix(currentMatrix)
        val destMatrix = Matrix()
        destMatrix.postScale(targetScale, targetScale)
        val dx = viewCenterX - target.labelX * targetScale
        val dy = viewCenterY - target.labelY * targetScale
        destMatrix.postTranslate(dx, dy)

        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 450
            interpolator = DecelerateInterpolator()
            addUpdateListener { va ->
                val f = va.animatedValue as Float
                interpolateMatrix(startMatrix, destMatrix, f)
                invalidate()
            }
        }
        animator.start()

        // Trigger pulsating glow
        hintAnimator?.cancel()
        hintAnimator = ValueAnimator.ofInt(0, 255, 0, 255, 0).apply {
            duration = 1200
            addUpdateListener { va ->
                hintPulseAlpha = va.animatedValue as Int
                invalidate()
            }
        }
        hintAnimator?.start()

        return true
    }

    fun zoomIn() {
        val newScale = min(getCurrentScale() * 1.5f, maxScale)
        animateZoom(newScale, width / 2f, height / 2f)
    }

    fun zoomOut() {
        val newScale = max(getCurrentScale() / 1.5f, minScale)
        animateZoom(newScale, width / 2f, height / 2f)
    }

    private fun animateZoom(targetScale: Float, focusX: Float, focusY: Float) {
        val startMatrix = Matrix(currentMatrix)
        val destMatrix = Matrix(currentMatrix)
        val scaleFactor = targetScale / getCurrentScale()
        destMatrix.postScale(scaleFactor, scaleFactor, focusX, focusY)

        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 300
            interpolator = DecelerateInterpolator()
            addUpdateListener { va ->
                val f = va.animatedValue as Float
                interpolateMatrix(startMatrix, destMatrix, f)
                clampTranslation()
                invalidate()
            }
        }
        animator.start()
    }

    private fun interpolateMatrix(start: Matrix, end: Matrix, fraction: Float) {
        val startVals = FloatArray(9)
        val endVals = FloatArray(9)
        val currVals = FloatArray(9)

        start.getValues(startVals)
        end.getValues(endVals)

        for (i in 0..8) {
            currVals[i] = startVals[i] + (endVals[i] - startVals[i]) * fraction
        }
        currentMatrix.setValues(currVals)
    }

    private fun clampTranslation() {
        currentMatrix.getValues(matrixValues)
        val transX = matrixValues[Matrix.MTRANS_X]
        val transY = matrixValues[Matrix.MTRANS_Y]
        val scale = matrixValues[Matrix.MSCALE_X]

        val viewW = width.toFloat()
        val viewH = height.toFloat()
        val imgW = imageWidth * scale
        val imgH = imageHeight * scale

        val minTransX = if (imgW < viewW) (viewW - imgW) / 2f else viewW - imgW - 100f
        val maxTransX = if (imgW < viewW) (viewW - imgW) / 2f else 100f

        val minTransY = if (imgH < viewH) (viewH - imgH) / 2f else viewH - imgH - 100f
        val maxTransY = if (imgH < viewH) (viewH - imgH) / 2f else 100f

        val clampedX = transX.coerceIn(minTransX, maxTransX)
        val clampedY = transY.coerceIn(minTransY, maxTransY)

        matrixValues[Matrix.MTRANS_X] = clampedX
        matrixValues[Matrix.MTRANS_Y] = clampedY
        currentMatrix.setValues(matrixValues)
    }

    private fun getCurrentScale(): Float {
        currentMatrix.getValues(matrixValues)
        return matrixValues[Matrix.MSCALE_X]
    }
}
