package site.app4web.app4web.Action

import android.content.Context
import android.util.Log
import org.json.JSONObject
import site.app4web.app4web.Helper.JasonHelper
import java.net.URI


class JasonSessionAction {
    operator fun set(action: JSONObject, data: JSONObject?, event: JSONObject?, context: Context) {
        try {
            val options = action.getJSONObject("options")
            val domain: String
            if (options.has("domain")) {
                var urlString = options.getString("domain")
                if (!urlString.startsWith("http")) {
                    urlString = "https://$urlString"
                }
                val uri = URI(urlString)
                domain = uri.host.toLowerCase()
            } else if (options.has("url")) {
                var urlString = options.getString("url")
                if (!urlString.startsWith("http")) {
                    urlString = "https://$urlString"
                }
                val uri = URI(urlString)
                domain = uri.host.toLowerCase()
            } else {
                return
            }

            // store either header or body under the domain name
            // сохраняем заголовок или тело под доменным именем
            val pref = context.getSharedPreferences("session", 0)
            val editor = pref.edit()

            // Stringify object first
            // Stringify объект первым
            val stringified_session = options.toString()
            editor.putString(domain, stringified_session)
            editor.commit()
            JasonHelper.next("success", action, JSONObject(), event, context)
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    }

    fun reset(action: JSONObject, data: JSONObject?, event: JSONObject?, context: Context) {
        try {
            val options = action.getJSONObject("options")
            val domain: String
            if (options.has("domain")) {
                var urlString = options.getString("domain")
                if (!urlString.startsWith("http")) {
                    urlString = "https://$urlString"
                }
                val uri = URI(urlString)
                domain = uri.host.toLowerCase()
            } else if (options.has("url")) {
                var urlString = options.getString("url")
                if (!urlString.startsWith("http")) {
                    urlString = "https://$urlString"
                }
                val uri = URI(urlString)
                domain = uri.host.toLowerCase()
            } else {
                return
            }
            val pref = context.getSharedPreferences("session", 0)
            val editor = pref.edit()
            editor.remove(domain)
            editor.commit()
            JasonHelper.next("success", action, JSONObject(), event, context)
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    }
}