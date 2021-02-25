package site.app4web.app4web.Component

import android.content.*
import android.graphics.PorterDuff
import android.os.Build
import android.util.Log
import android.view.*
import android.widget.*
import org.json.JSONObject
import site.app4web.app4web.Core.JasonViewActivity
import site.app4web.app4web.Helper.JasonHelper


object JasonSwitchComponent {
    fun build(view: View?, component: JSONObject, parent: JSONObject?, context: Context): View? {
        var view = view
        return if (view == null) {
            Switch(context)
        } else {
            try {
                view = JasonComponent.build(view, component, parent, context)
                val aSwitch = view as Switch?
                var checked = false
                if (component.has("name")) {
                    if ((context as JasonViewActivity).model!!.`var`!!.has(component.getString("name"))) {
                        checked = context.model!!.`var`!!.getBoolean(component.getString("name"))
                    } else {
                        if (component.has("value")) {
                            checked = component.getBoolean("value")
                        }
                    }
                }
                val style = JasonHelper.style(component, context)
                aSwitch!!.isChecked = checked
                aSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
                    onChange(
                        aSwitch,
                        isChecked,
                        style,
                        context
                    )
                }
                changeColor(aSwitch, checked, style)
                view.requestLayout()
                view
            } catch (e: Exception) {
                Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
                View(context)
            }
        }
    }

    fun onChange(view: Switch, isChecked: Boolean, style: JSONObject?, root_context: Context) {
        changeColor(view, isChecked, style)
        val component = view.tag as JSONObject
        try {
            (root_context as JasonViewActivity).model!!.`var`!!.put(
                component.getString("name"),
                view.isChecked
            )
            if (component.has("action")) {
                val action = component.getJSONObject("action")
                root_context.call(action.toString(), JSONObject().toString(), "{}", view.context)
            }
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    }

    fun changeColor(s: Switch, isChecked: Boolean, style: JSONObject) {
        try {
            if (isChecked) {
                val color: Int
                if (style.has("color")) {
                    color = JasonHelper.parse_color(style.getString("color"))
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        s.thumbDrawable.setColorFilter(color, PorterDuff.Mode.MULTIPLY)
                        s.trackDrawable.setColorFilter(color, PorterDuff.Mode.MULTIPLY)
                    }
                } else {
                    s.thumbDrawable.clearColorFilter()
                    s.trackDrawable.clearColorFilter()
                }
            } else {
                val color: Int
                if (style.has("color:disabled")) {
                    color = JasonHelper.parse_color(style.getString("color:disabled"))
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        s.thumbDrawable.setColorFilter(color, PorterDuff.Mode.MULTIPLY)
                        s.trackDrawable.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
                    }
                } else {
                    s.thumbDrawable.clearColorFilter()
                    s.trackDrawable.clearColorFilter()
                }
            }
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    }
}