package site.app4web.app4web.Action

import android.content.Context
import android.util.Log
import org.json.JSONObject
import site.app4web.app4web.Helper.JasonHelper
import site.app4web.app4web.Launcher.Launcher

class JasonGlobalAction {
    fun reset(action: JSONObject, data: JSONObject?, event: JSONObject?, context: Context) {
        /********************
         *
         * The following resets a global variable named "db".
         * When a variable is reset, the key itself gets destroyed, so when you check ('db' in $global), it will return false
         * Следующее сбрасывает глобальную переменную с именем «db».
         * Когда переменная сбрасывается, сам ключ уничтожается, поэтому, когда вы проверяете ('db' в $ global), он возвращает false
         *
         * {
         * "type": "$global.reset",
         * "options": {
         * "items": ["db"]
         * }
         * }
         *
         */
        try {
            val pref = context.getSharedPreferences("global", 0)
            val editor = pref.edit()
            val options = action.getJSONObject("options")
            if (options.has("items")) {
                val items = options.getJSONArray("items")
                for (i in 0 until items.length()) {
                    val item = items.getString(i)
                    editor.remove(item)
                    (context.applicationContext as Launcher).resetGlobal(item)
                }
                editor.commit()
            }

            // Execute next
            // Выполнить дальше
            JasonHelper.next(
                "success",
                action,
                (context.applicationContext as Launcher).global,
                event,
                context
            )
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    }

    operator fun set(action: JSONObject, data: JSONObject?, event: JSONObject?, context: Context) {
        /********************
         *
         * The following sets a global variable named "db".
         * Следующее устанавливает глобальную переменную с именем "db".
         *
         * {
         * "type": "$global.set",
         * "options": {
         * "db": ["a", "b", "c", "d"]
         * }
         * }
         *
         * Once set, you can access them through template expressions from ANYWHERE within the app, like this:
         * После установки вы можете получить к ним доступ через выражения шаблона из ЛЮБОГО места в приложении, например:
         *
         * {
         * "items": {
         * "{{#each $global.db}}": {
         * "type": "label",
         * "text": "{{this}}"
         * }
         * }
         * }
         *
         */
        try {
            val pref = context.getSharedPreferences("global", 0)
            val editor = pref.edit()
            val options = action.getJSONObject("options")
            val keysIterator = options.keys()
            while (keysIterator.hasNext()) {
                val key = keysIterator.next()
                val `val` = options[key]
                editor.putString(key, `val`.toString())
                (context.applicationContext as Launcher).setGlobal(key, `val`)
            }
            editor.commit()

            // Execute next
            // Выполнить дальше
            JasonHelper.next(
                "success",
                action,
                (context.applicationContext as Launcher).global,
                event,
                context
            )
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    }
}