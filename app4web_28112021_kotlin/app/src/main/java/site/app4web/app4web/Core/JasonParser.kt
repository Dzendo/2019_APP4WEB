package site.app4web.app4web.Core

import android.content.Context
import android.util.Log
import com.eclipsesource.v8.V8
import com.eclipsesource.v8.V8Array
import com.eclipsesource.v8.V8Object
import site.app4web.app4web.Helper.JasonHelper
import org.json.JSONObject
import java.util.concurrent.LinkedBlockingQueue
import timber.log.Timber

class JasonParser private constructor() {
    interface JasonParserListener {
        fun onFinished(json: JSONObject?)
    }

    private var listener: JasonParserListener? = null
    private var juice: V8? = null
    fun setParserListener(listener: JasonParserListener?) {
        this.listener = listener
    }

    private inner class Task(var data_type: String, var data: JSONObject, var template: Any)

    private val taskQueue = LinkedBlockingQueue<Task>()
    private fun processTask(task: Task) {
        try {
            val template = task.template
            val data = task.data
            val data_type = task.data_type

            // thread handling - acquire handle
            // обработка потока - получить дескриптор
            juice!!.locker.acquire()
            val console = Console()
            val v8Console = V8Object(juice)
            juice!!.add("console", v8Console)
            v8Console.registerJavaMethod(
                console,
                "log",
                "log",
                arrayOf<Class<*>>(String::class.java)
            )
            v8Console.registerJavaMethod(
                console, "error", "error", arrayOf<Class<*>>(
                    String::class.java
                )
            )
            v8Console.registerJavaMethod(console, "trace", "trace", arrayOf())
            val templateJson = template.toString()
            val dataJson = data.toString()
            var `val` = "{}"
            val parser = juice!!.getObject("ST")
            // Get global variables (excluding natively injected variables which will never be used in the template)
            // Получить глобальные переменные (исключая встроенные переменные, которые никогда не будут использоваться в шаблоне)
            val globals =
                juice!!.executeStringScript("JSON.stringify(Object.keys(this).filter(function(key){return ['ST', 'to_json', 'setImmediate', 'clearImmediate', 'console'].indexOf(key) === -1;}));")
            if (data_type.equals("json", ignoreCase = true)) {
                val parameters = V8Array(juice)
                parameters.push(templateJson)
                parameters.push(dataJson)
                parameters.push(globals)
                parameters.push(true)
                `val` = parser.executeStringFunction("transform", parameters)
                parameters.release()
            } else {
                val raw_data = data.getString("\$jason")
                val parameters = V8Array(juice).push("html")
                parameters.push(templateJson)
                parameters.push(raw_data)
                parameters.push(globals)
                parameters.push(true)
                `val` = juice!!.executeStringFunction("to_json", parameters)
                parameters.release()
            }
            parser.release()
            v8Console.release()
            if (`val`.equals("null", ignoreCase = true)) {
                res = JSONObject()
            } else {
                res = JSONObject(`val`)
            }
            listener!!.onFinished(res)
        } catch (e: Exception) {
            Timber.w(e.stackTrace[0].methodName + " : " + e.toString())
        }

        // thread handling - release handle
        // обработка потока - дескриптор освобождения
        juice!!.locker.release()
    }

    private var thread: Thread? = null
    fun parse(data_type: String?, data: JSONObject?, template: Any?, context: Context?) {
        try {
            taskQueue.put(Task(data_type, data, template))
            if (thread == null) {
                thread = Thread {
                    while (true) {
                        try {
                            if (taskQueue.size > 0) {
                                processTask(taskQueue.take())
                            }
                            if (taskQueue.size == 0) {
                                thread = null
                                break
                            }
                        } catch (e: Exception) {
                            Timber.w(e.stackTrace[0].methodName + " : " + e.toString())
                        }
                    }
                }
                thread!!.start()
            } else {
            }
        } catch (e: Exception) {
            Timber.w(e.stackTrace[0].methodName + " : " + e.toString())
        }
    }

    companion object {
        var res: JSONObject? = null
        private val context: Context? = null
        private var instance: JasonParser? = null
        fun getInstance(context: Context?): JasonParser? {
            if (instance == null) {
                instance = JasonParser()
                try {
                    val js = JasonHelper.read_file("st", context)
                    val xhtmljs = JasonHelper.read_file("xhtml", context)
                    instance!!.juice = V8.createV8Runtime()
                    instance!!.juice.executeVoidScript(js)
                    instance!!.juice.executeVoidScript(xhtmljs)
                    instance!!.juice.getLocker().release()
                } catch (e: Exception) {
                    Timber.w(e.stackTrace[0].methodName + " : " + e.toString())
                }
            }
            return instance
        }

        fun inject(js: String?) {
            instance!!.juice!!.executeVoidScript(js)
        }

        fun reset() {
            try {
                val js = JasonHelper.read_file("st", context)
                val xhtmljs = JasonHelper.read_file("xhtml", context)
                instance!!.juice = V8.createV8Runtime()
                instance!!.juice.executeVoidScript(js)
                instance!!.juice.executeVoidScript(xhtmljs)
                instance!!.juice.getLocker().release()
            } catch (e: Exception) {
                Timber.w(e.stackTrace[0].methodName + " : " + e.toString())
            }
        }
    }
}

/**
 * Override for console to print javascript debug output in the Android Studio console
 * Переопределить консоль для печати отладочного вывода JavaScript в консоли Android Studio.
 */
internal class Console {
    fun log(message: String?) {
        Timber.d(message)
    }

    fun error(message: String?) {
        Timber.e(message)
    }

    fun trace() {
        Timber.e("Unable to reproduce JS stacktrace")
    } // Невозможно воспроизвести JS stacktrace
}