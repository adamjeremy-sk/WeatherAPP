package com.weatherapp.libre

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.random.Random

class SnowflakeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private data class Snowflake(
        var x: Float,
        var y: Float,
        var radius: Float,
        var speedY: Float,
        var alpha: Int,
        var xDrift: Float
    )

    private val snowflakes = mutableListOf<Snowflake>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private var isRunning = false

    fun startSnowing() {
        if (isRunning) return
        isRunning = true
        visibility = VISIBLE
        generateFlakes()
        postInvalidateOnAnimation()
    }

    fun stopSnowing() {
        isRunning = false
        snowflakes.clear()
        visibility = GONE
        invalidate()
    }

    private fun generateFlakes() {
        snowflakes.clear()
        val count = 45
        val w = if (width > 0) width.toFloat() else 1080f
        val h = if (height > 0) height.toFloat() else 1920f

        for (i in 0 until count) {
            snowflakes.add(
                Snowflake(
                    x = Random.nextFloat() * w,
                    y = Random.nextFloat() * h,
                    radius = Random.nextFloat() * 6f + 3f,
                    speedY = Random.nextFloat() * 4f + 2f,
                    alpha = Random.nextInt(140, 240),
                    xDrift = Random.nextFloat() * 2f - 1f
                )
            )
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (isRunning) {
            generateFlakes()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!isRunning) return

        val w = width.toFloat()
        val h = height.toFloat()

        for (flake in snowflakes) {
            paint.alpha = flake.alpha
            canvas.drawCircle(flake.x, flake.y, flake.radius, paint)

            // Animate position
            flake.y += flake.speedY
            flake.x += flake.xDrift

            // Wrap around bottom
            if (flake.y > h) {
                flake.y = -10f
                flake.x = Random.nextFloat() * w
            }
            if (flake.x < 0) flake.x = w
            if (flake.x > w) flake.x = 0f
        }

        postInvalidateOnAnimation()
    }
}
