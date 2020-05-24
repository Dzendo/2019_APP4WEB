package site.app4web.app4web.Component

import android.R
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.View
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import org.json.JSONObject
import site.app4web.app4web.Core.JasonViewActivity
import site.app4web.app4web.Helper.JasonHelper
import site.app4web.app4web.Section.JasonLayout

open class JasonComponent {
    companion object {
        const val INTENT_ACTION_CALL = "call"
        const val ACTION_PROP = "action"
        const val DATA_PROP = "data"
        const val HREF_PROP = "href"
        const val TYPE_PROP = "type"
        const val OPTIONS_PROP = "options"
        fun build(
            view: View,
            component: JSONObject?,
            parent: JSONObject?,
            root_context: Context
        ): View {
            var width = 0f
            var height = 0f
            val corner_radius = 0
            view.tag = component
            val style = JasonHelper.style(component, root_context)
            return try {
                if (parent == null) {
                    // Layer type
                    width = RelativeLayout.LayoutParams.WRAP_CONTENT.toFloat()
                    height = RelativeLayout.LayoutParams.WRAP_CONTENT.toFloat()
                    if (style!!.has("height")) {
                        try {
                            height = JasonHelper.pixels(
                                root_context,
                                style.getString("height"),
                                "vertical"
                            ) .toFloat()
                            if (style.has("ratio")) {
                                val ratio =
                                    JasonHelper.ratio(style.getString("ratio"))
                                width = height * ratio
                            }
                        } catch (e: Exception) {
                            Log.d(
                                "Warning",
                                e.stackTrace[0].methodName + " : " + e.toString()
                            )
                        }
                    }
                    if (style.has("width")) {
                        try {
                            width = JasonHelper.pixels(
                                root_context,
                                style.getString("width"),
                                "horizontal"
                            ) .toFloat()
                            if (style.has("ratio")) {
                                val ratio =
                                    JasonHelper.ratio(style.getString("ratio"))
                                height = width / ratio
                            }
                        } catch (e: Exception) {
                            Log.d(
                                "Warning",
                                e.stackTrace[0].methodName + " : " + e.toString()
                            )
                        }
                    }
                    val layoutParams =
                        RelativeLayout.LayoutParams(width.toInt(), height.toInt())
                    view.layoutParams = layoutParams
                } else {
                    // Section item type
                    // Тип элемента раздела
                    val layoutParams =
                        JasonLayout.autolayout(null, parent, component, root_context)
                    view.layoutParams = layoutParams
                }
                val bgcolor: Int
                if (style!!.has("background")) {
                    val color = JasonHelper.parse_color(style.getString("background"))
                    bgcolor = color
                    view.setBackgroundColor(color)
                } else {
                    bgcolor = JasonHelper.parse_color("rgba(0,0,0,0)")
                }
                if (style.has("opacity")) {
                    try {
                        val opacity = style.getDouble("opacity").toFloat()
                        view.alpha = opacity
                    } catch (ex: Exception) {
                    }
                }


                // padding
                // заполнение
                var padding_left = JasonHelper.pixels(root_context, "0", "horizontal").toInt()
                var padding_right = JasonHelper.pixels(root_context, "0", "horizontal").toInt()
                var padding_top = JasonHelper.pixels(root_context, "0", "horizontal").toInt()
                var padding_bottom = JasonHelper.pixels(root_context, "0", "horizontal").toInt()
                if (style.has("padding")) {
                    padding_left = JasonHelper.pixels(
                        root_context,
                        style.getString("padding"),
                        "horizontal"
                    ).toInt()
                    padding_right = padding_left
                    padding_top = padding_left
                    padding_bottom = padding_left
                }

                // overwrite if more specific values exist
                // перезаписать, если существуют более конкретные значения
                if (style.has("padding_left")) {
                    padding_left = JasonHelper.pixels(
                        root_context,
                        style.getString("padding_left"),
                        "horizontal"
                    ).toInt()
                }
                if (style.has("padding_right")) {
                    padding_right = JasonHelper.pixels(
                        root_context,
                        style.getString("padding_right"),
                        "horizontal"
                    ).toInt()
                }
                if (style.has("padding_top")) {
                    padding_top = JasonHelper.pixels(
                        root_context,
                        style.getString("padding_top"),
                        "vertical"
                    ).toInt()
                }
                if (style.has("padding_bottom")) {
                    padding_bottom = JasonHelper.pixels(
                        root_context,
                        style.getString("padding_bottom"),
                        "vertical"
                    ).toInt()
                }
                if (style.has("corner_radius")) {
                    val corner = JasonHelper.pixels(
                        root_context,
                        style.getString("corner_radius"),
                        "horizontal"
                    )
                    var color =
                        ContextCompat.getColor(root_context, R.color.transparent)
                    val cornerShape = GradientDrawable()
                    cornerShape.shape = GradientDrawable.RECTANGLE
                    if (style.has("background")) {
                        color = JasonHelper.parse_color(style.getString("background"))
                    }
                    cornerShape.setColor(color)
                    cornerShape.cornerRadius = corner

                    // border + corner_radius handling
                    // граница + обработка радиуса угла
                    if (style.has("border_width")) {
                        val border_width = JasonHelper.pixels(
                            root_context,
                            style.getString("border_width"),
                            "horizontal"
                        ).toInt()
                        if (border_width > 0) {
                            val border_color: Int
                            border_color = if (style.has("border_color")) {
                                JasonHelper.parse_color(style.getString("border_color"))
                            } else {
                                JasonHelper.parse_color("#000000")
                            }
                            cornerShape.setStroke(border_width, border_color)
                        }
                    }
                    cornerShape.invalidateSelf()
                    view.background = cornerShape
                } else {
                    // border handling (no corner radius)
                    // обработка границы (без углового радиуса)
                    if (style.has("border_width")) {
                        val border_width = JasonHelper.pixels(
                            root_context,
                            style.getString("border_width"),
                            "horizontal"
                        ).toInt()
                        if (border_width > 0) {
                            val border_color: Int
                            border_color = if (style.has("border_color")) {
                                JasonHelper.parse_color(style.getString("border_color"))
                            } else {
                                JasonHelper.parse_color("#000000")
                            }
                            val cornerShape = GradientDrawable()
                            cornerShape.setStroke(border_width, border_color)
                            cornerShape.setColor(bgcolor)
                            cornerShape.invalidateSelf()
                            view.background = cornerShape
                        }
                    }
                }
                view.setPadding(padding_left, padding_top, padding_right, padding_bottom)
                view
            } catch (e: Exception) {
                Log.d(
                    "Warning",
                    e.stackTrace[0].methodName + " : " + e.toString()
                )
                View(root_context)
            }
        }

        fun addListener(
            view: View?,
            root_context: Context
        ) {
            val clickListener =
                View.OnClickListener { v ->
                    val component = v.tag as JSONObject
                    try {
                        if (component.has("action")) {
                            val action = component.getJSONObject("action")
                            (root_context as JasonViewActivity).call(
                                action.toString(),
                                JSONObject().toString(),
                                "{}",
                                v.context
                            )
                        } else if (component.has("href")) {
                            val href = component.getJSONObject("href")
                            val action =
                                JSONObject().put("type", "\$href").put("options", href)
                            (root_context as JasonViewActivity).call(
                                action.toString(),
                                JSONObject().toString(),
                                "{}",
                                v.context
                            )
                        } else {
                            // NONE Explicitly stated.
                            // Need to bubble up all the way to the root viewholder.
                            var cursor = view
                            while (cursor!!.parent != null) {
                                val item =
                                    (cursor.parent as View).tag as JSONObject
                                cursor =
                                    if (item != null && (item.has("action") || item.has("href"))) {
                                        (cursor.parent as View).performClick()
                                        break
                                    } else {
                                        cursor.parent as View
                                    }
                            }
                        }
                    } catch (e: Exception) {
                        Log.d(
                            "Warning",
                            e.stackTrace[0].methodName + " : " + e.toString()
                        )
                    }
                }
            view!!.setOnClickListener(clickListener)
        }
    }
}