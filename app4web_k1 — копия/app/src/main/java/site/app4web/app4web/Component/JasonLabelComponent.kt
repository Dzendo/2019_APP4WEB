package site.app4web.app4web.Component //import

import android.content.Context
import android.graphics.Typeface
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.TextView
import site.app4web.app4web.Helper.JasonHelper
import org.json.JSONObject

object JasonLabelComponent {
    fun build(
        view: View?,
        component: JSONObject,
        parent: JSONObject?,
        context: Context
    ): View {
        return if (view == null) {
            TextView(context)
        } else {
            try {
                (view as TextView).setText(component.getString("text"))
                JasonComponent.Companion.build(view, component, parent, context)
                //val type: String
                val style: JSONObject = JasonHelper.style(component, context)
                val type = component.getString("type")
                if (style.has("color")) {
                    val color: Int = JasonHelper.parse_color(style.getString("color"))
                    (view as TextView).setTextColor(color)
                }
                JasonHelper.setTextViewFont(view as TextView?, style, context)
                var g = 0
                if (style.has("align")) {
                    val align: String = style.getString("align")
                    if (align.equals("center", ignoreCase = true)) {
                        g = g or Gravity.CENTER_HORIZONTAL
                        (view as TextView).setGravity(Gravity.CENTER_HORIZONTAL)
                    } else if (align.equals("right", ignoreCase = true)) {
                        g = g or Gravity.RIGHT
                        (view as TextView).setGravity(Gravity.RIGHT)
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
                (view as TextView).setGravity(g)
                if (style.has("size")) {
                    (view as TextView).setTextSize(style.getString("size").toFloat())
                }
                (view as TextView).setHorizontallyScrolling(false)
                JasonComponent.Companion.addListener(view, context)
                view.requestLayout()
                view
            } catch (e: Exception) {
                Log.d(
                    "Warning",
                    e.stackTrace[0].methodName + " : " + e.toString()
                )
                View(context)
            }
        }
    }
}