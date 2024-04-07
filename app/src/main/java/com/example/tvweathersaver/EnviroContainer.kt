package com.example.tvweathersaver

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Color
import androidx.constraintlayout.widget.ConstraintLayout
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
    private val weatherToken: String?) {

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("DiscouragedApi")
    private fun drawView(previousId: Int, key: String, value: Double, unit: String, limits: JSONArray, index: Int) {
        if (layout.findViewById<EnviroView>(key.hashCode()) == null) {
            val drawableId = context.resources.getIdentifier(
                key,
                "drawable",
                "com.example.tvweathersaver"
            );
            val enviroView = EnviroView(
                context,
                drawableId,
                key,
                Pair<Int, Int>(limits.getInt(0), limits.getInt(1)),
                value.toFloat(),
                unit,
                backgroundColor
            )
            enviroView.id = key.hashCode()
            val params = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
            )
            params.rightToRight = ConstraintSet.PARENT_ID;
            params.leftToLeft = ConstraintSet.PARENT_ID;
            params.bottomToBottom = ConstraintSet.PARENT_ID;
            if(previousId != -1) {
                val prevParams = layout.findViewById<EnviroView>(previousId).layoutParams as ConstraintLayout.LayoutParams
                prevParams.leftToLeft = enviroView.id
                params.rightToRight = previousId
            }
            enviroView.layoutParams = params
            layout.addView(enviroView);
        } else {
            val existsView = layout.findViewById(key.hashCode()) as? EnviroView
            existsView?.updateView(value.toFloat())
        }
    }
    fun update() {
        val job: Job = scope.launch {
            val response = withContext(Dispatchers.IO) { HttpClient.get(context.getString(R.string.enviro_backend_url) + "/get_current_state",
                weatherToken) }
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