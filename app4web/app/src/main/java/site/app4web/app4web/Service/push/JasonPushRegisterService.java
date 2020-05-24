package site.app4web.app4web.Service.push;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
//import com.google.firebase.iid.FirebaseInstanceIdService;
import site.app4web.app4web.Core.JasonViewActivity;
import site.app4web.app4web.Launcher.Launcher;

import org.json.JSONObject;
// Закрыто Afalina для подъемf FireBase 19
public class JasonPushRegisterService {   //extends FirebaseInstanceIdService{
   // @Override
    public void onTokenRefresh() {
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        if (refreshedToken != null) {
            try {
                JSONObject payload = new JSONObject();
                payload.put("token", refreshedToken);
                JSONObject response = new JSONObject();
                response.put("$jason", payload);
                ((JasonViewActivity) Launcher.getCurrentContext()).simple_trigger("$push.onregister", response, Launcher.getCurrentContext());
            } catch (Exception e) {
                Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
            }
        }
    }
}
