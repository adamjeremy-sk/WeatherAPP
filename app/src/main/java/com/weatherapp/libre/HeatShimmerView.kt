package com.weatherapp.libre

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import kotlin.math.sin
import kotlin.random.Random

class HeatShimmerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private data class HeatParticle(
        var x: Float,
        var y: Float,
        var speedY: Float,
        var radius: Float,
        var alpha: Int,
        var waveOffset: Float
    )

    private val heatParticles = mutableListOf<HeatParticle>()
    private val wavePath = Path()

    private val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.parseColor("#44FFA500") // Warm amber heat glow
    }

    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#33FF7700") // Soft hot air haze
    }

    private var phase = 0f
    private var isRunning = false

    fun startHeatShimmer() {
        if (isRunning) return
        isRunning = true
        visibility = VISIBLE
        generateParticles()
        postInvalidateOnAnimation()
    }

    fun stopHeatShimmer() {
        isRunning = false
        heatParticles.clear()
        visibility = GONE
        invalidate()
    }

    private fun generateParticles() {
        heatParticles.clear()
        val count = 35
        val w = if (width > 0) width.toFloat() else 1080f
        val h = if (height > 0) height.toFloat() else 1920f

        for (i in 0 until count) {
            heatParticles.add(
                HeatParticle(
                    x = Random.nextFloat() * w,
                    y = Random.nextFloat() * h,
                    speedY = Random.nextFloat() * 3.5f + 1.5f,
                    radius = Random.nextFloat() * 12f + 6f,
                    alpha = Random.nextInt(30, 90),
                    waveOffset = Random.nextFloat() * 6.28f
                )
            )
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (isRunning) generateParticles()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!isRunning) return

        val w = width.toFloat()
        val h = height.toFloat()

        phase += 0.08f

        // Draw rising heat wave mirage lines across screen
        val numWaves = 12
        val waveSpacing = h / numWaves

        for (i in 0 until numWaves) {
            val baseY = i * waveSpacing + ((phase * 15f) % waveSpacing)
            wavePath.reset()
            wavePath.moveTo(0f, baseY)

            var x = 0f
            val step = 20f
            while (x <= w) {
                val y = baseY + sin(x * 0.015f + phase + (i * 0.8f)) * 8f
                wavePath.lineTo(x, y)
                x += step
            }

            val alphaInt = ((1f - (baseY / h)) * 70 + 20).toInt().coerceIn(10, 90)
            wavePaint.alpha = alphaInt
            canvas.drawPath(wavePath, wavePaint)
        }

        // Draw rising hot air particles
        for (p in heatParticles) {
            particlePaint.alpha = p.alpha
            val waveX = p.x + sin(phase + p.waveOffset) * 10f
            canvas.drawCircle(waveX, p.y, p.radius, particlePaint)

            // Move hot air upward
            p.y -= p.speedY
            if (p.y < -20f) {
                p.y = h + 10f
                p.x = Random.nextFloat() * w
            }
        }

        postInvalidateOnAnimation()
    }
}
