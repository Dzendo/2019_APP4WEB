package site.app4web.app4web.Action;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import site.app4web.app4web.Core.JasonViewActivity;
import site.app4web.app4web.Helper.JasonHelper;
import site.app4web.app4web.Launcher.Launcher;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Iterator;

public class JasonGlobalAction {
    public void reset(final JSONObject action, final JSONObject data, final JSONObject event, final Context context) {

        /********************

         The following resets a global variable named "db".
         When a variable is reset, the key itself gets destroyed, so when you check ('db' in $global), it will return false
         Следующее сбрасывает глобальную переменную с именем «db».
         Когда переменная сбрасывается, сам ключ уничтожается, поэтому, когда вы проверяете ('db' в $ global), он возвращает false

         {
             "type": "$global.reset",
             "options": {
                 "items": ["db"]
             }
         }

         ********************/

        try {
            SharedPreferences pref = context.getSharedPreferences("global", 0);
            SharedPreferences.Editor editor = pref.edit();

            JSONObject options = action.getJSONObject("options");
            if(options.has("items")){
                JSONArray items = options.getJSONArray("items");
                for (int i=0; i<items.length(); i++) {
                    String item = items.getString(i);
                    editor.remove(item);
                    ((Launcher)context.getApplicationContext()).resetGlobal(item);
                }
                editor.commit();
            }

            // Execute next
            // Выполнить дальше
            JasonHelper.next("success", action, ((Launcher)context.getApplicationContext()).getGlobal(), event, context);

        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }

    }
    public void set(final JSONObject action, final JSONObject data, final JSONObject event, final Context context) {

        /********************

         The following sets a global variable named "db".
         Следующее устанавливает глобальную переменную с именем "db".

         {
             "type": "$global.set",
             "options": {
                 "db": ["a", "b", "c", "d"]
             }
         }

         Once set, you can access them through template expressions from ANYWHERE within the app, like this:
         После установки вы можете получить к ним доступ через выражения шаблона из ЛЮБОГО места в приложении, например:

         {
             "items": {
                 "{{#each $global.db}}": {
                     "type": "label",
                     "text": "{{this}}"
                 }
             }
         }

         ********************/

        try {
            SharedPreferences pref = context.getSharedPreferences("global", 0);
            SharedPreferences.Editor editor = pref.edit();

            JSONObject options = action.getJSONObject("options");

            Iterator<String> keysIterator = options.keys();
            while (keysIterator.hasNext()) {
                String key = keysIterator.next();
                Object val = options.get(key);
                editor.putString(key, val.toString());
                ((Launcher)context.getApplicationContext()).setGlobal(key, val);
            }
            editor.commit();

            // Execute next
            // Выполнить дальше
            JasonHelper.next("success", action, ((Launcher)context.getApplicationContext()).getGlobal(), event, context);

        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
    }
}
