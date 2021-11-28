package site.app4web.app4web.Component

import android.content.Context
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.TextView
import site.app4web.app4web.Core.JasonViewActivity
import org.json.JSONObject
import java.util.HashMap


class JasonComponentFactory {
    var signature_to_type: Map<String, Int> = HashMap()

    companion object {
        fun build(
            prototype: View?,
            component: JSONObject,
            parent: JSONObject?,
            context: Context
        ): View {
            try {
                val type: String
                type = component.getString("type")
                val view: View
                if (type.equals("label", ignoreCase = true)) {
                    view = JasonLabelComponent.build(prototype, component, parent, context)
                } else if (type.equals("image", ignoreCase = true)) {
                    view = JasonImageComponent.build(prototype, component, parent, context)
                } else if (type.equals("button", ignoreCase = true)) {
                    view = JasonButtonComponent.build(prototype, component, parent, context)
                } else if (type.equals("space", ignoreCase = true)) {
                    view = JasonSpaceComponent.build(prototype, component, parent, context)
                } else if (type.equals("textfield", ignoreCase = true)) {
                    view = JasonTextfieldComponent.build(prototype, component, parent, context)
                } else if (type.equals("textarea", ignoreCase = true)) {
                    view = JasonTextareaComponent.build(prototype, component, parent, context)
                } else if (type.equals("html", ignoreCase = true)) {
                    view = JasonHtmlComponent.build(prototype, component, parent, context)
                } else if (type.equals("map", ignoreCase = true)) {
                    view = JasonMapComponent.build(prototype, component, parent, context)
                } else if (type.equals("slider", ignoreCase = true)) {
                    view = JasonSliderComponent.build(prototype, component, parent, context)
                } else if (type.equals("switch", ignoreCase = true)) {
                    view = JasonSwitchComponent.build(prototype, component, parent, context)
                } else {
                    // Non-existent component warning
                    // Предупреждение о несуществующем компоненте
                    val error_component = JSONObject(component.toString())
                    error_component.put("type", "label")
                    error_component.put(
                        "text", """
     ${"$"}${component.getString("type")}
     (not implemented yet)
     """.trimIndent()
                    )
                    view = JasonLabelComponent.build(prototype, error_component, parent, context)
                    (view as TextView).gravity = Gravity.CENTER
                }

                // Focus textfield/textarea
                // Фокус текстового поля / textarea
                if (component.has("focus")) {
                    (context as JasonViewActivity).focusView = view
                }
                return view
            } catch (e: Exception) {
                Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
            }
            return View(context)
        }
    }
}