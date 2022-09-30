package site.app4web.app4web.Service.push

import android.util.Log
import com.google.firebase.iid.FirebaseInstanceId
import org.json.JSONObject
import site.app4web.app4web.Core.JasonViewActivity
import site.app4web.app4web.Launcher.Launcher



//import com.google.firebase.iid.FirebaseInstanceIdService;
// Закрыто Afalina для подъемf FireBase 19
class JasonPushRegisterService {
    //extends FirebaseInstanceIdService{
    // @Override
    fun onTokenRefresh() {
        val refreshedToken = FirebaseInstanceId.getInstance().token
        if (refreshedToken != null) {
            try {
                val payload = JSONObject()
                payload.put("token", refreshedToken)
                val response = JSONObject()
                response.put("\$jason", payload)
                (Launcher.Companion.getCurrentContext() as JasonViewActivity).simple_trigger(
                    "\$push.onregister",
                    response,
                    Launcher.Companion.getCurrentContext()
                )
            } catch (e: Exception) {
                Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
            }
        }
    }
}