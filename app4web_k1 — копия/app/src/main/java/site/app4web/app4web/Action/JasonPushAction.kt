package site.app4web.app4web.Action

import android.content.Context
import android.util.Log
import com.google.firebase.iid.FirebaseInstanceId
import org.json.JSONObject
import site.app4web.app4web.Core.JasonViewActivity
import site.app4web.app4web.Helper.JasonHelper
import site.app4web.app4web.Launcher.Launcher

// Unlike APN which requires manual registration, FCM automatically registers push upon app launch.
// As a result, $push.register can behave in two different ways:
// 1. If the device is already registered, it immediately triggers $push.onregister event.
// 2. If the device is NOT yet registered, it doesn't do anything (the $push.onregister event will be auto-triggered by JasonPushRegisterService instead)
// В отличие от APN, который требует ручной регистрации, FCM автоматически регистрирует push при запуске приложения.
// В результате $push.register может вести себя двумя разными способами:
// 1. Если устройство уже зарегистрировано, оно немедленно вызывает событие $push.onregister.
// 2. Если устройство еще не зарегистрировано, оно ничего не делает (событие $push.onregister будет автоматически вызываться JasonPushRegisterService)
class JasonPushAction {
    fun register(
        action: JSONObject?,
        data: JSONObject?,
        event: JSONObject?,
        context: Context?
    ) {
        val refreshedToken = FirebaseInstanceId.getInstance().token
        if (refreshedToken != null) {
            // Token exists => already registered => Immediately trigger $push.onregister
            try {
                val response = JSONObject()
                val payload = JSONObject()
                payload.put("token", refreshedToken)
                response.put("\$jason", payload)
                (Launcher.currentContext as JasonViewActivity).simple_trigger(
                    "\$push.onregister",
                    response,
                    (Launcher.currentContext as JasonViewActivity)
                )
            } catch (e: Exception) {
                Log.d(
                    "Warning",
                    e.stackTrace[0].methodName + " : " + e.toString()
                )
            }
        } else {
            // Token doesn't exist => ignore => JasonPushRegisterService will take care of $push.onregister
            // Токен не существует => ignore => JasonPushRegisterService позаботится о $push.onregister
        }
        JasonHelper.next("success", action, data, event, context)
    }
}