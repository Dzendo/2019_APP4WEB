package site.app4web.app4web.Service.push

//import com.google.firebase.iid.FirebaseInstanceIdService;
import android.util.Log
import com.google.firebase.iid.FirebaseInstanceId
//import com.google.firebase.iid.FirebaseInstanceIdService;
import site.app4web.app4web.Core.JasonViewActivity
import site.app4web.app4web.Launcher.Launcher
import org.json.JSONObject


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
                (Launcher.Companion.currentContext as JasonViewActivity).simple_trigger(
                    "\$push.onregister",
                    response,
                    Launcher.Companion.currentContext as JasonViewActivity
                )
            } catch (e: Exception) {
                Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
            }
        }
    }
}