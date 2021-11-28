package site.app4web.app4web.Service.push

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import site.app4web.app4web.Core.JasonViewActivity
import site.app4web.app4web.Launcher.Launcher
import org.json.JSONArray
import org.json.JSONObject


class JasonPushMessageService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        if (remoteMessage.data.size > 0) {
            val json = remoteMessage.data
            val payload = JSONObject()
            val response = JSONObject()
            try {
                for ((key, value) in json) {
                    // Detect if the result is JSONObject, JSONArray, or String
                    val `val` = value.trim { it <= ' ' }
                    if (`val`.startsWith("[")) {
                        payload.put(key, JSONArray(`val`))
                    } else if (`val`.startsWith("{")) {
                        payload.put(key, JSONObject(`val`))
                    } else {
                        payload.put(key, `val`)
                    }
                }
                response.put("\$jason", payload)
                (Launcher.Companion.currentContext as JasonViewActivity).simple_trigger(
                    "\$push.onmessage",
                    response,
                    Launcher.Companion.currentContext as JasonViewActivity
                )
            } catch (e: Exception) {
                Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
            }
        }
    }
}