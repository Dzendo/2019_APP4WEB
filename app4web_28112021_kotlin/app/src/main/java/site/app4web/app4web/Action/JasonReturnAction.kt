package site.app4web.app4web.Action

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import org.json.JSONObject
import site.app4web.app4web.Core.JasonParser
import site.app4web.app4web.Core.JasonParser.JasonParserListener
import site.app4web.app4web.Helper.JasonHelper


class JasonReturnAction {
    /*

    -------------------------------------------

    Throughout every action call chain, the original caller action is passed down inside the 'event' object.

    1. When we reach the end via `$return.success`, we look into  what event.success contains.
    Then we execute $lambda on it.

    2. When we reach the end via `$return.success`, we look into  what event.success contains.
    Then we execute $lambda on it.

    Во всех цепочках вызовов действий исходное действие вызывающей стороны передается внутри объекта «событие».

     1. Когда мы достигаем конца через `$return.success`, мы смотрим, что содержит event.success.
     Затем мы выполняем $lambda на нем.

     2. Когда мы достигаем конца через `$return.success`, мы смотрим, что содержит event.success.
     Затем мы выполняем $lambda на нем.



    -------------------------------------------

    {
        "$jason": {
            "head": {
                "actions": {
                    "$load": {
                        "type": "$trigger",
                        "options": {
                            "name": "sync"
                        },
                        "success": {
                            "type": "$trigger",
                                "options": {
                                "name": "process"
                            },
                            "success": {
                                "type": "$render"
                            }
                        },
                        "error": {
                            "trigger": "err"
                        }
                    },
                    "sync": {
                        "type": "$network.request",
                        "options": {
                            "url": "https://www.jasonbase.com/things/4nf.json"
                        },
                        "success": {
                            "type": "$return.success"
                        }
                    },
                    "err": {
                        "type": "$util.banner",
                        "options": {
                            "title": "error",
                            "description": "Something went wrong."
                        }
                    }
                },
                "templates": {
                    ...
                }
            }
        }
    }


    whenever triggering something,
    attach the original action as event
    when reaching $return.success or $return.error, just replace it with $event.success
    всякий раз, когда вызывая что-то,
    прикрепить исходное действие как событие
    при достижении $return.success или $return.error просто замените его на $event.success
    */
    private fun next(
        type: String,
        action: JSONObject,
        data: JSONObject,
        event: JSONObject,
        context: Context
    ) {
        if (event.has(type)) {
            try {
                val options: JSONObject
                options = if (action.has("options")) {
                    action.getJSONObject("options")
                } else {
                    JSONObject()
                }
                JasonParser.getInstance(context)!!.setParserListener(object : JasonParserListener {
                    override fun onFinished(parsed_options: JSONObject?) {
                        try {
                            val intent = Intent("call")
                            intent.putExtra("action", event.getJSONObject(type).toString())
                            intent.putExtra("data", parsed_options.toString())
                            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
                        } catch (e: java.lang.Exception) {
                            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
                        }
                    }
                })
                JasonParser.Companion.getInstance(context)!!.parse("json", data, options, context)
            } catch (e: Exception) {
                Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
            }
        } else {
            JasonHelper.next(type, JSONObject(), data, event, context)
        }
    }

    fun success(action: JSONObject, data: JSONObject, event: JSONObject, context: Context) {
        next("success", action, data, event, context)
    }

    fun error(action: JSONObject, data: JSONObject, event: JSONObject, context: Context) {
        next("error", action, data, event, context)
    }
}