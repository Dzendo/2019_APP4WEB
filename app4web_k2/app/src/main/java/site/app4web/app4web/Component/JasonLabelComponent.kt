package site.app4web.app4web.Component

import android.content.*
import android.util.Log
import android.view.*
import android.widget.TextView
import org.json.JSONObject
import site.app4web.app4web.Helper.JasonHelper


object JasonLabelComponent {
    fun build(view: View?, component: JSONObject, parent: JSONObject?, context: Context?): View {
        return if (view == null) {
            TextView(context)
        } else {
            try {
                (view as TextView).text = component.getString("text")
                JasonComponent.build(view, component, parent, context)
                val type: String
                val style = JasonHelper.style(component, context)
                type = component.getString("type")
                if (style.has("color")) {
                    val color = JasonHelper.parse_color(style.getString("color"))
                    view.setTextColor(color)
                }
                JasonHelper.setTextViewFont(view as TextView?, style, context)
                var g = 0
                if (style.has("align")) {
                    val align = style.getString("align")
                    if (align.equals("center", ignoreCase = true)) {
                        g = g or Gravity.CENTER_HORIZONTAL
                        view.gravity = Gravity.CENTER_HORIZONTAL
                    } else if (align.equals("right", ignoreCase = true)) {
                        g = g or Gravity.RIGHT
                        view.gravity = Gravity.RIGHT
                    } else if (align.equals("left", ignoreCase = true)) {
                        g = g or Gravity.LEFT
                    }
                    g = if (align.equals("top", ignoreCase = true)) {
                        g or Gravity.TOP
                    } else if (align.equals("bottom", ignoreCase = true)) {
                        g or Gravity.BOTTOM
                    } else {
                        g or Gravity.CENTER_VERTICAL
                    }
                } else {
                    g = Gravity.CENTER_VERTICAL
                }
                view.gravity = g
                if (style.has("size")) {
                    view.textSize = style.getString("size").toFloat()
                }
                view.setHorizontallyScrolling(false)
                JasonComponent.addListener(view, context)
                view.requestLayout()
                view
            } catch (e: Exception) {
                Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
                View(context)
            }
        }
    }
}