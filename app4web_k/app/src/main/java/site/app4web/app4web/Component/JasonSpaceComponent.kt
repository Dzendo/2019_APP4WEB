package site.app4web.app4web.Component

import android.content.Context
import android.util.Log
import android.view.View
import org.json.JSONObject

object JasonSpaceComponent {
    fun build(
        view: View?,
        component: JSONObject?,
        parent: JSONObject?,
        context: Context
    ): View {
        return if (view == null) {
            View(context)
        } else {
            try {
                JasonComponent.Companion.build(view, component, parent, context)
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