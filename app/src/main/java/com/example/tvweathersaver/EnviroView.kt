package com.example.tvweathersaver

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.util.AttributeSet
import android.view.View
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Color
import androidx.core.content.res.getFloatOrThrow
import androidx.core.content.res.getIntOrThrow
import androidx.core.content.res.getResourceIdOrThrow

private const val textSize = 22f

fun Float.format(digits: Int) = "%.${digits}f".format(this)
fun String.format(digits: Int) = "%s".format(this.substring(0, digits))

private const val imgSize: Int = 48;

class EnviroView : View {
    private var icon: Bitmap? = null;
    private var name: String? = null;
    private var limits: Pair<Int, Int>? = null;
    private var value: Float? = null;
    private var unit: String? = null;
    private var color: Color? = null;
    private val textPaint: Paint = Paint();

    fun updateView(value: Float) {
        this.value = value
        invalidate()
    }
    @RequiresApi(Build.VERSION_CODES.O)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs, 0)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init(attrs, defStyle)
    }
    @RequiresApi(Build.VERSION_CODES.O)
    constructor(context: Context, iconResource: Int, name: String,
                limits:Pair<Int, Int>, value: Float, unit: String,
                color: Color) : super(context) {
        init()
        icon = BitmapFactory.decodeResource(context.getResources(), iconResource);
        this.name = name
        this.limits = limits
        this.value = value
        this.unit = unit
        this.color = color
    }
    @SuppressLint("CustomViewStyleable")
    @RequiresApi(Build.VERSION_CODES.O)
    private fun init(attrs: AttributeSet?, defStyle: Int) {
        val parsedAttributes = context.obtainStyledAttributes(
            attrs, R.styleable.EnviroModuleView, defStyle, 0
        )
        try {
            val iconReference = parsedAttributes.getResourceIdOrThrow(R.styleable.EnviroModuleView_icon)
            icon = BitmapFactory.decodeResource(context.resources, iconReference)
            name = parsedAttributes.getString(R.styleable.EnviroModuleView_name)
            limits = Pair<Int, Int>(
                parsedAttributes.getIntOrThrow(R.styleable.EnviroModuleView_limitLow),
                parsedAttributes.getIntOrThrow(R.styleable.EnviroModuleView_limitUp))
            value = parsedAttributes.getFloatOrThrow(R.styleable.EnviroModuleView_value)
            unit = parsedAttributes.getString(R.styleable.EnviroModuleView_unit)
        } catch(e: IllegalArgumentException) {
            // TODO: Process exception
        }
        parsedAttributes.recycle()
        init()
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun init() {
        textPaint.color = Color.White.hashCode()
        textPaint.style = Paint.Style.FILL
        textPaint.typeface = context.resources.getFont(R.font.maston_font)
        textPaint.textSize = textSize
        textPaint.textAlign = Paint.Align.LEFT;
        textPaint.strokeWidth = 10.0f
    }
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = paddingLeft + imgSize*2 + paddingRight
        val height = paddingTop + imgSize*2 + paddingBottom
        setMeasuredDimension(width, height)
    }
    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        icon ?: return
        val x = paddingLeft + imgSize*2/ 2f - imgSize
        var y = paddingTop + imgSize*2 / 2f - imgSize
        textPaint.run {
            y -= ((descent() + ascent()) / 2)

            val rectBox = RectF(
                x, y, x + imgSize*1.5f, y + imgSize*1.5f
            );
            val backgroundPaint = Paint()
            backgroundPaint.color = this.color.hashCode()
            backgroundPaint.alpha = 128
            canvas.drawRoundRect(rectBox, imgSize * 2.0f, imgSize * 2.0f, backgroundPaint)
            val bufRect = rectBox
            rectBox.left = x + 0.25f* imgSize
            rectBox.top = y + 0.25f* imgSize
            rectBox.right = x + imgSize*1.25f
            rectBox.bottom = y + imgSize*1.25f
            canvas.drawBitmap(icon!!, null, rectBox, null)
            val text = "${value?.format(1)} ${unit?.format(1)}"
            val bounds = RectF(bufRect)
            bounds.right = textPaint.measureText(text, 0, text.length)
            bounds.bottom = textPaint.descent() - textPaint.ascent()
            bounds.left += (bufRect.width() - bounds.right) / 2.0f;
            bounds.top += (bufRect.height() - bounds.bottom) / 2.0f;

            canvas.drawText(
                text,
                (bounds.left), (y + imgSize*1.6).toFloat(), this
            )
            super.onDraw(canvas)
        }
    }
}