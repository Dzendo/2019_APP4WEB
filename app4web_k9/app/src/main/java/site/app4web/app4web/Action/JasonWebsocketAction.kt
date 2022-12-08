package site.app4web.app4web.Action

import android.content.Context
import org.json.JSONObject
import site.app4web.app4web.Helper.JasonHelper
import site.app4web.app4web.Launcher.Launcher

/*****************************************
 *
 * ### Actions:
 * - There are 3 actions: Open, Close, Send
 * - All actions are asynchronous => They don't wait for a response and immediately calls "success"
 * - Instead of a return value, all of these actions trigger a service.
 * - The corresponding service (can be seen at JasonWebsocketService) emits an event when there's a result.
 * ### Действия:
 *       - Есть 3 действия: Открыть, Закрыть, Отправить
 *       - Все действия асинхронны => Они не ждут ответа и сразу же называют «успех»
 *       - Вместо возвращаемого значения все эти действия запускают службу.
 *       - Соответствующий сервис (можно увидеть на JasonWebsocketService) генерирует событие, когда есть результат.
 *
 * [1] Open
 * {
 * "type": "$websocket.open",
 * "options": {
 * "url": "..."
 * },
 * "success": { ... }
 * }
 *
 * [2] Close
 * {
 * "type": "$websocket.close",
 * "success": { ... }
 * }
 *
 * [3] Send
 * {
 * "type": "$websocket.send",
 * "options": {
 * "message": "..."
 * },
 * "success": { ... }
 * }
 *
 */
class JasonWebsocketAction {
    fun open(
        action: JSONObject?,
        data: JSONObject?,
        event: JSONObject?,
        context: Context
    ) {
        (context.applicationContext as Launcher).call(
            "JasonWebsocketService",
            "open",
            action!!,
            context
        )
        JasonHelper.next("success", action, JSONObject(), event, context)
    }

    fun close(
        action: JSONObject?,
        data: JSONObject?,
        event: JSONObject?,
        context: Context
    ) {
        (context.applicationContext as Launcher).call(
            "JasonWebsocketService",
            "close",
            action!!,
            context
        )
        JasonHelper.next("success", action, JSONObject(), event, context)
    }

    fun send(
        action: JSONObject?,
        data: JSONObject?,
        event: JSONObject?,
        context: Context
    ) {
        (context.applicationContext as Launcher).call(
            "JasonWebsocketService",
            "send",
            action!!,
            context
        )
        JasonHelper.next("success", action, JSONObject(), event, context)
    }
}