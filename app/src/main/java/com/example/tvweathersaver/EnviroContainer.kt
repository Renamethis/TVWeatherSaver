package com.example.tvweathersaver

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Point
import android.util.Log
import android.widget.ListView
import android.widget.TextView
import androidx.compose.ui.graphics.Color
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
import androidx.constraintlayout.widget.ConstraintSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray


class EnviroContainer(
    private val layout: ConstraintLayout,
    private val context: Context,
    private val scope: CoroutineScope,
    private val backgroundColor: Color,
    screenWidth: Int,
    screenHeight: Int) {
    private var width: Int = screenWidth;
    private var height: Int = screenHeight;

    @SuppressLint("DiscouragedApi")
    private fun drawView(previousId: Int, key: String, value: Double, unit: String, limits: JSONArray, index: Int) {
        if (layout.findViewById<EnviroModuleView>(key.hashCode()) == null) {
            val drawableId = context.resources.getIdentifier(
                key,
                "drawable",
                "com.example.tvweathersaver"
            );
            val enviroView = EnviroModuleView(
                context,
                drawableId,
                key,
                Pair<Int, Int>(limits.getInt(0), limits.getInt(1)),
                value.toFloat(),
                unit,
                Point(0, 0),
                backgroundColor
            )
            enviroView.id = key.hashCode()
            var params = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
            )
            params.topToTop = layout.findViewById<TextView>(R.id.temperature_view).id
//            params.bottomToTop = ConstraintSet.PARENT_ID;
            params.startToStart = ConstraintSet.PARENT_ID;
            params.endToEnd = ConstraintSet.PARENT_ID;

////            params.topToTop = ConstraintSet.PARENT_ID;
//            params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
            if(previousId != -1) {
//                set.connect(enviroView.id, ConstraintSet.BOTTOM, previousId, ConstraintSet.BOTTOM);
//                set.connect(enviroView.id, ConstraintSet.TOP, previousId, ConstraintSet.TOP);
                val prevParams: LayoutParams = layout.findViewById<EnviroModuleView>(previousId).layoutParams as LayoutParams
                prevParams.startToEnd = enviroView.id
//                prevParams.bottomToTop = enviroView.id
//                params.topToBottom = previousId
                params.endToStart = previousId
//                params.rightToRight = previousId
            } else {

            }
            enviroView.layoutParams = params
            layout.addView(enviroView);
        } else {
            val existsView = layout.findViewById(key.hashCode()) as? EnviroModuleView
            existsView?.updateView(value.toFloat())
        }
    }
    fun update() {
        val job: Job = scope.launch {
            val response = withContext(Dispatchers.IO) { HttpClient.get(context.getString(R.string.enviro_backend_url)) }
            if(response != null) {
                var i: Int = 0;
                var prevId: Int = -1;
                for (key in response.keys()) {
                    if (key != "datetime") {
                        val obj = response.getJSONObject(key);
                        val value = obj.getDouble("value")
                        val unit = obj.getString("unit")
                        val limits = obj.getJSONArray("limits")
                        drawView(prevId, key, value, unit, limits, i)
                        prevId = key.hashCode()
                        i++
                    }
                }
            }
            layout.invalidate()
        }
    }
}