package site.app4web.app4web.Component

import android.content.*
import android.graphics.PorterDuff
import android.util.Log
import android.view.*
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import org.json.JSONObject
import site.app4web.app4web.Core.JasonViewActivity
import site.app4web.app4web.Helper.JasonHelper


object JasonSliderComponent {
    fun build(view: View?, component: JSONObject, parent: JSONObject?, context: Context): View? {
        var view = view
        return if (view == null) {
            SeekBar(context)
        } else {
            try {
                view = JasonComponent.build(view, component, parent, context)
                val seekBar = view as SeekBar?
                if (component.has("name")) {
                    var `val` = "0.5"
                    if ((context as JasonViewActivity).model!!.`var`!!.has(component.getString("name"))) {
                        `val` = context.model!!.`var`!!.getString(component.getString("name"))
                    } else {
                        // default value
                        if (component.has("value")) {
                            `val` = component.getString("value")
                        }
                    }
                    seekBar!!.progress = Math.round(`val`.toDouble() * 100.0).toInt()
                    addListener(seekBar, context)
                }
                val style = JasonHelper.style(component, context)
                if (style.has("color")) {
                    val color = JasonHelper.parse_color(style.getString("color"))
                    seekBar!!.progressDrawable.setColorFilter(color, PorterDuff.Mode.SRC_IN)
                    seekBar.thumb.setColorFilter(color, PorterDuff.Mode.SRC_IN)
                } else {
                    // maybe it's not necessary
                    // возможно это не нужно
                    seekBar!!.progressDrawable.clearColorFilter()
                    // it's necessary
                    // необходимо
                    seekBar.thumb.clearColorFilter()
                }
                view.requestLayout()
                view
            } catch (e: Exception) {
                Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
                View(context)
            }
        }
    }

    fun addListener(view: SeekBar, root_context: Context) {
        val seekListener: OnSeekBarChangeListener = object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {}
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                val component = seekBar.tag as JSONObject
                try {
                    // don't work with int if progress == 0
                    // не работаем с int, если progress == 0
                    val progress = java.lang.Double.toString(seekBar.progress / 100.0)
                    (root_context as JasonViewActivity).model!!.`var`!!.put(
                        component.getString("name"),
                        progress
                    )
                    if (component.has("action")) {
                        val action = component.getJSONObject("action")
                        root_context.call(
                            action.toString(),
                            JSONObject().toString(),
                            "{}",
                            view.context
                        )
                    }
                } catch (e: Exception) {
                    Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
                }
            }
        }
        view.setOnSeekBarChangeListener(seekListener)
    }
}