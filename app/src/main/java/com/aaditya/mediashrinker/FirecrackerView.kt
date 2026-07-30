package com.aaditya.mediashrinker

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// TEMPORARY (cosmetic Independence Day theme) — only started when
// IndependenceDayTheme.isIndependenceDay() is true. Safe to remove in a
// future update along with the rest of the Independence Day theme code.
class FirecrackerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private data class Particle(
        var x: Float,
        var y: Float,
        val vx: Float,
        val vy: Float,
        val color: Int,
        var life: Float = 1f
    )

    private val bursts = mutableListOf<MutableList<Particle>>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val colors = intArrayOf(
        0xFFFF9933.toInt(), // saffron
        0xFFFFFFFF.toInt(), // white
        0xFF138808.toInt(), // green
        0xFFFFD700.toInt()  // gold sparkle
    )

    private var animator: ValueAnimator? = null

    fun start() {
        if (animator != null) return
        visibility = VISIBLE
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 16
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener {
                updateParticles()
                // Roughly one new burst every ~1.5-2 seconds on average
                if (Random.nextInt(0, 100) < 3) spawnBurst()
                invalidate()
            }
            start()
        }
    }

    fun stop() {
        animator?.cancel()
        animator = null
        bursts.clear()
        visibility = GONE
    }

    private fun spawnBurst() {
        if (width == 0 || height == 0) return
        val cx = Random.nextFloat() * width
        val cy = Random.nextFloat() * (height * 0.6f)
        val particles = mutableListOf<Particle>()
        val count = 16
        for (i in 0 until count) {
            val angle = (2 * Math.PI * i / count).toFloat()
            val speed = Random.nextFloat() * 6f + 3f
            particles.add(
                Particle(
                    x = cx, y = cy,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed,
                    color = colors[Random.nextInt(colors.size)]
                )
            )
        }
        bursts.add(particles)
    }

    private fun updateParticles() {
        val burstIterator = bursts.iterator()
        while (burstIterator.hasNext()) {
            val particles = burstIterator.next()
            val particleIterator = particles.iterator()
            while (particleIterator.hasNext()) {
                val p = particleIterator.next()
                p.x += p.vx
                p.y += p.vy
                p.life -= 0.02f
                if (p.life <= 0f) particleIterator.remove()
            }
            if (particles.isEmpty()) burstIterator.remove()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (particles in bursts) {
            for (p in particles) {
                paint.color = p.color
                paint.alpha = (p.life.coerceIn(0f, 1f) * 200).toInt()
                canvas.drawCircle(p.x, p.y, 4f, paint)
            }
        }
    }
}
