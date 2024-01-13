package com.example.tvweathersaver

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.text.TextPaint
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.core.content.res.getResourceIdOrThrow
import org.json.JSONObject
import android.graphics.Rect
import androidx.core.content.res.getFloatOrThrow
import androidx.core.content.res.getIntOrThrow

class EnviroModuleView : View {
    private var point: Point? = null
    private var icon: Bitmap? = null;
    private var name: String? = null;
    private var limits: Pair<Int, Int>? = null;
    private var value: Float? = null;
    private var unit: String? = null;
    private val textPaint: Paint = Paint();

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init(attrs, defStyle)
    }
    constructor(context: Context, iconResource: Int, name: String,
                limits:Pair<Int, Int>, value: Float, unit: String,
                point: Point) : super(context) {
        init()
        icon = BitmapFactory.decodeResource(context.getResources(), iconResource);
        this.name = name
        this.limits = limits
        this.value = value
        this.unit = unit
        this.point = point
    }
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
    private fun init() {
        textPaint.color = Color.WHITE
        textPaint.style = Paint.Style.FILL
        textPaint.textSize = 25f
    }
    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        point ?: return
        icon ?: return

        canvas.drawBitmap(icon!!, null,
                          Rect(point!!.x, point!!.y,
                              point!!.x + 64,
                              point!!.y + 64),
                    null)
        canvas.drawText("$name $value $unit", 0f, 0f, textPaint)
        super.onDraw(canvas)
    }
}