package site.app4web.app4web.Launcher

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Resources
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import com.bumptech.glide.request.target.ViewTarget
import site.app4web.app4web.Core.JasonModel
import site.app4web.app4web.Core.JasonViewActivity
import site.app4web.app4web.Helper.JasonHelper
import site.app4web.app4web.R
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.InputStream
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import site.app4web.app4web.BuildConfig
import site.app4web.app4web.Service.agent.JasonAgentService
import site.app4web.app4web.Service.websocket.JasonWebsocketService
import site.app4web.app4web.UI.Setting
import site.app4web.app4web.UI.Setting.Companion.CreateSetting


open class Launcher : Application() {
    private var handlers: JSONObject? = null
    var global: JSONObject? = null
        private set
    var env: JSONObject? = null
        private set
    private var models: JSONObject? = null
    var services: JSONObject? = null
    fun call(serviceName: String?, methodName: String?, action: JSONObject, context: Context?) {
        try {
            val service = services!![serviceName]
            val method =
                service.javaClass.getMethod(methodName, action.javaClass, Context::class.java)
            method.invoke(service, action, context)
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    }

    fun setTabModel(url: String?, model: JasonModel?) {
        try {
            models!!.put(url, model)
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    }

    fun getTabModel(url: String?): JasonModel? {
        return try {
            if (models!!.has(url)) {
                models!![url] as JasonModel
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun setEnv(key: String?, json: Any?) {
        try {
            env!!.put(key, json)
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    }

    fun setGlobal(key: String?, json: Any?) {
        try {
            global!!.put(key, json)
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    }

    fun resetGlobal(key: String?) {
        try {
            global!!.remove(key)
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    }

    override fun onCreate() {
        super.onCreate()
        //new Setting();     // Вставил ДО для Setting
        // в пакет com.bumptech.glide.request.target.ViewTarget
        // устанавливается glide_request (запрос скольжения )- из ids.xml
        ViewTarget.setTagId(R.id.glide_request)

        // Look for all extensions and initialize them if they have initialize class methods
        // Ищите все расширения и инициализируйте их, если у них есть методы класса инициализации
        // Считывает весь список файлов из Assets/file; считывает их имена в массив
        // открывает и считывае по очереди в jr; ищет в каждом classname и создает такие классы
        // Далее определяются и укладываются глобальные сведения где запущены версия сборка итп
        try {
            val fileList = assets.list("file")
            for (i in fileList!!.indices) {
                val filename = fileList[i]
                var jr: String? = null
                try {
                    val `is` = assets.open("file/$filename")
                    val size = `is`.available()
                    val buffer = ByteArray(size)
                    `is`.read(buffer)
                    `is`.close()
                    jr = String(buffer, StandardCharsets.UTF_8)
                    val jrjson = JSONObject(jr)
                    if (jrjson.has("classname")) {
                        val resolved_classname =
                            "site.app4web.app4web.Action." + jrjson.getString("classname")
                        val classmethodName = "initialize"
                        val classObject = Class.forName(resolved_classname)
                        val classMethod = classObject.getMethod("initialize", Context::class.java)
                        classMethod.invoke(classObject, applicationContext)
                    }
                } catch (e: Exception) {
                    Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
                }
            }
            services = JSONObject()
            val websocketService = JasonWebsocketService(this)
            val agentService = JasonAgentService()
            services!!.put("JasonWebsocketService", websocketService)
            services!!.put("JasonAgentService", agentService)


            // handler init
            // обработчик init
            handlers = JSONObject()

            // $global
            val global_pref = getSharedPreferences("global", 0)
            global = JSONObject()
            if (global_pref != null) {
                val map = global_pref.all
                for ((key, value) in map) {
                    try {
                        val `val` = value as String
                        val json = JSONTokener(`val`).nextValue()
                        if (json is JSONObject) {
                            global!!.put(key, JSONObject(`val`))
                        } else if (json is JSONArray) {
                            global!!.put(key, JSONArray(`val`))
                        }
                    } catch (e: Exception) {
                        Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
                    }
                }
            }
            env = JSONObject()
            models = JSONObject()

            // device info
            // информация об устройстве
            val device = JSONObject()
            val displayMetrics = Resources.getSystem().displayMetrics
            val width = displayMetrics.widthPixels / displayMetrics.density
            val height = displayMetrics.heightPixels / displayMetrics.density
            device.put("width", width.toDouble())
            device.put("height", height.toDouble())
            device.put("language", Locale.getDefault().toString())
            val os = JSONObject()
            os.put("name", "android")
            os.put("version", Build.VERSION.RELEASE)
            os.put("sdk", Build.VERSION.SDK_INT)
            device.put("os", os)
            env!!.put("device", device)
            val app = JSONObject()
            app.put("version", BuildConfig.VERSION_NAME)
            app.put("build", Integer.toString(BuildConfig.VERSION_CODE))
            env!!.put("app", app)
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    } // end onCreate()

    /***************************
     *
     * Intent schedule/trigger
     * График намерений / триггер
     *
     */
    fun on(key: String?, `val`: JSONObject?) {
        try {
            val store = JSONObject()
            store.put("type", "on")
            store.put("content", `val`)
            handlers!!.put(key, store)
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    }

    fun once(key: String?, `val`: JSONObject?) {
        try {
            val store = JSONObject()
            store.put("type", "once")
            store.put("content", `val`)
            handlers!!.put(key, store)
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    }

    fun trigger(intent_to_resolve: JSONObject, context: JasonViewActivity) {
        try {
            val type = intent_to_resolve.getString("type")
            if (type.equals("success", ignoreCase = true)) {
                // success
                val name = intent_to_resolve["name"]
                val handler: JSONObject
                handler = if (name is String) {
                    getHandler(name)
                } else {
                    getHandler(intent_to_resolve.getInt("name").toString())
                }
                val intent: Intent?
                intent = if (intent_to_resolve.has("intent")) {
                    intent_to_resolve["intent"] as Intent
                } else {
                    null
                }
                val classname = handler.getString("class")
                val primaryClassname = "site.app4web.app4web.Action.$classname"
                val secondaryClassname = "site.app4web.app4web.Core.$classname"
                val methodname = handler.getString("method")
                val module: Any?
                if (context.modules!!.containsKey(primaryClassname)) {
                    module = context.modules!![primaryClassname]
                } else if (context.modules!!.containsKey(secondaryClassname)) {
                    module = context.modules!![secondaryClassname]
                } else {
                    val classObject = Class.forName(secondaryClassname)
                    val constructor = classObject.getConstructor()
                    module = constructor.newInstance()
                    context.modules!![secondaryClassname] = module
                }
                val method = module!!.javaClass.getMethod(
                    methodname,
                    Intent::class.java,
                    JSONObject::class.java
                )
                val options = handler.getJSONObject("options")
                method.invoke(module, intent, options)
            } else {
                // error
                // ошибка
                val handler = (context.applicationContext as Launcher).getHandler(
                    intent_to_resolve.getInt("name").toString()
                )
                if (handler.has("options")) {
                    val options = handler.getJSONObject("options")
                    val action = options.getJSONObject("action")
                    val event = options.getJSONObject("event")
                    val ctxt = options["context"] as Context
                    JasonHelper.next("error", action, JSONObject(), event, ctxt)
                }
            }

            // reset intent_to_resolve
            // сбросить intent_to_resolve
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    }

    fun callback(handler: JSONObject, result: String?, context: JasonViewActivity) {
        try {
            var classname = handler.getString("class")
            classname = "site.app4web.app4web.Action.$classname"
            val methodname = handler.getString("method")
            val module: Any?
            if (context.modules!!.containsKey(classname)) {
                module = context.modules!![classname]
            } else {
                val classObject = Class.forName(classname)
                val constructor = classObject.getConstructor()
                module = constructor.newInstance()
                context.modules!![classname] = module
            }
            val method =
                module!!.javaClass.getMethod(methodname, JSONObject::class.java, String::class.java)
            //JSONObject options = handler.getJSONObject("options");
            method.invoke(module, handler, result)
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    }

    // Private
    // Частный
    private fun getHandler(key: String): JSONObject {
        return try {
            // 1. gets the handler
            val handler = handlers!!.getJSONObject(key)
            if (handler.has("type")) {
                if (handler.getString("type").equals("once", ignoreCase = true)) {
                    // "once" is a one time thing (Only triggered once):
                    // so we de-register it after getting
                    // «один раз» - вещь разовая (срабатывает только один раз):
                    // поэтому мы отменили регистрацию после получения
                    handlers!!.remove(key)
                }
            }
            handler.getJSONObject("content")
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
            JSONObject()
        }
    }

    // При отладке переопределяется в DebugLauncher с добавлением
    // .addNetworkInterceptor(new StethoInterceptor())
    open fun getHttpClient(timeout: Long): OkHttpClient {
        return if (timeout > 0) {
            OkHttpClient.Builder()
                .writeTimeout(timeout, TimeUnit.SECONDS)
                .readTimeout(timeout, TimeUnit.SECONDS)
                .build()
        } else {
            OkHttpClient.Builder().build()
        }
    }

    companion object {
        // get current context from anywhere
        // получить текущий контекст из любого места
        var currentContext: Context?
            get() = Launcher.Companion.currentContext
            set(context) {
                Launcher.Companion.currentContext = context
            }
        var setting = CreateSetting(null) // NO Синглетон от ДО
    }
}