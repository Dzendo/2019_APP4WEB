package site.app4web.app4web.Action

import android.content.Context
import android.util.Log
import org.json.JSONObject
import site.app4web.app4web.Helper.JasonHelper


class JasonLogAction {
    fun info(action: JSONObject, data: JSONObject, event: JSONObject, context: Context) {
        log(action, data, event, context, "i")
    }

    fun debug(action: JSONObject, data: JSONObject, event: JSONObject, context: Context) {
        log(action, data, event, context, "d")
    }

    fun error(action: JSONObject, data: JSONObject, event: JSONObject, context: Context) {
        log(action, data, event, context, "e")
    }

    private fun log(
        action: JSONObject,
        data: JSONObject,
        event: JSONObject,
        context: Context,
        mode: String
    ) {
        try {
            if (action.has("options")) {
                val options = action.getJSONObject("options")
                if (options.has("text")) {
                    if (mode.equals("i", ignoreCase = true)) {
                        Log.i("Log", options.getString("text"))
                    } else if (mode.equals("d", ignoreCase = true)) {
                        Log.d("Log", options.getString("text"))
                    } else if (mode.equals("e", ignoreCase = true)) {
                        Log.e("Log", options.getString("text"))
                    }
                }
            }
            JasonHelper.next("success", action, data, event, context)
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    }
}