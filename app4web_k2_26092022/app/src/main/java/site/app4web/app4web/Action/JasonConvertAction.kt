package site.app4web.app4web.Action

import android.content.Context
import android.util.Log
import com.eclipsesource.v8.JavaVoidCallback
import com.eclipsesource.v8.V8
import com.eclipsesource.v8.V8Array
import com.eclipsesource.v8.V8Object
import org.json.JSONObject
import site.app4web.app4web.Helper.JasonHelper


class JasonConvertAction {
    private var action: JSONObject? = null
    private var context: Context? = null
    private var event_cache: JSONObject? = null
    fun csv(action: JSONObject, data: JSONObject?, event: JSONObject?, context: Context?) {
        this.action = action
        this.context = context
        event_cache = event
        try {
            val options = action.getJSONObject("options")
            var result = "[]"
            if (options.has("data")) {
                val csv_data = options.getString("data")
                if (!csv_data.isEmpty()) {
                    val js = JasonHelper.read_file("csv", context)
                    val runtime = V8.createV8Runtime()
                    runtime.executeVoidScript(js)
                    val csv = runtime.getObject("csv")
                    val parameters = V8Array(runtime).push(csv_data)
                    val `val` = csv.executeArrayFunction("run", parameters)
                    parameters.release()
                    csv.release()
                    result = stringify(runtime, `val`)
                    runtime.release()
                }
            }
            JasonHelper.next("success", action, result, event, context)
        } catch (e: Exception) {
            handle_exception(e)
        }
    }

    fun rss(action: JSONObject, data: JSONObject?, event: JSONObject?, context: Context?) {
        this.action = action
        this.context = context
        event_cache = event
        try {
            val options = action.getJSONObject("options")
            val rss_data = options.getString("data")
            val js = JasonHelper.read_file("rss", context)
            val timers = JasonHelper.read_file("timers", context)
            val runtime = V8.createV8Runtime()
            runtime.executeVoidScript(js)

            // Shim to support javascript timer functions in V8
            // Shim для поддержки функций таймера JavaScript в V8
            runtime.registerJavaMethod(Sleep(), "sleep")
            runtime.executeVoidScript(timers)
            runtime.executeVoidScript("var timerLoop = makeWindowTimer(this, sleep);")
            val rss = runtime.getObject("rss")
            val parameters = V8Array(runtime).push(rss_data)
            // Register a callback to receive the RSS JSON data
            // Регистрация обратного вызова для получения данных RSS JSON
            runtime.registerJavaMethod(RSSCallback(), "callback")
            rss.executeObjectFunction("run", parameters)

            // Now we need to kick off the timer loop to get the parsing started
            // Теперь нам нужно запустить цикл таймера, чтобы начать анализ
            runtime.executeVoidScript("timerLoop()")
            parameters.release()
            rss.release()
            runtime.release()
        } catch (e: Exception) {
            handle_exception(e)
        }
    }

    /**
     * Converts a JSON object to a string using the javascript method `JSON.stringify`
     * Преобразует объект JSON в строку, используя метод javascript `JSON.stringify`
     * @param runtime     a V8 runtime
     * @param jsonObject  a JSON object (V8Object)
     * @return            a String representation of the JSON object
     * @return            a строковое представление объекта JSON
     */
    private fun stringify(runtime: V8, jsonObject: V8Object): String {
        val parameters = V8Array(runtime).push(jsonObject)
        val json = runtime.getObject("JSON")
        val result = json.executeStringFunction("stringify", parameters)
        parameters.release()
        jsonObject.release()
        json.release()
        return result
    }

    /**
     * Handles an exception by passing the error to JasonHelper.next if possible, otherwise log
     * the output
     * Обрабатывает исключение, передавая ошибку в JasonHelper.next, если это возможно, иначе войдите
     * выход
     * @param exc  Exception
     */
    private fun handle_exception(exc: Exception) {
        try {
            val error = JSONObject()
            error.put("data", exc.toString())
            JasonHelper.next("error", action, error, event_cache, context)
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    }

    /**
     * Callback to handle the sleep function called from timer javascript code. Simply sleeps for
     * the number of milliseconds passed in the arguments.
     * Обратный вызов для обработки функции сна, вызываемой из таймера кода JavaScript. Просто спит
     * количество миллисекунд, переданных в аргументах.
     */
    internal inner class Sleep : JavaVoidCallback {
        override fun invoke(receiver: V8Object, parameters: V8Array) {
            try {
                Thread.sleep(parameters[0] as Long)
            } catch (e: InterruptedException) {
                handle_exception(e)
            }
        }
    }

    /**
     * Callback that gets run at the end of RSS parsing. The one parameter is the RSS data, as a
     * V8Array
     * Обратный вызов, который запускается в конце разбора RSS. Один параметр - это данные RSS, как
     * V8Array
     */
    internal inner class RSSCallback : JavaVoidCallback {
        override fun invoke(receiver: V8Object, parameters: V8Array) {
            try {
                val rss_data = parameters[0] as V8Array
                val result = stringify(receiver.runtime, rss_data)
                JasonHelper.next("success", action, result, event_cache, context)
            } catch (e: Exception) {
                handle_exception(e)
            }
        }
    }
}