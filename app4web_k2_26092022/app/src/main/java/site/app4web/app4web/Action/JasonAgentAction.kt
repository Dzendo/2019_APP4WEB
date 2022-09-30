package site.app4web.app4web.Action

import android.content.Context
import android.util.Log
import org.json.JSONObject
import site.app4web.app4web.Launcher.Launcher
import site.app4web.app4web.Service.agent.JasonAgentService

class JasonAgentAction {
    fun request(action: JSONObject?, data: JSONObject?, event: JSONObject?, context: Context) {
        try {
            if (action != null) {
                (context.applicationContext as Launcher).call(
                    "JasonAgentService",
                    "jason_request",
                    action,
                    context
                )
            }
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    }

    fun refresh(action: JSONObject?, data: JSONObject?, event: JSONObject?, context: Context) {
        try {
            val agentService =
                (context.applicationContext as Launcher).services?.get("JasonAgentService") as JasonAgentService
            if (action != null) {
                agentService.refresh(action, context)
            }
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    }

    fun clear(action: JSONObject?, data: JSONObject?, event: JSONObject?, context: Context) {
        try {
            val agentService =
                (context.applicationContext as Launcher).services?.get("JasonAgentService") as JasonAgentService
            if (action != null) {
                agentService.clear(action, context)
            }
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    }

    fun inject(action: JSONObject?, data: JSONObject?, event: JSONObject?, context: Context) {
        try {
            val agentService =
                (context.applicationContext as Launcher).services?.get("JasonAgentService") as JasonAgentService
            if (action != null) {
                agentService.inject(action, context)
            }
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    }
}