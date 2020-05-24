package site.app4web.app4web.Action

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import site.app4web.app4web.Core.JasonParser
import site.app4web.app4web.Core.JasonRequire
import site.app4web.app4web.Helper.JasonHelper
import site.app4web.app4web.Launcher.Launcher
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

/**
 * Created by e on 9/14/17.
 * Создано e on 14.09.17.
 */
class JasonScriptAction {
    fun include(
        action: JSONObject,
        data: JSONObject?,
        event: JSONObject?,
        context: Context
    ) {
        try {
            val options = action.getJSONObject("options")
            if (options.has("items")) {
                val items = options.getJSONArray("items")
                val refs = JSONObject()
                val client =
                    (context.applicationContext as Launcher).getHttpClient(0)
                val urlItems = JSONArray()
                val inlineItems = JSONArray()
                for (i in 0 until items.length()) {
                    val item = items[i] as JSONObject
                    if (item.has("url")) {
                        urlItems.put(item.getString("url"))
                    } else if (item.has("text")) {
                        inlineItems.put(item.getString("text"))
                    }
                }
                if (urlItems.length() > 0) {
                    val latch = CountDownLatch(urlItems.length())
                    val taskExecutor =
                        Executors.newFixedThreadPool(urlItems.length())
                    for (i in 0 until urlItems.length()) {
                        val url = urlItems.getString(i)
                        taskExecutor.submit(JasonRequire(url, latch, refs, client, context))
                    }
                    try {
                        latch.await()
                    } catch (e: Exception) {
                        Log.d(
                            "Warning",
                            e.stackTrace[0].methodName + " : " + e.toString()
                        )
                    }
                }

                // remote inject
                // удаленный ввод
                val keys: Iterator<*> = refs.keys()
                while (keys.hasNext()) {
                    val key = keys.next()!!
                    val js = refs.getString(key as String)
                    JasonParser.Companion.inject(js)
                }

                // local inject (inline)
                // локальный ввод (встроенный)
                for (i in 0 until inlineItems.length()) {
                    val js = inlineItems.getString(i)
                    JasonParser.Companion.inject(js)
                }
                JasonHelper.next("success", action, data, event, context)
            }
        } catch (e: Exception) {
            Log.d(
                "Warning",
                e.stackTrace[0].methodName + " : " + e.toString()
            )
        }
    }

    fun clear(
        action: JSONObject?,
        data: JSONObject?,
        event: JSONObject?,
        context: Context?
    ) {
        JasonParser.Companion.reset()
        JasonHelper.next("success", action, data, event, context)
    }
}