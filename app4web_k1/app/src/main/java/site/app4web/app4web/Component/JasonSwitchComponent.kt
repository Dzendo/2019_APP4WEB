package site.app4web.app4web.Component //import

import android.content.Context
import android.graphics.PorterDuff
import android.os.Build
import android.util.Log
import android.view.View
import android.widget.CompoundButton
import android.widget.Switch
import site.app4web.app4web.Core.JasonViewActivity
import site.app4web.app4web.Helper.JasonHelper
import org.json.JSONObject

object JasonSwitchComponent {
    fun build(
        view: View,
        component: JSONObject,
        parent: JSONObject?,
        context: Context
    ): View {
        var view = view
        return if (view == null) {
            Switch(context)
        } else {
            try {
                view = JasonComponent.Companion.build(view, component, parent, context)
                val aSwitch: Switch = view as Switch
                var checked = false
                if (component.has("name")) {
                    if ((context as JasonViewActivity).model?.`var`?.has(component.getString("name"))!!) {
                        checked = (context as JasonViewActivity).model?.`var`?.getBoolean(
                            component.getString("name")
                        ) ?: false
                    } else {
                        if (component.has("value")) {
                            checked = component.getBoolean("value")
                        }
                    }
                }
                val style: JSONObject = JasonHelper.style(component, context)
                aSwitch.setChecked(checked)
                aSwitch.setOnCheckedChangeListener(object : CompoundButton.OnCheckedChangeListener {
                    override fun onCheckedChanged(
                        buttonView: CompoundButton?,
                        isChecked: Boolean
                    ) {
                        onChange(aSwitch, isChecked, style, context)
                    }
                })
                changeColor(aSwitch, checked, style)
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

    fun onChange(
        view: Switch,
        isChecked: Boolean,
        style: JSONObject,
        root_context: Context
    ) {
        changeColor(view, isChecked, style)
        val component: JSONObject = view.getTag() as JSONObject
        try {
            (root_context as JasonViewActivity).model?.`var`?.put(
                component.getString("name"),
                view.isChecked()
            )
            if (component.has("action")) {
                val action: JSONObject = component.getJSONObject("action")
                (root_context as JasonViewActivity).call(
                    action.toString(),
                    JSONObject().toString(),
                    "{}",
                    view.getContext()
                )
            }
        } catch (e: Exception) {
            Log.d(
                "Warning",
                e.stackTrace[0].methodName + " : " + e.toString()
            )
        }
    }

    fun changeColor(s: Switch, isChecked: Boolean, style: JSONObject) {
        try {
            if (isChecked) {
                val color: Int
                if (style.has("color")) {
                    color = JasonHelper.parse_color(style.getString("color"))
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        s.getThumbDrawable().setColorFilter(color, PorterDuff.Mode.MULTIPLY)
                        s.getTrackDrawable().setColorFilter(color, PorterDuff.Mode.MULTIPLY)
                    }
                } else {
                    s.getThumbDrawable().clearColorFilter()
                    s.getTrackDrawable().clearColorFilter()
                }
            } else {
                val color: Int
                if (style.has("color:disabled")) {
                    color = JasonHelper.parse_color(style.getString("color:disabled"))
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        s.getThumbDrawable().setColorFilter(color, PorterDuff.Mode.MULTIPLY)
                        s.getTrackDrawable().setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
                    }
                } else {
                    s.getThumbDrawable().clearColorFilter()
                    s.getTrackDrawable().clearColorFilter()
                }
            }
        } catch (e: Exception) {
            Log.d(
                "Warning",
                e.stackTrace[0].methodName + " : " + e.toString()
            )
        }
    }
}