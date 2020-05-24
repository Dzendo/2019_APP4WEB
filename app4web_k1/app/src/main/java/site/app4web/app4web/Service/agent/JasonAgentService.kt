package site.app4web.app4web.Service.agent

import android.R
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Build
import android.util.Log
import android.view.View
import android.webkit.*
import android.widget.ProgressBar
import android.widget.RelativeLayout
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.notify
import okhttp3.internal.wait
import org.json.JSONArray
import org.json.JSONObject
import site.app4web.app4web.Core.JasonParser
import site.app4web.app4web.Core.JasonParser.JasonParserListener
import site.app4web.app4web.Core.JasonViewActivity
import site.app4web.app4web.Helper.JasonHelper
import site.app4web.app4web.Launcher.Launcher
import java.io.IOException
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

class JasonAgentService  // Initialize
// Инициализация
{
    private val pending = JSONObject()
    private val pending_injections = JSONObject()

    private inner class JasonAgentInterface(
        private val agent: WebView,
        private val context: Context
    ) {

        // $agent to Jasonette interface
        // $ агент к интерфейсу Jasonette
        @JavascriptInterface
        fun postMessage(json: String?) {
            /****************************************
             *
             * A message can be:
             * Сообщение может быть:
             *
             * 1. Request
             * 1. Запрос
             *
             * {
             * "request": {
             * "data": [RPC Object],
             * "nonce": [Auto-generated nonce to handle return values later]
             * }
             * }
             *
             * 2. Response
             * 2. Ответ
             *
             * {
             * "response": {
             * "data": [Return Value]
             * }
             * }
             *
             * 3. Trigger
             * 3. Триггер
             *
             * {
             * "trigger": {
             * "name": [Jasonette Event Name],
             * "data": [The "$jason" value to pass along with the event]
             * }
             * }
             *
             * 4. Href
             * 4. Hyper REFerence это основной и необходимый атрибут тега A,
             * указывающий браузеру, какое действие надо выполнить при клике на ссылку.
             *
             * {
             * "href": {
             * "data": [Jasonette HREF object]
             * }
             * }
             *
             */
            try {
                val message = JSONObject(json!!)
                val transaction = agent.tag as JSONObject
                if (message.has("request")) {
                    /***
                     * 1. Request: Agent making request to another agent
                     * 1. Запрос: агент делает запрос другому агенту
                     *
                     * {
                     * "request": {
                     * "data": [RPC Object],
                     * "nonce": [Auto-generated nonce to handle return values later]
                     * }
                     * }
                     */
                    if (transaction.has("to")) {
                        val content = message.getJSONObject("request")

                        /**
                         * Compose an agent_request object
                         * Составьте объект agent_request
                         *
                         * agent_request := {
                         * "from": [SOURCE AGENT ID],
                         * "request": [JSON-RPC Object],
                         * "nonce": [NONCE]
                         * }
                         *
                         */
                        val agent_request = JSONObject()
                        agent_request.put("from", transaction.getString("to"))
                        agent_request.put("request", content.getJSONObject("data"))
                        agent_request.put("nonce", content.getString("nonce"))
                        request(null, agent_request, context)
                    }
                } else if (message.has("response")) {
                    /*****************
                     *
                     * 2. Response
                     * 2. Ответ
                     *
                     * {
                     * "response": {
                     * "data": [Return Value]
                     * }
                     * }
                     *
                     */
                    if (transaction.has("to")) {
                        val res = message.getJSONObject("response")

                        // Params
                        val param = res["data"]


                        // "from": exists => the caller request was from another agent
                        if (transaction.has("from")) {
                            /**
                             * Compose an agent_request object
                             * Составьте объект agent_request
                             *
                             * agent_request := {
                             * "from": [SOURCE AGENT ID],
                             * "request": [JSON-RPC Object],
                             * "nonce": [NONCE]
                             * }
                             *
                             */
                            val params = JSONArray()
                            params.put(param)

                            // from
                            val from = transaction.getString("from")
                            if (transaction.has("nonce")) {

                                // 1. Construct jsonrpc
                                // 1. Построить jsonrpc
                                val jsonrpc = JSONObject()

                                // 1.1. Method: Call the callback method stored under $agent.callbacks object
                                // 1.1. Метод: вызов метода обратного вызова, хранящегося в объекте $ agent.callbacks
                                val method =
                                    "\$agent.callbacks[\"" + transaction.getString("nonce") + "\"]"
                                jsonrpc.put("method", method)

                                // 1.2. id: need to call the callback method on the "from" agent
                                // 1.2. id: необходимо вызвать метод обратного вызова на агенте "from"
                                jsonrpc.put("id", from)

                                // 1.3. params array
                                // 1.3. массив параметров
                                jsonrpc.put("params", params)

                                // 2. Construct the callback agent_request
                                // 2. Построить обратный вызов agent_request
                                val agent_request = JSONObject()
                                // 2.1. "from" and "nonce" should be set to null, since this is a return value, and there is no coming back again.
                                // 2.1. "from" и "nonce" должны быть установлены в null, так как это возвращаемое значение, и возвращаться больше нельзя.
                                agent_request.put("from", null)
                                agent_request.put("nonce", null)
                                // 2.2. set JSON RPC request
                                // 2.2. установить запрос JSON RPC
                                agent_request.put("request", jsonrpc)
                                request(null, agent_request, context)
                            }

                            // "from" doesn't exist => call from Jasonette => return via success callback
                            // "из" не существует => вызов из Jasonette => возврат через успешный обратный вызов
                        } else {
                            if (transaction.has("jason")) {
                                val original_action = transaction.getJSONObject("jason")
                                // return param as a return value
                                JasonHelper.next(
                                    "success",
                                    original_action,
                                    param,
                                    JSONObject(),
                                    context
                                )
                            }
                        }
                    }
                } else if (message.has("trigger")) {
                    /************************
                     *
                     * 3. Trigger
                     * 3. Триггер // спусковой крючок
                     *
                     * {
                     * "trigger": {
                     * "name": [Jasonette Event Name],
                     * "data": [The "$jason" value to pass along with the event]
                     * }
                     * }
                     *
                     */

                    // If the parent is not the same, don't trigger
                    // Если родитель не тот же, не запускать
                    val current_url = (context as JasonViewActivity).model!!.url
                    if (transaction.has("parent") && !transaction.getString("parent")
                            .equals(current_url, ignoreCase = true)
                    ) {
                        return
                    }
                    val trigger = message.getJSONObject("trigger")
                    val m = JSONObject()
                    if (trigger.has("name")) {
                        // trigger
                        // спусковой крючок
                        m.put("trigger", trigger.getString("name"))
                        // options
                        // опции
                        if (trigger.has("data")) {
                            m.put("options", trigger["data"])
                        }

                        // Call the Jasonette event
                        // Вызов события Jasonette
                        val intent = Intent("call")
                        intent.putExtra("action", m.toString())
                        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
                    }
                } else if (message.has("href")) {
                    /************************************
                     *
                     * 4. Href
                     *
                     * {
                     * "href": {
                     * "data": [Jasonette HREF object]
                     * }
                     * }
                     *
                     */
                    if (message.getJSONObject("href").has("data")) {
                        val m = message.getJSONObject("href").getJSONObject("data")
                        val intent = Intent("call")
                        val href = JSONObject()
                        href.put("type", "\$href")
                        href.put("options", m)
                        intent.putExtra("action", href.toString())
                        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
                    }
                }
            } catch (e: Exception) {
                Log.d(
                    "Warning",
                    e.stackTrace[0].methodName + " : " + e.toString()
                )
            }
        }

    }

    fun setup(context: JasonViewActivity, options: JSONObject, id: String): WebView {
        /**
         *
         * 1. Initialize WebView
         * - Does an agent with the ID exist already?
         * YES => Use that one
         * NO =>
         * 1. Create a new WebView
         * 2. Attach to the parent view's agents object
         *
         * 2. Creating a WebView
         * - Hide it
         * - Set metadata payload on it so it can be referenced later
         *
         * 3. Filling a WebView
         * - Is the state "empty"?
         * YES => Load
         * NO => Ignore
         *
         * 1. Инициализируйте WebView
         *           - Агент с идентификатором уже существует?
         *           ДА => Используйте это
         *           НЕТ =>
         *           1. Создайте новый WebView
         *           2. Присоедините к объекту агентов родительского представления
         *
         *           2. Создание WebView
         *           - Скрыть
         *           - Установите полезную нагрузку метаданных, чтобы на нее можно было ссылаться позже.
         *
         *           3. Заполнение WebView
         *           - Является ли государство «пустым»?
         *           ДА => Загрузить
         *           НЕТ => Игнорировать
         *
         */
        var agent: WebView
        /*******************************************
         * 1. Initialize WebView
         *
         * - Does an agent with the ID exist already?
         * YES => Use that one
         * NO =>
         * 1. Create a new WebView
         * 2. Attach to the parent view's agents object
         *
         * 1. Инициализируйте WebView
         *
         *           - Агент с идентификатором уже существует?
         *           ДА => Используйте это
         *           НЕТ =>
         *           1. Создайте новый WebView
         *           2. Присоедините к объекту агентов родительского представления
         *
         */
        try {
            // An agent with the ID already exists
            // Агент с идентификатором уже существует
            if (context.agents.has(id)) {
                agent = context.agents[id] as WebView

                // No such agent exists yet. Create one.
                // Такой агент еще не существует. Создай.
            } else {
                /*******************************************
                 * 2. Creating a WebView
                 * 2. Создание WebView
                 */

                // 2.1. Initialize
                // 2.1. инициализировать
                CookieManager.getInstance().setAcceptCookie(true)
                agent = WebView(context)
                agent.settings.defaultTextEncodingName = "utf-8"
                if (id.startsWith("\$webcontainer")) {
                    val pBar: ProgressBar
                    if (agent.findViewById<View?>(42) == null) {
                        pBar = ProgressBar(context, null, R.attr.progressBarStyleHorizontal)
                        val layoutParams = RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.MATCH_PARENT,
                            RelativeLayout.LayoutParams.WRAP_CONTENT
                        )
                        layoutParams.addRule(RelativeLayout.ALIGN_BOTTOM, agent.id)
                        layoutParams.height = 5
                        pBar.scaleY = 4f
                        pBar.layoutParams = layoutParams
                        val color: Int
                        color = if (options.has("style") && options.getJSONObject("style")
                                .has("progress")
                        ) {
                            JasonHelper.parse_color(
                                options.getJSONObject("style").getString("progress")
                            )
                        } else {
                            JasonHelper.parse_color("#007AFF")
                        }
                        pBar.progressDrawable
                            .setColorFilter(color, PorterDuff.Mode.SRC_IN)
                        pBar.id = 42
                    } else {
                        pBar = agent.findViewById(42)
                    }
                    agent.addView(pBar)
                    agent.setWebChromeClient(object : WebChromeClient() {
                        override fun onProgressChanged(
                            view: WebView?,
                            progress: Int
                        ) {
                            pBar.progress = progress
                            if (progress == 100) {
                                pBar.visibility = View.GONE
                            } else {
                                pBar.visibility = View.VISIBLE
                            }
                        }
                    })
                } else {
                    agent.setWebChromeClient(WebChromeClient())
                }
                agent.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        // Inject agent.js
                        // Внедрить agent.js
                        try {
                            val injection_script = JasonHelper.read_file("agent", context)
                            val interface_script =
                                "\$agent.interface.postMessage = function(r) { JASON.postMessage(JSON.stringify(r)); };"
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                view.evaluateJavascript(
                                    "$injection_script $interface_script",
                                    null
                                )
                            } else {
                                view.loadUrl("javascript:$injection_script $interface_script")
                            }
                            if (pending.has(id)) {
                                val q = pending.getJSONObject(id)
                                val jason_request: JSONObject?
                                jason_request = if (q.has("jason_request")) {
                                    q.getJSONObject("jason_request")
                                } else {
                                    null
                                }
                                val agent_request: JSONObject?
                                agent_request = if (q.has("agent_request")) {
                                    q.getJSONObject("agent_request")
                                } else {
                                    null
                                }
                                request(jason_request, agent_request, context)
                                pending.remove(id)
                            }
                            if (pending_injections.has(id)) {
                                inject(pending_injections.getJSONObject(id), context)
                                pending_injections.remove(id)
                            }

                            // only set state to rendered if it's not about:blank
                            // устанавливаем состояние только для визуализации, если это не так: blank
                            if (!url.equals("about:blank", ignoreCase = true)) {
                                val payload = view.tag as JSONObject
                                payload.put("state", "rendered")
                                view.tag = payload
                            }
                        } catch (e: Exception) {
                            Log.d(
                                "Warning",
                                e.stackTrace[0].methodName + " : " + e.toString()
                            )
                        }
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView,
                        url: String
                    ): Boolean {
                        // resolve
                        // разрешить
                        var url = url
                        val notifier = AtomicReference<JSONObject?>()
                        try {
                            val payload = view.tag as JSONObject

                            // Only override behavior for web container
                            // только переопределить поведение для веб-контейнера
                            if (!id.startsWith("\$webcontainer")) {
                                return false
                            }
                            if (!payload.has("url")) {
                                return false
                            }
                            if (view.hitTestResult.type > 0) {
                                if (options.has("action")) {
                                    // 1. Parse the action if it's a trigger
                                    val resolved_action: Any
                                    resolved_action =
                                        if (options.getJSONObject("action").has("trigger")) {
                                            val event_name =
                                                options.getJSONObject("action").getString("trigger")
                                            val head =
                                                context.model!!.jason!!.getJSONObject("\$jason")
                                                    .getJSONObject("head")
                                            val events = head.getJSONObject("actions")
                                            // Look up an action by event_name
                                            events[event_name]
                                        } else {
                                            options["action"]
                                        }


                                    /* set $jason */
                                    /* установить $ Джейсон */
                                    val u = JSONObject()
                                    u.put("url", url)
                                    val ev = JSONObject()
                                    ev.put("\$jason", u)
                                    context.model!!["state"] = ev
                                    JasonParser.Companion.getInstance(context)
                                        ?.setParserListener(object : JasonParserListener {
                                            override fun onFinished(reduced_action: JSONObject?) {
                                                synchronized(notifier) {
                                                    notifier.set(reduced_action)
                                                    notifier.notify()
                                                }
                                            }
                                        })
                                    JasonParser.Companion.getInstance(context)!!.parse(
                                        "json",
                                        context.model!!.state,
                                        resolved_action,
                                        context
                                    )
                                    synchronized(notifier) {
                                        while (notifier.get() == null) {
                                            notifier.wait()
                                        }
                                    }
                                    val parsed = notifier.get()
                                    return if (parsed!!.has("type") && parsed.getString("type")
                                            .equals("\$default", ignoreCase = true)
                                    ) {
                                        false
                                    } else {
                                        val intent = Intent("call")
                                        intent.putExtra("action", options["action"].toString())

                                        // file url handling
                                        // обработка URL файла
                                        if (url.startsWith("file://")) {
                                            if (url.startsWith("file:///android_asset/file")) {
                                                url = url.replace("/android_asset/file/", "")
                                            }
                                        }
                                        intent.putExtra("data", "{\"url\": \"$url\"}")
                                        LocalBroadcastManager.getInstance(context)
                                            .sendBroadcast(intent)
                                        true
                                    }
                                }
                            }
                            payload.put("state", "loading")
                            view.tag = payload
                        } catch (e: Exception) {
                            Log.d(
                                "Warning",
                                e.stackTrace[0].methodName + " : " + e.toString()
                            )
                            notifier.notify()
                        }
                        return false
                    }
                }
                agent.isVerticalScrollBarEnabled = false
                agent.isHorizontalScrollBarEnabled = false
                if (options.has("style") && options.getJSONObject("style").has("background")) {
                    agent.setBackgroundColor(
                        JasonHelper.parse_color(
                            options.getJSONObject("style").getString("background")
                        )
                    )
                } else {
                    agent.setBackgroundColor(Color.TRANSPARENT)
                }
                val settings = agent.settings
                settings.layoutAlgorithm = WebSettings.LayoutAlgorithm.SINGLE_COLUMN
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.javaScriptCanOpenWindowsAutomatically = true
                settings.setAppCachePath(context.cacheDir.absolutePath)
                settings.allowFileAccess = true
                settings.setAppCacheEnabled(true)
                settings.cacheMode = WebSettings.LOAD_DEFAULT

                // 2.2. Create and Attach JavaScript Interface
                // 2.2. Создать и присоединить интерфейс JavaScript
                val agentInterface = JasonAgentInterface(agent, context)
                agent.addJavascriptInterface(agentInterface, "JASON")

                // 2.3. Hide it
                // 2.3. Скрыть
                agent.visibility = View.GONE

                // 2.3. Set Payload
                // 2.3. Установить полезную нагрузку
                val payload = JSONObject()
                // 2.3.1. copy options
                // 2.3.1. варианты копирования
                val iterator = options.keys()
                while (iterator.hasNext()) {
                    val key = iterator.next()
                    val value = options[key]
                    payload.put(key, value)
                }
                // 2.3.2. Add 'id' and 'state'
                // 2.3.2. Добавьте «id» и «state»
                payload.put("to", id)
                payload.put("state", "empty")
                payload.put("parent", context.model!!.url)
                // 2.3.3. Set
                // 2.3.3. Задавать
                agent.tag = payload

                // 2.4. Attach to the parent view
                // 2.4. Прикрепить к родительскому представлению
                context.agents.put(id, agent)
            }
            /*******************************************
             *
             * 3. Filling the WebView with content
             * - Is the state "empty"?
             * YES => Load
             * NO => Ignore
             *
             * 3. Наполнение WebView содержимым
             *               - Является ли государство «пустым»?
             *               ДА => Загрузить
             *               НЕТ => Игнорировать
             */
            val payload = agent.tag as JSONObject

            // Fill in the content if empty
            // Заполнить содержимое, если оно пустое
            if (payload.has("state") && payload.getString("state")
                    .equals("empty", ignoreCase = true)
            ) {
                if (options.has("text")) {
                    val html = options.getString("text")
                    agent.loadDataWithBaseURL("http://localhost/", html, "text/html", "utf-8", null)
                } else if (options.has("url")) {
                    val url = options.getString("url")
                    // 1. file url
                    // 1. файл url
                    if (url.startsWith("file://")) {
                        agent.loadUrl("file:///android_asset/file/" + url.substring(7))
                        // 2. remote url
                        // 2. удаленный URL
                    } else {
                        agent.loadUrl(url)
                    }
                }
            }
        } catch (e: Exception) {
            agent = WebView(context)
            Log.d(
                "Warning",
                e.stackTrace[0].methodName + " : " + e.toString()
            )
        }
        return agent
    }

    fun jason_request(jason_request: JSONObject?, context: Context) {
        request(jason_request, null, context)
    }

    fun request(
        jason_request: JSONObject?,
        agent_request: JSONObject?,
        context: Context
    ) {
        try {
            /*****************************
             *
             * 1. jason_request is an entire Jasonette action which contains a JSON-RPC object as its options
             *
             * jason_request := {
             * "type": "$agent.request",
             * "options": [JSON-RPC Object],
             * "success": [Next Jasonette Action]
             * }
             *
             * 2. agent_request is a JSON-RPC object
             *
             * agent_request := {
             * "from": [SOURCE AGENT ID],
             * "request": [JSON-RPC Object],
             * "nonce": [NONCE]
             * }
             *
             * 1. jason_request - это целое действие Jasonette, которое содержит объект JSON-RPC в качестве параметров
             *
             *               jason_request: = {
             *               "type": "$ agent.request",
             *               "options": [JSON-RPC Object],
             *               «успех»: [Следующее действие Jasonette]
             *               }
             *
             * 2. agent_request является объектом JSON-RPC
             *
             *               agent_request: = {
             *               "от": [ИДЕНТИФИКАТОР ИСТОЧНИКА]
             *               "запрос": [объект JSON-RPC],
             *               "nonce": [NONCE]
             *               }
             *
             */
            /***
             *
             * 0. Check if it's jason_request or agent_request
             * - If jason_request => Use this to proceed
             * - If agent_request => Use this to proceed
             *
             * 1. Get JSON-RPC arguments
             *
             * - id
             * - method
             * - params
             *
             * 2. Find the agent by ID
             * - iF agent exists, go on.
             *
             * 3. Set the $source on the agent's payload (tag)
             *
             * 4. Create a JavaScript call string
             *
             * 5. Run the call script
             *
             * 0. Проверьте, является ли это jason_request или agent_request
             *               - Если jason_request => Используйте это, чтобы продолжить
             *               - Если agent_request => Используйте это, чтобы продолжить
             *
             * 1. Получить аргументы JSON-RPC
             *
             *               - Я бы
             *               - метод
             *               - параметры
             *
             * 2. Найти агента по ID
             *               - Если агент существует, продолжайте.
             *
             * 3. Установите $ source на полезную нагрузку агента (тег)
             *
             * 4. Создайте строку вызова JavaScript
             *
             *              5. Запустите скрипт вызова
             *
             */

            // Get JSON RPC object
            // Получить объект JSON RPC
            val jsonrpc: JSONObject
            jsonrpc = if (jason_request != null) {
                /**
                 * 1. jason_request is an entire Jasonette action which contains a JSON-RPC object as its options
                 * 1. jason_request - это целое действие Jasonette, которое содержит объект JSON-RPC в качестве параметров
                 *
                 * jason_request := {
                 * "type": "$agent.request",
                 * "options": [JSON-RPC Object],
                 * "success": [Next Jasonette Action]
                 * }
                 *
                 */
                jason_request.getJSONObject("options")
            } else {
                /**
                 * 2. agent_request is a JSON-RPC object
                 * 2. agent_request является объектом JSON-RPC
                 *
                 * agent_request := {
                 * "from": [SOURCE AGENT ID],
                 * "request": [JSON-RPC Object],
                 * "nonce": [NONCE]
                 * }
                 *
                 */
                agent_request!!.getJSONObject("request")
            }
            if (jsonrpc.has("id")) {
                var identifier = jsonrpc.getString("id")

                // Special case handling for $webcontainers (For sandboxing per view)
                // Обработка особого случая для $ webcontainers (для песочницы для представления)
                if (identifier.equals("\$webcontainer", ignoreCase = true)) {
                    identifier = "\$webcontainer@" + (context as JasonViewActivity).model!!.url
                }
                if ((context as JasonViewActivity).agents.has(identifier)) {
                    // Find agent by ID
                    // Найти агента по ID
                    val agent =
                        context.agents[identifier] as WebView

                    /**
                     *
                     * A transaction looks like this:
                     * Транзакция выглядит так:
                     *
                     * {
                     * "state": "empty",
                     * "to": [Agent ID],
                     * "from": [Desitnation Agent ID],
                     * "nonce": [NONCE],
                     * "jason": [The original Jason action that triggered everything],
                     * "request": [JSON RPC object]
                     * }
                     *
                     */


                    // Set Transaction payload
                    // Установить полезную нагрузку транзакции

                    // 1. Initialize
                    // 1. Инициализация
                    val transaction: JSONObject
                    transaction = if (agent.tag != null) {
                        agent.tag as JSONObject
                    } else {
                        JSONObject()
                    }

                    // 2. Fill in
                    // 2. Заполнить
                    if (jason_request != null) {
                        // 2.1. From: Set it as the caller Jasonette action
                        // 2.1. От: Установите это как вызывающее действие Jasonette
                        transaction.put("from", null)
                        // 2.2. Nonce
                        // 2.2. данное время
                        transaction.put("nonce", null)
                        // 2.3. Original JASON Caller Action
                        // 2.3. Оригинальный JASON Caller Action
                        transaction.put("jason", jason_request)
                    } else {
                        // 2.1. From: Set it as the caller agent ID
                        // 2.1. От: Установите его как идентификатор вызывающего агента
                        if (agent_request!!.has("from")) {
                            transaction.put("from", agent_request.getString("from"))
                        }
                        // 2.2. Nonce
                        // 2.2. данное время
                        if (agent_request.has("nonce")) {
                            transaction.put("nonce", agent_request.getString("nonce"))
                        }
                    }
                    agent.tag = transaction

                    // Create a JS call string
                    // Создать строку вызова JS
                    var params = "null"
                    if (jsonrpc.has("method")) {
                        val method = jsonrpc.getString("method")
                        if (jsonrpc.has("params")) {
                            params = jsonrpc.getJSONArray("params").toString()
                        }
                        val callstring = "$method.apply(this, $params);"
                        context.runOnUiThread {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                agent.evaluateJavascript(callstring, null)
                            } else {
                                agent.loadUrl("javascript:$callstring")
                            }
                        }
                        return
                    }
                } else {
                    // If the agent is not yet ready, put it in a pending queue,
                    // this will be triggered later when the webview becomes ready
                    // Если агент еще не готов, поместите его в очередь ожидания,
                    // это будет запущено позже, когда веб-представление будет готово
                    val q = JSONObject()
                    q.put("jason_request", jason_request)
                    q.put("agent_request", agent_request)
                    pending.put(identifier, q)
                }
            }
        } catch (e: Exception) {
            Log.d(
                "Warning",
                e.stackTrace[0].methodName + " : " + e.toString()
            )
        }
    }

    fun refresh(action: JSONObject, context: Context) {
        // Get JSON RPC object
        // Получить объект JSON RPC
        /**
         *
         * action := {
         * "type": "$agent.refresh",
         * "options": {
         * "id": [AGENT ID]
         * },
         * "success": [Next Jasonette Action]
         * }
         *
         */
        try {
            val jsonrpc = action.getJSONObject("options")
            if (jsonrpc.has("id")) {
                var identifier = jsonrpc.getString("id")
                if (identifier.equals("\$webcontainer", ignoreCase = true)) {
                    identifier = "\$webcontainer@" + (context as JasonViewActivity).model!!.url
                }
                if ((context as JasonViewActivity).agents.has(identifier)) {
                    val agent =
                        context.agents[identifier] as WebView
                    // Find agent by ID
                    context.runOnUiThread { agent.loadUrl(agent.url!!) }
                    JasonHelper.next("success", action, JSONObject(), JSONObject(), context)
                    return
                }
            }
        } catch (e: Exception) {
            Log.d(
                "Warning",
                e.stackTrace[0].methodName + " : " + e.toString()
            )
        }
        JasonHelper.next("error", action, JSONObject(), JSONObject(), context)
    }

    fun clear(action: JSONObject, context: Context) {
        // Get JSON RPC object
        // Получить объект JSON RPC
        /**
         *
         * action := {
         * "type": "$agent.clear",
         * "options": {
         * "id": [AGENT ID]
         * },
         * "success": [Next Jasonette Action]
         * }
         *
         */
        try {
            val jsonrpc = action.getJSONObject("options")
            if (jsonrpc.has("id")) {
                var identifier = jsonrpc.getString("id")
                if (identifier.equals("\$webcontainer", ignoreCase = true)) {
                    identifier = "\$webcontainer@" + (context as JasonViewActivity).model!!.url
                }
                if ((context as JasonViewActivity).agents.has(identifier)) {
                    // Find agent by ID
                    val agent =
                        context.agents[identifier] as WebView
                    context.runOnUiThread {
                        try {
                            val newTag = agent.tag as JSONObject
                            newTag.put("state", "empty")
                            agent.tag = newTag
                        } catch (e: Exception) {
                            Log.d(
                                "Warning",
                                e.stackTrace[0].methodName + " : " + e.toString()
                            )
                        }
                        agent.loadUrl("about:blank")
                    }
                    JasonHelper.next("success", action, JSONObject(), JSONObject(), context)
                    return
                }
            }
        } catch (e: Exception) {
            Log.d(
                "Warning",
                e.stackTrace[0].methodName + " : " + e.toString()
            )
        }
        JasonHelper.next("error", action, JSONObject(), JSONObject(), context)
    }

    /*************************************
     *
     * $agent.inject: Inject JavaScript into $agent context
     * $ agent.inject: добавить JavaScript в контекст $ agent
     *
     * {
     * "type": "$agent.inject",
     * "options": {
     * "id": "app",
     * "items": [{
     * "url": "file://authentication.js"
     * }]
     * },
     * "success": {
     * "type": "$agent.request",
     * "options": {
     * "id": "app",
     * "method": "login",
     * "params": ["eth", "12341234"]
     * }
     * }
     * }
     *
     */
    fun inject(action: JSONObject, context: Context) {
        // id
        // items
        try {
            val options = action.getJSONObject("options")
            if (options.has("id")) {
                var identifier = options.getString("id")
                if (identifier.equals("\$webcontainer", ignoreCase = true)) {
                    identifier = "\$webcontainer@" + (context as JasonViewActivity).model!!.url
                }
                if ((context as JasonViewActivity).agents.has(identifier)) {
                    val agent =
                        context.agents[identifier] as WebView
                    if (options.has("items")) {
                        val items = options.getJSONArray("items")
                        val latch = CountDownLatch(items.length())
                        val taskExecutor =
                            Executors.newFixedThreadPool(items.length())
                        val codes = ArrayList<String?>()
                        for (i in 0 until items.length()) {
                            codes.add("")
                        }
                        val errors = ArrayList<String>()
                        for (i in 0 until items.length()) {
                            val item = items.getJSONObject(i)
                            taskExecutor.submit(Fetcher(latch, item, i, codes, errors, context))
                        }
                        try {
                            latch.await()
                        } catch (e: Exception) {
                            Log.d(
                                "Warning",
                                e.stackTrace[0].methodName + " : " + e.toString()
                            )
                        }

                        // All finished. Now can utilize codes
                        // Все закончено. Теперь можно использовать коды
                        var code_string = ""
                        for (s in codes) {
                            code_string = """
                                $code_string$s

                                """.trimIndent()
                        }
                        val codestr = code_string
                        context.runOnUiThread {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                agent.evaluateJavascript(codestr, null)
                            } else {
                                agent.loadUrl("javascript:$codestr")
                            }
                            JasonHelper.next(
                                "success",
                                action,
                                JSONObject(),
                                JSONObject(),
                                context
                            )
                        }
                    } else {
                        val error = JSONObject()
                        error.put("message", "need to specify items")
                        JasonHelper.next("error", action, error, JSONObject(), context)
                    }
                } else {
                    pending_injections.put(identifier, action)
                }
            }
        } catch (e: Exception) {
            Log.d(
                "Warning",
                e.stackTrace[0].methodName + " : " + e.toString()
            )
        }
    }

    internal inner class Fetcher(
        var latch: CountDownLatch,
        var item: JSONObject,
        var index: Int,
        var codes: ArrayList<String?>,
        var errors: ArrayList<String>,
        var context: Context
    ) : Runnable {
        override fun run() {
            try {
                if (item.has("url")) {
                    val url = item.getString("url")
                    if (url.startsWith("file://")) {
                        fetch_local(url, context)
                    } else if (url.startsWith("http")) {
                        fetch_remote(url, context)
                    } else {
                        errors.add("url must be either file:// or http:// or https://")
                        latch.countDown()
                    }
                } else if (item.has("text")) {
                    val code = item.getString("text")
                    codes[index] = code
                    latch.countDown()
                }
            } catch (e: Exception) {
                Log.d(
                    "Warning",
                    e.stackTrace[0].methodName + " : " + e.toString()
                )
                latch.countDown()
            }
        }

        fun fetch_local(url: String, context: Context?) {
            try {
                val r = Runnable {
                    try {
                        val code = JasonHelper.read_file_scheme(url, context)
                        codes[index] = code
                        latch.countDown()
                    } catch (e: Exception) {
                        Log.d(
                            "Warning",
                            e.stackTrace[0].methodName + " : " + e.toString()
                        )
                        errors.add("Couldn't read the file")
                    }
                }
                val t = Thread(r)
                t.start()
            } catch (e: Exception) {
                Log.d(
                    "Warning",
                    e.stackTrace[0].methodName + " : " + e.toString()
                )
                latch.countDown()
            }
        }

        private fun fetch_remote(url: String, context: Context) {
            try {
                val request: Request
                val builder = Request.Builder()
                request = builder.url(url).build()
                val client =
                    ((context as JasonViewActivity).application as Launcher).getHttpClient(0)
                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        errors.add("Failed to fetch from url")
                        latch.countDown()
                        e.printStackTrace()
                    }

                    @Throws(IOException::class)
                    override fun onResponse(call: Call, response: Response) {
                        try {
                            if (!response.isSuccessful) {
                                errors.add("Response was not successful")
                                latch.countDown()
                            } else {
                                val code = response.body!!.string()
                                codes[index] = code
                                latch.countDown()
                            }
                        } catch (e: Exception) {
                            Log.d(
                                "Warning",
                                e.stackTrace[0].methodName + " : " + e.toString()
                            )
                            latch.countDown()
                        }
                    }
                })
            } catch (e: Exception) {
                Log.d(
                    "Warning",
                    e.stackTrace[0].methodName + " : " + e.toString()
                )
                latch.countDown()
            }
        }

    }
}