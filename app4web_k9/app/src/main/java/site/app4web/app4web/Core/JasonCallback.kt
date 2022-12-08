package site.app4web.app4web.Core

import android.content.Context
import android.content.Intent
import android.util.Log
import org.json.JSONObject
import site.app4web.app4web.Helper.JasonHelper

class JasonCallback {
    fun href(intent: Intent, options: JSONObject) {
        try {
            val action = options.getJSONObject("action")
            val event = options.getJSONObject("event")
            val context = options["context"] as Context
            val return_string = intent.getStringExtra("return")
            val return_value = JSONObject(return_string!!)
            JasonHelper.next("success", action, return_value, event, context)
        } catch (e: Exception) {
            Log.d(
                "Warning",
                e.stackTrace[0].methodName + " : " + e.toString()
            )
        }
    }
}