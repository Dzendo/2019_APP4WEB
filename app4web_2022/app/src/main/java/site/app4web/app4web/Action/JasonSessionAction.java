package site.app4web.app4web.Action;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import site.app4web.app4web.Helper.JasonHelper;
import org.json.JSONObject;
import java.net.URI;

public class JasonSessionAction {
    public void set(final JSONObject action, final JSONObject data, final JSONObject event, final Context context){
        try {
            JSONObject options = action.getJSONObject("options");
            String domain;
            if(options.has("domain")){
                String urlString = options.getString("domain");
                if(!urlString.startsWith("http")){
                    urlString = "https://" + urlString;
                }
                URI uri = new URI(urlString);
                domain = uri.getHost().toLowerCase();
            }else if(options.has("url")){
                String urlString = options.getString("url");
                if(!urlString.startsWith("http")){
                    urlString = "https://" + urlString;
                }
                URI uri = new URI(urlString);
                domain = uri.getHost().toLowerCase();
            } else {
                return;
            }

            // store either header or body under the domain name
            // сохраняем заголовок или тело под доменным именем
            SharedPreferences pref = context.getSharedPreferences("session", 0);
            SharedPreferences.Editor editor = pref.edit();

            // Stringify object first
            // Stringify объект первым
            String stringified_session = options.toString();
            editor.putString(domain, stringified_session);
            editor.commit();

            JasonHelper.next("success", action, new JSONObject(), event, context);

        } catch (Exception e){
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }


    }
    public void reset(final JSONObject action, final JSONObject data, final JSONObject event, final Context context){
        try {
            JSONObject options = action.getJSONObject("options");
            String domain;
            if(options.has("domain")){
                String urlString = options.getString("domain");
                if(!urlString.startsWith("http")){
                    urlString = "https://" + urlString;
                }
                URI uri = new URI(urlString);
                domain = uri.getHost().toLowerCase();
            }else if(options.has("url")){
                String urlString = options.getString("url");
                if(!urlString.startsWith("http")){
                    urlString = "https://" + urlString;
                }
                URI uri = new URI(urlString);
                domain = uri.getHost().toLowerCase();
            } else {
                return;
            }

            SharedPreferences pref = context.getSharedPreferences("session", 0);
            SharedPreferences.Editor editor = pref.edit();
            editor.remove(domain);
            editor.commit();

            JasonHelper.next("success", action, new JSONObject(), event, context);

        } catch (Exception e){
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
    }
}
