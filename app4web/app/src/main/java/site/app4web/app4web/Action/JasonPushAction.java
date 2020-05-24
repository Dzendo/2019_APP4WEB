package site.app4web.app4web.Action;

import android.content.Context;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import site.app4web.app4web.Core.JasonViewActivity;
import site.app4web.app4web.Helper.JasonHelper;
import site.app4web.app4web.Launcher.Launcher;

import org.json.JSONObject;

// Unlike APN which requires manual registration, FCM automatically registers push upon app launch.
// As a result, $push.register can behave in two different ways:
// 1. If the device is already registered, it immediately triggers $push.onregister event.
// 2. If the device is NOT yet registered, it doesn't do anything (the $push.onregister event will be auto-triggered by JasonPushRegisterService instead)
// В отличие от APN, который требует ручной регистрации, FCM автоматически регистрирует push при запуске приложения.
// В результате $push.register может вести себя двумя разными способами:
// 1. Если устройство уже зарегистрировано, оно немедленно вызывает событие $push.onregister.
// 2. Если устройство еще не зарегистрировано, оно ничего не делает (событие $push.onregister будет автоматически вызываться JasonPushRegisterService)

public class JasonPushAction {
    public void register(final JSONObject action, JSONObject data, final JSONObject event, Context context) {
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        if(refreshedToken != null){
            // Token exists => already registered => Immediately trigger $push.onregister
            try {
                JSONObject response = new JSONObject();
                JSONObject payload = new JSONObject();
                payload.put("token", refreshedToken);
                response.put("$jason", payload);
                ((JasonViewActivity)Launcher.getCurrentContext()).simple_trigger("$push.onregister", response, Launcher.getCurrentContext());
            } catch (Exception e) {
                Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
            }
        } else {
            // Token doesn't exist => ignore => JasonPushRegisterService will take care of $push.onregister
            // Токен не существует => ignore => JasonPushRegisterService позаботится о $push.onregister
        }
        JasonHelper.next("success", action, data, event, context);
    }
}
