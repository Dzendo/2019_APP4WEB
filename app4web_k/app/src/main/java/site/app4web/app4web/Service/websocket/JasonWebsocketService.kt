package site.app4web.app4web.Service.websocket

import android.util.Log
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONObject
import site.app4web.app4web.Core.JasonViewActivity
import site.app4web.app4web.Launcher.Launcher

/*****************************************
 *
 * ### Events:
 * ### События:
 *
 * - There are 4 events: $websocket.onopen, $websocket.onclose, $websocket.onmessage, $websocket.onerror
 * - Есть 4 события: $ websocket.onopen, $ websocket.onclose, $ websocket.onmessage, $ websocket.onerror
 *
 * [1] $websocket.onopen
 * - Triggered when $websocket.open action succeeds.
 * - Срабатывает, когда действие $ websocket.open завершается успешно.
 * - You can start sending messages after this event.
 * - Вы можете начать отправку сообщений после этого события.
 * - Response Payload: none
 * - полезная нагрузка ответа: нет
 *
 * [2] $websocket.onclose
 * - Triggered when $websocket.close action succeeds or the socket closes
 * - срабатывает при успешном действии $ websocket.close или при закрытии сокета
 * - Response Payload: none
 * - полезная нагрузка ответа: нет
 *
 * [3] $websocket.onerror
 * - Triggered when there's an error
 * - срабатывает при ошибке
 * - Response Payload:
 * - полезная нагрузка ответа:
 * {
 * "$jason": {
 * "error": [THE ERROR MESSAGE]
 * }
 * }
 *
 * [4] $websocket.onmessage
 * - Triggered whenever there's an incoming message
 * - срабатывает при поступлении входящего сообщения
 * - Response Payload:
 * - полезная нагрузка ответа:
 * {
 * "$jason": {
 * "message": [THE INCOMING MESSAGE STRING]
 * }
 * }
 *
 */
class JasonWebsocketService(private val launcher: Launcher) {
    private inner class JasonWebSocketListener : WebSocketListener() {
        override fun onOpen(
            webSocket: WebSocket,
            response: Response
        ) {
            val context =
                Launcher.Companion.currentContext as JasonViewActivity
            context.runOnUiThread {
                val context =
                    Launcher.Companion.currentContext as JasonViewActivity
                context.simple_trigger("\$websocket.onopen", JSONObject(), context)
            }
        }

        override fun onMessage(
            webSocket: WebSocket,
            text: String
        ) {
            val context =
                Launcher.Companion.currentContext as JasonViewActivity
            context.runOnUiThread {
                try {
                    val response = JSONObject()
                    val message = JSONObject()
                    message.put("message", text)
                    message.put("type", "string")
                    response.put("\$jason", message)
                    context.simple_trigger("\$websocket.onmessage", response, context)
                } catch (e: Exception) {
                    Log.d(
                        "Warning",
                        e.stackTrace[0].methodName + " : " + e.toString()
                    )
                }
            }
        }

        override fun onMessage(
            webSocket: WebSocket,
            bytes: ByteString
        ) {
            val context =
                Launcher.Companion.currentContext as JasonViewActivity
            context.runOnUiThread {
                try {
                    val response = JSONObject()
                    val message = JSONObject()
                    message.put("message", bytes.hex())
                    message.put("type", "bytes")
                    response.put("\$jason", message)
                    context.simple_trigger("\$websocket.onmessage", response, context)
                } catch (e: Exception) {
                    Log.d(
                        "Warning",
                        e.stackTrace[0].methodName + " : " + e.toString()
                    )
                }
            }
        }

        override fun onClosing(
            webSocket: WebSocket,
            code: Int,
            reason: String
        ) {
            val context =
                Launcher.Companion.currentContext as JasonViewActivity
            context.runOnUiThread {
                val context =
                    Launcher.Companion.currentContext as JasonViewActivity
                context.simple_trigger("\$websocket.onclose", JSONObject(), context)
            }
        }

        override fun onFailure(
            webSocket: WebSocket,
            t: Throwable,
            response: Response?
        ) {
            val context =
                Launcher.Companion.currentContext as JasonViewActivity
            context.runOnUiThread {
                try {
                    val context =
                        Launcher.Companion.currentContext as JasonViewActivity
                    val res = JSONObject()
                    val message = JSONObject()
                    message.put("error", t.message)
                    res.put("\$jason", message)
                    context.simple_trigger("\$websocket.onerror", res, context)
                } catch (e: Exception) {
                    Log.d(
                        "Warning",
                        e.stackTrace[0].methodName + " : " + e.toString()
                    )
                }
            }
        }

      //  companion object {
      //      private const
      val NORMAL_CLOSURE_STATUS = 1000
       // }
    }

    private var listener: JasonWebSocketListener? = null
    private var ws: WebSocket? = null
    private val thread: Thread? = null
    fun open(action: JSONObject) {
        try {
            val options = action.getJSONObject("options")
            val url = options.getString("url")
            val client = launcher.getHttpClient(0)
            val request = Request.Builder().url(url).build()
            listener = JasonWebSocketListener()
            ws = client!!.newWebSocket(request, listener!!)
            client.dispatcher.executorService.shutdown()
        } catch (e: Exception) {
            Log.d(
                "Warning",
                e.stackTrace[0].methodName + " : " + e.toString()
            )
        }
    }

    fun close() {
        ws!!.close(1000, "Goodbye!")
    }

    fun send(action: JSONObject) {
        try {
            val options = action.getJSONObject("options")
            val text = options.getString("message")
            ws!!.send(text)
        } catch (e: Exception) {
            Log.d(
                "Warning",
                e.stackTrace[0].methodName + " : " + e.toString()
            )
        }
    }

}