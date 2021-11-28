package site.app4web.app4web.Action

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.HandlerThread
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.util.Log
import site.app4web.app4web.Helper.JasonHelper
import org.json.JSONObject
import java.util.HashMap


class JasonTimerAction {
    private val timers: HashMap<String?, Runnable?>
    private val handler: Handler
    fun start(action: JSONObject, data: JSONObject?, event: JSONObject?, context: Context?) {
        try {
            if (action.has("options")) {
                val options = action.getJSONObject("options")
                if (options.has("name")) {
                    val name = options.getString("name")

                    // Look up timer
                    // if it exists, reset first, and then start
                    // ищем таймер
                    // если он существует, сначала выполните сброс, а затем начните
                    if (timers[name] != null) {
                        cancelTimer(name)
                    }
                    val repeats = options.has("repeats")
                    val interval =
                        (java.lang.Float.valueOf(options.getString("interval")) * 1000).toInt()
                    val timerAction = options.getJSONObject("action")
                    if (repeats) {
                        val runnableCode: Runnable = object : Runnable {
                            override fun run() {
                                Log.d("Handlers", "Called on main thread")
                                val intent = Intent("call")
                                intent.putExtra("action", timerAction.toString())
                                LocalBroadcastManager.getInstance(context!!).sendBroadcast(intent)
                                handler.postDelayed(this, interval.toLong())
                            }
                        }
                        handler.post(runnableCode)

                        // Register timer
                        // Регистрация таймера
                        timers[name] = runnableCode
                    } else {
                        val runnableCode = Runnable {
                            Log.d("Handlers", "Called on main thread")
                            val intent = Intent("call")
                            intent.putExtra("action", timerAction.toString())
                            LocalBroadcastManager.getInstance(context!!).sendBroadcast(intent)
                        }
                        handler.postDelayed(runnableCode, interval.toLong())

                        // Register timer
                        timers[name] = runnableCode
                    }
                }
            }

            // Go on to the next success action
            // Переход к следующему успешному действию
            JasonHelper.next("success", action, data, event, context)
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    }

    fun stop(action: JSONObject, data: JSONObject?, event: JSONObject?, context: Context?) {
        try {
            if (action.has("options")) {
                val options = action.getJSONObject("options")
                if (options.has("name")) {
                    cancelTimer(options.getString("name"))
                } else {
                    cancelTimer(null)
                }
            } else {
                cancelTimer(null)
            }

            // Go on to the next success action
            // Переход к следующему успешному действию
            JasonHelper.next("success", action, data, event, context)
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    }

    private fun cancelTimer(name: String?) {
        if (name != null) {
            val runnableCode = timers[name]
            if (runnableCode != null) {
                handler.removeCallbacks(runnableCode)
                timers.remove(name)
            }
        } else {
            // Cancel all timers
            // Отмена всех таймеров
            for (key in timers.keys) {
                val runnableCode = timers[key]
                handler.removeCallbacks(runnableCode!!)
                timers.remove(name)
            }
        }
    }

    init {
        val thread = HandlerThread("TimerThread")
        thread.start()
        timers = HashMap()
        handler = Handler(thread.looper)
    }
}