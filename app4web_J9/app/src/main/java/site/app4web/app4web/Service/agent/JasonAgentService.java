package site.app4web.app4web.Service.agent;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import site.app4web.app4web.Core.JasonParser;
import site.app4web.app4web.Core.JasonViewActivity;
import site.app4web.app4web.Helper.JasonHelper;
import site.app4web.app4web.Launcher.Launcher;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class JasonAgentService {

    private JSONObject pending = new JSONObject();
    private JSONObject pending_injections = new JSONObject();

    // Initialize
    // Инициализация
    public JasonAgentService() {
    }
    private class JasonAgentInterface {

        private WebView agent;
        private Context context;

        public JasonAgentInterface(WebView agent, Context context) {
            this.agent = agent;
            this.context = context;
        }

        // $agent to Jasonette interface
        // $ агент к интерфейсу Jasonette
        @JavascriptInterface
        public void postMessage(String json) {
            /****************************************

             A message can be:
             Сообщение может быть:

             1. Request
             1. Запрос

             {
             "request": {
             "data": [RPC Object],
             "nonce": [Auto-generated nonce to handle return values later]
             }
             }

             2. Response
             2. Ответ

             {
             "response": {
             "data": [Return Value]
             }
             }

             3. Trigger
             3. Триггер

             {
             "trigger": {
             "name": [Jasonette Event Name],
             "data": [The "$jason" value to pass along with the event]
             }
             }

             4. Href
             4. Hyper REFerence это основной и необходимый атрибут тега A,
                указывающий браузеру, какое действие надо выполнить при клике на ссылку.

             {
             "href": {
             "data": [Jasonette HREF object]
             }
             }

             ****************************************/


            try {
                JSONObject message = new JSONObject(json);
                JSONObject transaction = (JSONObject) this.agent.getTag();

                if (message.has("request")) {
                    /***
                     1. Request: Agent making request to another agent
                     1. Запрос: агент делает запрос другому агенту

                     {
                     "request": {
                     "data": [RPC Object],
                     "nonce": [Auto-generated nonce to handle return values later]
                     }
                     }
                     ***/


                    if (transaction.has("to")) {
                        JSONObject content = message.getJSONObject("request");

                        /**
                         Compose an agent_request object
                         Составьте объект agent_request

                         agent_request := {
                         "from": [SOURCE AGENT ID],
                         "request": [JSON-RPC Object],
                         "nonce": [NONCE]
                         }

                         **/
                        JSONObject agent_request = new JSONObject();
                        agent_request.put("from", transaction.getString("to"));
                        agent_request.put("request", content.getJSONObject("data"));
                        agent_request.put("nonce", content.getString("nonce"));

                        request(null, agent_request, this.context);

                    }

                } else if (message.has("response")) {
                    /*****************

                     2. Response
                     2. Ответ

                     {
                     "response": {
                     "data": [Return Value]
                     }
                     }

                     *****************/
                    if (transaction.has("to")) {
                        JSONObject res = message.getJSONObject("response");

                        // Params
                        Object param = res.get("data");


                        // "from": exists => the caller request was from another agent
                        if (transaction.has("from")) {
                            /**
                             Compose an agent_request object
                             Составьте объект agent_request

                             agent_request := {
                             "from": [SOURCE AGENT ID],
                             "request": [JSON-RPC Object],
                             "nonce": [NONCE]
                             }

                             **/
                            JSONArray params = new JSONArray();
                            params.put(param);

                            // from
                            String from = transaction.getString("from");

                            if (transaction.has("nonce")) {

                                // 1. Construct jsonrpc
                                // 1. Построить jsonrpc
                                JSONObject jsonrpc = new JSONObject();

                                // 1.1. Method: Call the callback method stored under $agent.callbacks object
                                // 1.1. Метод: вызов метода обратного вызова, хранящегося в объекте $ agent.callbacks
                                String method = "$agent.callbacks[\"" + transaction.getString("nonce") + "\"]";
                                jsonrpc.put("method", method);

                                // 1.2. id: need to call the callback method on the "from" agent
                                // 1.2. id: необходимо вызвать метод обратного вызова на агенте "from"
                                jsonrpc.put("id", from);

                                // 1.3. params array
                                // 1.3. массив параметров
                                jsonrpc.put("params", params);

                                // 2. Construct the callback agent_request
                                // 2. Построить обратный вызов agent_request
                                JSONObject agent_request = new JSONObject();
                                // 2.1. "from" and "nonce" should be set to null, since this is a return value, and there is no coming back again.
                                // 2.1. "from" и "nonce" должны быть установлены в null, так как это возвращаемое значение, и возвращаться больше нельзя.
                                agent_request.put("from", null);
                                agent_request.put("nonce", null);
                                // 2.2. set JSON RPC request
                                // 2.2. установить запрос JSON RPC
                                agent_request.put("request", jsonrpc);

                                request(null, agent_request, this.context);
                            }

                            // "from" doesn't exist => call from Jasonette => return via success callback
                            // "из" не существует => вызов из Jasonette => возврат через успешный обратный вызов
                        } else {
                            if (transaction.has("jason")) {
                                JSONObject original_action = transaction.getJSONObject("jason");
                                // return param as a return value
                                JasonHelper.next("success", original_action, param, new JSONObject(), context);
                            }
                        }

                    }

                } else if (message.has("trigger")) {
                    /************************

                     3. Trigger
                     3. Триггер // спусковой крючок

                     {
                     "trigger": {
                     "name": [Jasonette Event Name],
                     "data": [The "$jason" value to pass along with the event]
                     }
                     }

                     ************************/

                    // If the parent is not the same, don't trigger
                    // Если родитель не тот же, не запускать
                    String current_url = ((JasonViewActivity) context).model.url;
                    if (transaction.has("parent") && !transaction.getString("parent").equalsIgnoreCase(current_url)) {
                        return;
                    }

                    JSONObject trigger = message.getJSONObject("trigger");
                    JSONObject m = new JSONObject();
                    if(trigger.has("name")) {
                        // trigger
                        // спусковой крючок
                        m.put("trigger", trigger.getString("name"));
                        // options
                        // опции
                        if (trigger.has("data")) {
                            m.put("options", trigger.get("data"));
                        }

                        // Call the Jasonette event
                        // Вызов события Jasonette
                        Intent intent = new Intent("call");
                        intent.putExtra("action", m.toString());
                        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

                    }


                } else if (message.has("href")) {
                    /************************************

                     4. Href

                     {
                     "href": {
                     "data": [Jasonette HREF object]
                     }
                     }

                     ************************************/

                    if (message.getJSONObject("href").has("data")) {
                        JSONObject m = message.getJSONObject("href").getJSONObject("data");
                        Intent intent = new Intent("call");
                        JSONObject href = new JSONObject();
                        href.put("type", "$href");
                        href.put("options", m);
                        intent.putExtra("action", href.toString());
                        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                    }


                }
            } catch (Exception e) {
                Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
            }
        }



    }

    public WebView setup(final JasonViewActivity context, final JSONObject options, final String id) {

        /**

         1. Initialize WebView
         - Does an agent with the ID exist already?
         YES => Use that one
         NO =>
         1. Create a new WebView
         2. Attach to the parent view's agents object

         2. Creating a WebView
         - Hide it
         - Set metadata payload on it so it can be referenced later

         3. Filling a WebView
         - Is the state "empty"?
         YES => Load
         NO => Ignore

         1. Инициализируйте WebView
                   - Агент с идентификатором уже существует?
                   ДА => Используйте это
                   НЕТ =>
                   1. Создайте новый WebView
                   2. Присоедините к объекту агентов родительского представления

                   2. Создание WebView
                   - Скрыть
                   - Установите полезную нагрузку метаданных, чтобы на нее можно было ссылаться позже.

                   3. Заполнение WebView
                   - Является ли государство «пустым»?
                   ДА => Загрузить
                   НЕТ => Игнорировать

         **/
        WebView agent;


        /*******************************************
         1. Initialize WebView

         - Does an agent with the ID exist already?
         YES => Use that one
         NO =>
         1. Create a new WebView
         2. Attach to the parent view's agents object

         1. Инициализируйте WebView

                   - Агент с идентификатором уже существует?
                   ДА => Используйте это
                   НЕТ =>
                   1. Создайте новый WebView
                   2. Присоедините к объекту агентов родительского представления

         *******************************************/
        try {
            // An agent with the ID already exists
            // Агент с идентификатором уже существует
            if (context.agents.has(id)) {
                agent = (WebView) context.agents.get(id);

                // No such agent exists yet. Create one.
                // Такой агент еще не существует. Создай.
            } else {

                /*******************************************
                 2. Creating a WebView
                 2. Создание WebView
                 *******************************************/

                // 2.1. Initialize
                // 2.1. инициализировать
                CookieManager.getInstance().setAcceptCookie(true);
                agent = new WebView(context);
                agent.getSettings().setDefaultTextEncodingName("utf-8");

                if (id.startsWith("$webcontainer")) {
                    ProgressBar pBar;
                    if (agent.findViewById(42) == null) {
                        pBar = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
                        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                        layoutParams.addRule(RelativeLayout.ALIGN_BOTTOM, agent.getId());
                        layoutParams.height = 5;
                        pBar.setScaleY(4f);
                        pBar.setLayoutParams(layoutParams);
                        int color;
                        if (options.has("style") && options.getJSONObject("style").has("progress")) {
                            color = JasonHelper.parse_color(options.getJSONObject("style").getString("progress"));
                        } else {
                            color = JasonHelper.parse_color("#007AFF");
                        }
                        pBar.getProgressDrawable().setColorFilter( color, android.graphics.PorterDuff.Mode.SRC_IN);
                        pBar.setId(42);
                    } else {
                        pBar = agent.findViewById(42);
                    }
                    final ProgressBar progressBar = pBar;
                    agent.addView(progressBar);
                    agent.setWebChromeClient(new WebChromeClient() {
                        public void onProgressChanged(WebView view, int progress) {
                            progressBar.setProgress(progress);
                            if (progress == 100) {
                                progressBar.setVisibility(View.GONE);

                            } else {
                                progressBar.setVisibility(View.VISIBLE);
                            }
                        }
                    });
                } else {
                    agent.setWebChromeClient(new WebChromeClient());
                }
                agent.setWebViewClient(new WebViewClient() {
                    @Override public void onPageFinished(WebView view, final String url) {
                        // Inject agent.js
                        // Внедрить agent.js
                        try {
                            String injection_script = JasonHelper.read_file("agent", context);
                            String interface_script = "$agent.interface.postMessage = function(r) { JASON.postMessage(JSON.stringify(r)); };";
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                                view.evaluateJavascript(injection_script + " " + interface_script, null);
                            } else {
                                view.loadUrl("javascript:" + injection_script + " " + interface_script);
                            }

                            if (pending.has(id)) {
                                JSONObject q = pending.getJSONObject(id);
                                JSONObject jason_request;
                                if (q.has("jason_request")) {
                                    jason_request = q.getJSONObject("jason_request");
                                } else {
                                    jason_request = null;
                                }
                                JSONObject agent_request;
                                if (q.has("agent_request")) {
                                    agent_request = q.getJSONObject("agent_request");
                                } else {
                                    agent_request = null;
                                }
                                request(jason_request, agent_request, context);
                                pending.remove(id);
                            }

                            if (pending_injections.has(id)) {
                                inject(pending_injections.getJSONObject(id), context);
                                pending_injections.remove(id);
                            }

                            // only set state to rendered if it's not about:blank
                            // устанавливаем состояние только для визуализации, если это не так: blank
                            if (!url.equalsIgnoreCase("about:blank")) {
                                JSONObject payload = (JSONObject)view.getTag();
                                payload.put("state", "rendered");
                                view.setTag(payload);
                            }
                        } catch (Exception e) {
                            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
                        }
                    }
                    @Override public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        // resolve
                        // разрешить
                        final AtomicReference<JSONObject> notifier = new AtomicReference<>();

                        try {


                            JSONObject payload = (JSONObject)view.getTag();

                            // Only override behavior for web container
                            // только переопределить поведение для веб-контейнера
                            if (!id.startsWith("$webcontainer")) {
                                return false;
                            }

                            if (!payload.has("url")) {
                                return false;
                            }



                            if(view.getHitTestResult().getType() > 0){
                                if (options.has("action")) {
                                    // 1. Parse the action if it's a trigger

                                    Object resolved_action;
                                    if (options.getJSONObject("action").has("trigger")) {
                                        String event_name = options.getJSONObject("action").getString("trigger");
                                        JSONObject head = context.model.jason.getJSONObject("$jason").getJSONObject("head");
                                        JSONObject events = head.getJSONObject("actions");
                                        // Look up an action by event_name
                                        resolved_action = events.get(event_name);
                                    } else {
                                        resolved_action = options.get("action");
                                    }



                                    /* set $jason */
                                    /* установить $ Джейсон */
                                    JSONObject u = new JSONObject();
                                    u.put("url", url);
                                    JSONObject ev = new JSONObject();
                                    ev.put("$jason", u);
                                    context.model.set("state", ev);

                                    JasonParser.getInstance(context).setParserListener(new JasonParser.JasonParserListener() {
                                       @Override
                                       public void onFinished(JSONObject reduced_action) {
                                           synchronized (notifier) {
                                               notifier.set(reduced_action);
                                               notifier.notify();
                                           }
                                       }
                                   });
                                    JasonParser.getInstance(context).parse("json", context.model.state, resolved_action, context);

                                    synchronized (notifier) {
                                        while (notifier.get() == null) {
                                            notifier.wait();
                                        }
                                    }

                                    JSONObject parsed = notifier.get();

                                    if (parsed.has("type") && parsed.getString("type").equalsIgnoreCase("$default")) {
                                        return false;
                                    } else {
                                        Intent intent = new Intent("call");
                                        intent.putExtra("action", options.get("action").toString());

                                        // file url handling
                                        // обработка URL файла
                                        if (url.startsWith("file://")) {
                                            if (url.startsWith("file:///android_asset/file")) {
                                                url = url.replace("/android_asset/file/", "");
                                            }
                                        }

                                        intent.putExtra("data", "{\"url\": \"" + url + "\"}");
                                        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                                        return true;
                                    }

                                }
                            }
                            payload.put("state", "loading");
                            view.setTag(payload);
                        } catch (Exception e) {
                            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
                            notifier.notify();
                        }
                        return false;
                    }
                });
                agent.setVerticalScrollBarEnabled(false);
                agent.setHorizontalScrollBarEnabled(false);
                if (options.has("style") && options.getJSONObject("style").has("background")) {
                    agent.setBackgroundColor(JasonHelper.parse_color(options.getJSONObject("style").getString("background")));
                } else {
                    agent.setBackgroundColor(Color.TRANSPARENT);
                }
                WebSettings settings = agent.getSettings();
                settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
                settings.setJavaScriptEnabled(true);
                settings.setDomStorageEnabled(true);
                settings.setMediaPlaybackRequiresUserGesture(false);
                settings.setJavaScriptCanOpenWindowsAutomatically(true);
                // settings.setAppCachePath(context.getCacheDir().getAbsolutePath()); // 26092022
                settings.setAllowFileAccess(true);
                //settings.setAppCacheEnabled(true); //26092022
                settings.setCacheMode(WebSettings.LOAD_DEFAULT);

                // 2.2. Create and Attach JavaScript Interface
                // 2.2. Создать и присоединить интерфейс JavaScript
                JasonAgentInterface agentInterface = new JasonAgentInterface(agent, context);
                agent.addJavascriptInterface(agentInterface, "JASON");

                // 2.3. Hide it
                // 2.3. Скрыть
                agent.setVisibility(View.GONE);

                // 2.3. Set Payload
                // 2.3. Установить полезную нагрузку
                JSONObject payload = new JSONObject();
                // 2.3.1. copy options
                // 2.3.1. варианты копирования
                for (Iterator<String> iterator = options.keys(); iterator.hasNext(); ) {
                    String      key     = iterator.next();
                    Object  value   = options.get(key);
                    payload.put(key, value);
                }
                // 2.3.2. Add 'id' and 'state'
                // 2.3.2. Добавьте «id» и «state»
                payload.put("to", id);
                payload.put("state", "empty");
                payload.put("parent", context.model.url);
                // 2.3.3. Set
                // 2.3.3. Задавать
                agent.setTag(payload);

                // 2.4. Attach to the parent view
                // 2.4. Прикрепить к родительскому представлению
                context.agents.put(id, agent);

            }

            /*******************************************

             3. Filling the WebView with content
             - Is the state "empty"?
             YES => Load
             NO => Ignore

             3. Наполнение WebView содержимым
                           - Является ли государство «пустым»?
                           ДА => Загрузить
                           НЕТ => Игнорировать
             *******************************************/

            JSONObject payload = (JSONObject)agent.getTag();

            // Fill in the content if empty
            // Заполнить содержимое, если оно пустое
            if(payload.has("state") && payload.getString("state").equalsIgnoreCase("empty")) {
                if (options.has("text")) {
                    String html = options.getString("text");
                    agent.loadDataWithBaseURL("http://localhost/", html, "text/html", "utf-8", null);
                } else if (options.has("url")) {
                    String url = options.getString("url");
                    // 1. file url
                    // 1. файл url
                    if (url.startsWith("file://")) {
                        agent.loadUrl("file:///android_asset/file/" + url.substring(7));
                        // 2. remote url
                        // 2. удаленный URL
                    } else {
                        agent.loadUrl(url);
                    }
                }
            }

        } catch (Exception e) {
            agent = new WebView(context);
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
        return agent;
    }



    public void jason_request(final JSONObject jason_request, Context context) {
        request(jason_request, null, context);
    }
    public void request(final JSONObject jason_request, final JSONObject agent_request, Context context) {
        try {

            /*****************************

             1. jason_request is an entire Jasonette action which contains a JSON-RPC object as its options

             jason_request := {
             "type": "$agent.request",
             "options": [JSON-RPC Object],
             "success": [Next Jasonette Action]
             }

             2. agent_request is a JSON-RPC object

             agent_request := {
             "from": [SOURCE AGENT ID],
             "request": [JSON-RPC Object],
             "nonce": [NONCE]
             }

             1. jason_request - это целое действие Jasonette, которое содержит объект JSON-RPC в качестве параметров

                           jason_request: = {
                           "type": "$ agent.request",
                           "options": [JSON-RPC Object],
                           «успех»: [Следующее действие Jasonette]
                           }

             2. agent_request является объектом JSON-RPC

                           agent_request: = {
                           "от": [ИДЕНТИФИКАТОР ИСТОЧНИКА]
                           "запрос": [объект JSON-RPC],
                           "nonce": [NONCE]
                           }

             *****************************/


            /***

             0. Check if it's jason_request or agent_request
             - If jason_request => Use this to proceed
             - If agent_request => Use this to proceed

             1. Get JSON-RPC arguments

             - id
             - method
             - params

             2. Find the agent by ID
             - iF agent exists, go on.

             3. Set the $source on the agent's payload (tag)

             4. Create a JavaScript call string

             5. Run the call script

             0. Проверьте, является ли это jason_request или agent_request
                           - Если jason_request => Используйте это, чтобы продолжить
                           - Если agent_request => Используйте это, чтобы продолжить

             1. Получить аргументы JSON-RPC

                           - Я бы
                           - метод
                           - параметры

             2. Найти агента по ID
                           - Если агент существует, продолжайте.

             3. Установите $ source на полезную нагрузку агента (тег)

             4. Создайте строку вызова JavaScript

             5. Запустите скрипт вызова

             ***/

            // Get JSON RPC object
            // Получить объект JSON RPC
            JSONObject jsonrpc;
            if (jason_request != null) {
                /**
                 1. jason_request is an entire Jasonette action which contains a JSON-RPC object as its options
                 1. jason_request - это целое действие Jasonette, которое содержит объект JSON-RPC в качестве параметров

                 jason_request := {
                 "type": "$agent.request",
                 "options": [JSON-RPC Object],
                 "success": [Next Jasonette Action]
                 }

                 **/
                jsonrpc = jason_request.getJSONObject("options");
            } else {
                /**
                 2. agent_request is a JSON-RPC object
                 2. agent_request является объектом JSON-RPC

                 agent_request := {
                 "from": [SOURCE AGENT ID],
                 "request": [JSON-RPC Object],
                 "nonce": [NONCE]
                 }

                 **/
                jsonrpc = agent_request.getJSONObject("request");
            }



            if (jsonrpc.has("id")) {
                String identifier = jsonrpc.getString("id");

                // Special case handling for $webcontainers (For sandboxing per view)
                // Обработка особого случая для $ webcontainers (для песочницы для представления)
                if (identifier.equalsIgnoreCase("$webcontainer")) {
                    identifier = "$webcontainer@" + ((JasonViewActivity)context).model.url;
                }


                if(((JasonViewActivity)context).agents.has(identifier)) {
                    // Find agent by ID
                    // Найти агента по ID
                    final WebView agent = (WebView)((JasonViewActivity)context).agents.get(identifier);


                    /**

                     A transaction looks like this:
                     Транзакция выглядит так:

                     {
                     "state": "empty",
                     "to": [Agent ID],
                     "from": [Desitnation Agent ID],
                     "nonce": [NONCE],
                     "jason": [The original Jason action that triggered everything],
                     "request": [JSON RPC object]
                     }

                     **/


                    // Set Transaction payload
                    // Установить полезную нагрузку транзакции

                    // 1. Initialize
                    // 1. Инициализация
                    JSONObject transaction;
                    if (agent.getTag() != null) {
                        transaction = (JSONObject)agent.getTag();
                    } else {
                        transaction = new JSONObject();
                    }

                    // 2. Fill in
                    // 2. Заполнить
                    if (jason_request != null) {
                        // 2.1. From: Set it as the caller Jasonette action
                        // 2.1. От: Установите это как вызывающее действие Jasonette
                        transaction.put("from", null);
                        // 2.2. Nonce
                        // 2.2. данное время
                        transaction.put("nonce", null);
                        // 2.3. Original JASON Caller Action
                        // 2.3. Оригинальный JASON Caller Action
                        transaction.put("jason", jason_request);
                    } else {
                        // 2.1. From: Set it as the caller agent ID
                        // 2.1. От: Установите его как идентификатор вызывающего агента
                        if (agent_request.has("from")) {
                            transaction.put("from", agent_request.getString("from"));
                        }
                        // 2.2. Nonce
                        // 2.2. данное время
                        if (agent_request.has("nonce")) {
                            transaction.put("nonce", agent_request.getString("nonce"));
                        }
                    }

                    agent.setTag(transaction);

                    // Create a JS call string
                    // Создать строку вызова JS
                    String params = "null";
                    if (jsonrpc.has("method")) {
                        String method = jsonrpc.getString("method");
                        if (jsonrpc.has("params")) {
                            params = jsonrpc.getJSONArray("params").toString();
                        }
                        final String callstring = method + ".apply(this, " + params + ");";
                        ((JasonViewActivity) context).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                                    agent.evaluateJavascript(callstring, null);
                                } else {
                                    agent.loadUrl("javascript:" + callstring);
                                }
                            }
                        });
                        return;
                    }
                } else {
                    // If the agent is not yet ready, put it in a pending queue,
                    // this will be triggered later when the webview becomes ready
                    // Если агент еще не готов, поместите его в очередь ожидания,
                    // это будет запущено позже, когда веб-представление будет готово
                    JSONObject q = new JSONObject();
                    q.put("jason_request", jason_request);
                    q.put("agent_request", agent_request);
                    pending.put(identifier, q);
                }
            }

        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
    }
    public void refresh(final JSONObject action, final Context context) {
        // Get JSON RPC object
        // Получить объект JSON RPC
        /**

         action := {
         "type": "$agent.refresh",
         "options": {
         "id": [AGENT ID]
         },
         "success": [Next Jasonette Action]
         }

         **/
        try {
            JSONObject jsonrpc = action.getJSONObject("options");
            if (jsonrpc.has("id")) {
                String identifier = jsonrpc.getString("id");
                if (identifier.equalsIgnoreCase("$webcontainer")) {
                    identifier = "$webcontainer@" + ((JasonViewActivity) context).model.url;
                }
                if (((JasonViewActivity) context).agents.has(identifier)) {
                    final WebView agent = (WebView) ((JasonViewActivity) context).agents.get(identifier);
                    // Find agent by ID
                    ((JasonViewActivity) context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            agent.loadUrl(agent.getUrl());
                        }
                    });
                    JasonHelper.next("success", action, new JSONObject(), new JSONObject(), context);
                    return;
                }
            }
        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
        JasonHelper.next("error", action, new JSONObject(), new JSONObject(), context);
    }
    public void clear(final JSONObject action, final Context context) {
        // Get JSON RPC object
        // Получить объект JSON RPC
        /**

         action := {
         "type": "$agent.clear",
         "options": {
         "id": [AGENT ID]
         },
         "success": [Next Jasonette Action]
         }

         **/
        try {
            JSONObject jsonrpc = action.getJSONObject("options");
            if (jsonrpc.has("id")) {
                String identifier = jsonrpc.getString("id");
                if (identifier.equalsIgnoreCase("$webcontainer")) {
                    identifier = "$webcontainer@" + ((JasonViewActivity) context).model.url;
                }
                if (((JasonViewActivity) context).agents.has(identifier)) {
                    // Find agent by ID
                    final WebView agent = (WebView) ((JasonViewActivity) context).agents.get(identifier);
                    ((JasonViewActivity) context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                JSONObject newTag = (JSONObject)agent.getTag();
                                newTag.put("state", "empty");
                                agent.setTag(newTag);
                            } catch (Exception e) {
                                Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
                            }
                            agent.loadUrl("about:blank");
                        }
                    });
                    JasonHelper.next("success", action, new JSONObject(), new JSONObject(), context);
                    return;
                }
            }
        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
        JasonHelper.next("error", action, new JSONObject(), new JSONObject(), context);
    }

    /*************************************

     $agent.inject: Inject JavaScript into $agent context
     $ agent.inject: добавить JavaScript в контекст $ agent

     {
     "type": "$agent.inject",
     "options": {
     "id": "app",
     "items": [{
     "url": "file://authentication.js"
     }]
     },
     "success": {
     "type": "$agent.request",
     "options": {
     "id": "app",
     "method": "login",
     "params": ["eth", "12341234"]
     }
     }
     }

     *************************************/
    public void inject(final JSONObject action, final Context context) {
        // id
        // items

        try {
            JSONObject options = action.getJSONObject("options");
            if (options.has("id")) {
                String identifier = options.getString("id");
                if (identifier.equalsIgnoreCase("$webcontainer")) {
                    identifier = "$webcontainer@" + ((JasonViewActivity) context).model.url;
                }
                if (((JasonViewActivity) context).agents.has(identifier)) {
                    final WebView agent = (WebView) ((JasonViewActivity) context).agents.get(identifier);
                    if (options.has("items")) {
                        JSONArray items = options.getJSONArray("items");
                        CountDownLatch latch = new CountDownLatch(items.length());
                        ExecutorService taskExecutor = Executors.newFixedThreadPool(items.length());
                        ArrayList<String> codes = new ArrayList<String>();
                        for (int i=0; i<items.length(); i++) {
                            codes.add("");
                        }
                        ArrayList<String> errors = new ArrayList<String>();

                        for (int i=0; i<items.length(); i++) {
                            final JSONObject item = items.getJSONObject(i);
                            taskExecutor.submit(new Fetcher(latch, item, i, codes, errors, context));
                        }
                        try {
                            latch.await();
                        } catch (Exception e) {
                            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
                        }

                        // All finished. Now can utilize codes
                        // Все закончено. Теперь можно использовать коды

                        String code_string = "";
                        for(String s : codes) {
                            code_string = code_string + s + "\n";
                        }
                        final String codestr = code_string;
                        ((JasonViewActivity) context).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                                    agent.evaluateJavascript(codestr, null);
                                } else {
                                    agent.loadUrl("javascript:" + codestr);
                                }
                                JasonHelper.next("success", action, new JSONObject(), new JSONObject(), context);
                            }
                        });
                    } else {
                        JSONObject error = new JSONObject();
                        error.put("message", "need to specify items");
                        JasonHelper.next("error", action, error, new JSONObject(), context);
                    }
                } else {
                    pending_injections.put(identifier, action);
                }
            }
        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }

    }

    class Fetcher implements Runnable {
        CountDownLatch latch;
        JSONObject item;
        ArrayList<String> errors;
        ArrayList<String> codes;
        Context context;
        int index;
        public Fetcher(CountDownLatch latch, JSONObject item, int index, ArrayList<String> codes, ArrayList<String> errors, Context context) {
            this.latch = latch;
            this.item = item;
            this.context = context;
            this.index = index;
            this.codes = codes;
            this.errors = errors;
        }
        public void run() {
            try {
                if (item.has("url")) {
                    String url = item.getString("url");
                    if (url.startsWith("file://")) {
                        fetch_local(url, context);
                    } else if (url.startsWith("http")) {
                        fetch_remote(url, context);
                    } else {
                        errors.add("url must be either file:// or http:// or https://");
                        latch.countDown();
                    }
                } else if (item.has("text")) {
                    String code = item.getString("text");
                    codes.set(index, code);
                    latch.countDown();
                }
            } catch (Exception e) {
                Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
                latch.countDown();
            }
        }
        public void fetch_local(final String url, final Context context){
            try {
                Runnable r = new Runnable()
                {
                    @Override
                    public void run()
                    {
                        try {
                            String code = JasonHelper.read_file_scheme(url, context);
                            codes.set(index, code);
                            latch.countDown();
                        } catch (Exception e) {
                            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
                            errors.add("Couldn't read the file");
                        }
                    }
                };
                Thread t = new Thread(r);
                t.start();
            } catch (Exception e) {
                Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
                latch.countDown();
            }
        }

        private void fetch_remote(String url, Context context){
            try{
                Request request;
                Request.Builder builder = new Request.Builder();
                request = builder.url(url).build();
                OkHttpClient client = ((Launcher)((JasonViewActivity)context).getApplication()).getHttpClient(0);
                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        errors.add("Failed to fetch from url");
                        latch.countDown();
                        e.printStackTrace();
                    }

                    @Override
                    public void onResponse(Call call, final Response response) throws IOException {
                        try {
                            if (!response.isSuccessful()) {
                                errors.add("Response was not successful");
                                latch.countDown();
                            } else {
                                String code = response.body().string();
                                codes.set(index, code);
                                latch.countDown();
                            }
                        } catch (Exception e) {
                            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
                            latch.countDown();
                        }
                    }
                });
            } catch (Exception e){
                Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
                latch.countDown();
            }
        }
    }
}