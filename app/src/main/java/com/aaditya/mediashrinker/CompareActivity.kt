package com.aaditya.mediashrinker

import android.graphics.Matrix
import android.graphics.PointF
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class CompareActivity : AppCompatActivity() {

    private lateinit var beforeImage: ImageView
    private lateinit var afterImage: ImageView

    private val matrix = Matrix()
    private val savedMatrix = Matrix()
    private val startPoint = PointF()
    private val midPoint = PointF()
    private var oldDist = 1f
    private var mode = NONE

    companion object {
        private const val NONE = 0
        private const val DRAG = 1
        private const val ZOOM = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_compare)

        beforeImage = findViewById(R.id.beforeImage)
        afterImage = findViewById(R.id.afterImage)

        val beforeUri = intent.getStringExtra("before")
        val afterUri = intent.getStringExtra("after")

        beforeUri?.let { beforeImage.setImageURI(Uri.parse(it)) }
        afterUri?.let { afterImage.setImageURI(Uri.parse(it)) }

        // Wait for views to layout to set initial fit
        beforeImage.post { fitCenter(beforeImage) }
        afterImage.post { fitCenter(afterImage) }

        val touchListener = View.OnTouchListener { v, event ->
            handleTouch(event)
            if (event.action == MotionEvent.ACTION_UP) {
                v.performClick()
            }
            true
        }

        beforeImage.setOnTouchListener(touchListener)
        afterImage.setOnTouchListener(touchListener)
    }

    private fun fitCenter(view: ImageView) {
        val drawable = view.drawable ?: return
        val viewWidth = view.width.toFloat()
        val viewHeight = view.height.toFloat()
        val drawableWidth = drawable.intrinsicWidth.toFloat()
        val drawableHeight = drawable.intrinsicHeight.toFloat()

        val scale = if ((viewWidth / drawableWidth) < (viewHeight / drawableHeight)) {
            viewWidth / drawableWidth
        } else {
            viewHeight / drawableHeight
        }

        matrix.setScale(scale, scale)
        matrix.postTranslate((viewWidth - drawableWidth * scale) / 2f, (viewHeight - drawableHeight * scale) / 2f)
        applyMatrix()
    }

    private fun handleTouch(event: MotionEvent) {
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                savedMatrix.set(matrix)
                startPoint.set(event.x, event.y)
                mode = DRAG
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                oldDist = spacing(event)
                if (oldDist > 10f) {
                    savedMatrix.set(matrix)
                    midPoint(midPoint, event)
                    mode = ZOOM
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                mode = NONE
            }
            MotionEvent.ACTION_MOVE -> {
                if (mode == DRAG) {
                    matrix.set(savedMatrix)
                    matrix.postTranslate(event.x - startPoint.x, event.y - startPoint.y)
                } else if (mode == ZOOM) {
                    val newDist = spacing(event)
                    if (newDist > 10f) {
                        matrix.set(savedMatrix)
                        val scale = newDist / oldDist
                        matrix.postScale(scale, scale, midPoint.x, midPoint.y)
                    }
                }
                applyMatrix()
            }
        }
    }

    private fun applyMatrix() {
        beforeImage.imageMatrix = matrix
        afterImage.imageMatrix = matrix
    }

    private fun spacing(event: MotionEvent): Float {
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return kotlin.math.sqrt(x * x + y * y)
    }

    private fun midPoint(point: PointF, event: MotionEvent) {
        val x = event.getX(0) + event.getX(1)
        val y = event.getY(0) + event.getY(1)
        point.set(x / 2, y / 2)
    }
}
