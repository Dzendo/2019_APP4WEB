package site.app4web.app4web.Action

import android.content.Context
import android.util.Log
import org.json.JSONObject
import site.app4web.app4web.Core.JasonViewActivity
import site.app4web.app4web.Helper.JasonHelper


class JasonCacheAction {
    operator fun set(action: JSONObject, data: JSONObject?, event: JSONObject?, context: Context) {
        try {
            val activity = context as JasonViewActivity
            val pref = context.getSharedPreferences("cache", 0)
            val editor = pref.edit()

            // Merge with the new input
            // Слияние с новым входом
            val options = action.getJSONObject("options")
            val old_cache = JSONObject(pref.getString(activity.url, "{}"))
            val new_cache = JasonHelper.merge(old_cache, options)

            // Update SharedPreferences
            // Обновляем SharedPreferences
            val stringified_cache = new_cache.toString()
            editor.putString(activity.url, stringified_cache)
            editor.commit()

            // Update model
            // Обновить модель
            context.model!!.cache = new_cache

            // Execute next
            // Выполнить дальше
            JasonHelper.next("success", action, new_cache, event, context)
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    }

    fun reset(action: JSONObject?, data: JSONObject?, event: JSONObject?, context: Context) {
        try {
            // Update SharedPreferences
            // Обновляем SharedPreferences
            val activity = context as JasonViewActivity
            val pref = context.getSharedPreferences("cache", 0)
            val editor = pref.edit()
            editor.remove(activity.url)
            editor.commit()

            // Update model
            // Обновить модель
            context.model!!.cache = JSONObject()

            // Execute next
            // Выполнить дальше
            JasonHelper.next("success", action, JSONObject(), event, context)
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    }
}