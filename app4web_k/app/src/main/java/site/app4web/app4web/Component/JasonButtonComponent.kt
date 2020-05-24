package site.app4web.app4web.Component

import android.content.Context
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.TextView
import org.json.JSONObject
import site.app4web.app4web.Helper.JasonHelper

object JasonButtonComponent {
    fun build(
        view: View?,
        component: JSONObject,
        parent: JSONObject?,
        context: Context
    ): View? {
        var view = view
        if (component.has("url")) {
            // image button
            // кнопка изображения
            view = JasonImageComponent.build(view, component, parent, context)
        } else if (component.has("text")) {
            // label button
            // кнопка изображения
            view = JasonLabelComponent.build(view, component, parent, context)
            try {
                val style = component.getJSONObject("style")

                /*******
                 * ALIGN : By default align center
                 * ALIGN: по умолчанию выравнивание по центру
                 */

                // Default is center
                // По умолчанию центр
                var g = Gravity.CENTER
                if (style.has("align")) {
                    val align = style.getString("align")
                    if (align.equals("center", ignoreCase = true)) {
                        g = g or Gravity.CENTER_HORIZONTAL
                        (view as TextView?)!!.gravity = Gravity.CENTER_HORIZONTAL
                    } else if (align.equals("right", ignoreCase = true)) {
                        g = g or Gravity.RIGHT
                        (view as TextView?)!!.gravity = Gravity.RIGHT
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
                }
                (view as TextView?)!!.gravity = g
                /*******
                 * Padding: By default padding is 15
                 * Заполнение: по умолчанию заполнение 15
                 */
                // override each padding value only if it's not specified
                // переопределяем каждое значение отступа, только если оно не указано
                var padding_top = -1
                var padding_left = -1
                var padding_bottom = -1
                var padding_right = -1
                if (style.has("padding")) {
                    padding_top = JasonHelper.pixels(
                        context,
                        style.getString("padding_top"),
                        "horizontal"
                    ).toInt()
                    padding_left = padding_top
                    padding_right = padding_top
                    padding_bottom = padding_top
                }
                if (style.has("padding_top")) {
                    padding_top = JasonHelper.pixels(
                        context,
                        style.getString("padding_top"),
                        "vertical"
                    ).toInt()
                }
                if (style.has("padding_left")) {
                    padding_left = JasonHelper.pixels(
                        context,
                        style.getString("padding_left"),
                        "horizontal"
                    ).toInt()
                }
                if (style.has("padding_bottom")) {
                    padding_bottom = JasonHelper.pixels(
                        context,
                        style.getString("padding_bottom"),
                        "vertical"
                    ).toInt()
                }
                if (style.has("padding_right")) {
                    padding_right = JasonHelper.pixels(
                        context,
                        style.getString("padding_right"),
                        "horizontal"
                    ).toInt()
                }

                // if not specified, default is 15
                // если не указано, по умолчанию 15
                if (padding_top < 0) {
                    padding_top = 15
                }
                if (padding_left < 0) {
                    padding_left = 15
                }
                if (padding_bottom < 0) {
                    padding_bottom = 15
                }
                if (padding_right < 0) {
                    padding_right = 15
                }
                view.setPadding(padding_left, padding_top, padding_right, padding_bottom)
            } catch (e: Exception) {
                Log.d(
                    "Warning",
                    e.stackTrace[0].methodName + " : " + e.toString()
                )
            }
        } else {
            // shouldn't happen
            // не должно случиться
            return view ?: View(context)
        }
        JasonComponent.Companion.addListener(view, context)
        return view
    }
}