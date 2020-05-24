package site.app4web.app4web.Action;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import site.app4web.app4web.Helper.JasonHelper;
import site.app4web.app4web.Core.JasonViewActivity;
import org.json.JSONObject;

public class JasonCacheAction {
    public void set(final JSONObject action, final JSONObject data, final JSONObject event, final Context context){
        try {
            JasonViewActivity activity = (JasonViewActivity) context;
            SharedPreferences pref = context.getSharedPreferences("cache", 0);
            SharedPreferences.Editor editor = pref.edit();

            // Merge with the new input
            // Слияние с новым входом
            JSONObject options = action.getJSONObject("options");
            JSONObject old_cache = new JSONObject(pref.getString(activity.url, "{}"));
            JSONObject new_cache = JasonHelper.merge(old_cache, options);

            // Update SharedPreferences
            // Обновляем SharedPreferences
            String stringified_cache = new_cache.toString();
            editor.putString(activity.url, stringified_cache);
            editor.commit();

            // Update model
            // Обновить модель
            ((JasonViewActivity)context).model.cache = new_cache;

            // Execute next
            // Выполнить дальше
            JasonHelper.next("success", action, new_cache, event, context);

        } catch (Exception e){
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }


    }
    public void reset(final JSONObject action, final JSONObject data, final JSONObject event, final Context context){
        try {
            // Update SharedPreferences
            // Обновляем SharedPreferences
            JasonViewActivity activity = (JasonViewActivity) context;
            SharedPreferences pref = context.getSharedPreferences("cache", 0);
            SharedPreferences.Editor editor = pref.edit();
            editor.remove(activity.url);
            editor.commit();

            // Update model
            // Обновить модель
            ((JasonViewActivity)context).model.cache = new JSONObject();

            // Execute next
            // Выполнить дальше
            JasonHelper.next("success", action, new JSONObject(), event, context);

        } catch (Exception e){
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
    }
}
