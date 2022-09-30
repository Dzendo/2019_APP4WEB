package site.app4web.app4web.Core

import android.content.Intent
import android.net.Uri
import android.util.Log
import okhttp3.*
import org.json.JSONObject
import site.app4web.app4web.Core.JasonParser.JasonParserListener
import site.app4web.app4web.Helper.JasonHelper
import site.app4web.app4web.Launcher.Launcher
import java.io.IOException
import java.net.URI
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.regex.Pattern

class JasonModel(var url: String?, intent: Intent?, var view: JasonViewActivity) {
    var jason: JSONObject? = null
    var rendered: JSONObject? = null
    var state: JSONObject? = null
    var offline: Boolean
    var refs: JSONObject? = null

    // Variables
    var `var` // $get
            : JSONObject?
    var cache // $cache
            : JSONObject?
    var params // $params
            : JSONObject?
    var session: JSONObject?
    var action // latest executed action последнее выполненное действие
            : JSONObject? = null
    var client: OkHttpClient
    fun fetch() {
        if (url!!.startsWith("file://")) {
            fetch_local(url)
        } else {
            fetch_http(url)
        }
    }

    fun fetch_local(url: String?) {
        val context = view
        try {
            val r = Runnable {
                jason = JasonHelper.read_json(url, context) as JSONObject
                refs = JSONObject()
                resolve_and_build(jason.toString())
            }
            val t = Thread(r)
            t.start()
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    }

    private fun fetch_http(url: String?) {
        var url = url
        try {
            val request: Request
            val builder = Builder()

            // SESSION HANDLING
            // ОБРАЩЕНИЕ СЕССИИ

            // Attach Header from Session
            // Прикрепить заголовок из сессии
            if (session != null && session!!.has("header")) {
                val keys: Iterator<*> = session!!.getJSONObject("header").keys()
                while (keys.hasNext()) {
                    val key = keys.next() as String
                    val `val` = session!!.getJSONObject("header").getString(key)
                    builder.addHeader(key, `val`)
                }
            }
            // Attach Params from Session
            // Прикрепить параметры из сессии
            if (session != null && session!!.has("body")) {
                val keys: Iterator<*> = session!!.getJSONObject("body").keys()
                val b = Uri.parse(url).buildUpon()
                while (keys.hasNext()) {
                    val key = keys.next() as String
                    val `val` = session!!.getJSONObject("body").getString(key)
                    b.appendQueryParameter(key, `val`)
                }
                val uri = b.build()
                url = uri.toString()
            }
            request = builder
                .url(url)
                .build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (!offline) fetch_local("file://error.json")
                    e.printStackTrace()
                }

                @Throws(IOException::class)
                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        if (!offline) fetch_local("file://error.json")
                    } else {
                        val res = response.body!!.string()
                        refs = JSONObject()
                        resolve_and_build(res)
                    }
                }
            })
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    }

    private fun include(res: String) {
        val regex = "\"([+@])\"[ ]*:[ ]*\"(([^\"@]+)(@))?([^\"]+)\""
        val require_pattern = Pattern.compile(regex)
        val matcher = require_pattern.matcher(res)
        val urls = ArrayList<String>()
        while (matcher.find()) {
            //System.out.println("Path: " + matcher.group(3));
            // Fetch URL content and cache
            // Получить содержимое URL и кеш
            val matched = matcher.group(5)
            if (!matched.contains("\$document")) {
                urls.add(matcher.group(5))
            }
        }
        if (urls.size > 0) {
            val latch = CountDownLatch(urls.size)
            val taskExecutor = Executors.newFixedThreadPool(urls.size)
            for (i in urls.indices) {
                taskExecutor.submit(JasonRequire(urls[i], latch, refs, client, view))
            }
            try {
                latch.await()
            } catch (e: Exception) {
                Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
            }
        }
        resolve_reference()
    }

    private fun resolve_and_build(res: String) {
        try {
            jason = JSONObject(res)

            // "include" handling
            // 1. check if it contains "+": "..."
            // 2. if it does, need to resolve it first.
            // 3. if it doesn't, just build the view immediately
            // обработка include
            // 1. проверить, содержит ли оно "+": "..."
            // 2. если это так, сначала нужно разрешить его.
            // 3. если это не так, просто немедленно создайте представление

            // Exclude patterns that start with $ (will be handled by local resolve)
            // Исключить шаблоны, начинающиеся с $ (будут обрабатываться локальным разрешением)
            val regex = "\"([+@])\"[ ]*:[ ]*\"(([^$\"@]+)(@))?([^$\"]+)\""
            val require_pattern = Pattern.compile(regex)
            val matcher = require_pattern.matcher(res)
            if (matcher.find()) {
                // if requires resolution, require first.
                // если требуется разрешение, сначала требуется.
                include(res)
            } else {
                // otherwise, resolve local once and then render (for $document)
                // в противном случае, разрешаем локально один раз, а затем выполняем рендеринг
                val local_regex = "\"([+@])\"[ ]*:[ ]*\"(([^\"@]+)(@))?([^\"]+)\""
                val local_require_pattern = Pattern.compile(local_regex)
                val local_matcher = local_require_pattern.matcher(res)
                if (local_matcher.find()) {
                    resolve_local_reference()
                } else {
                    if (jason!!.has("\$jason")) {
                        view.loaded = false
                        view.build(jason)
                    } else {
                    }
                }
            }
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    }

    private fun resolve_reference() {
        // convert "+": "$document.blah.blah"
        // to "{{#include $root.$document.blah.blah}}": {}
        var str_jason = jason.toString()
        try {

            // Exclude a pattern that starts with $ => will be handled by resolve_local_reference
            // Исключить шаблон, который начинается с $ =>, будет обработано resol_local_reference
            val remote_pattern_with_path_str = "\"([+@])\"[ ]*:[ ]*\"(([^$\"@]+)(@))([^\"]+)\""
            val remote_pattern_with_path = Pattern.compile(remote_pattern_with_path_str)
            val remote_with_path_matcher = remote_pattern_with_path.matcher(str_jason)
            str_jason =
                remote_with_path_matcher.replaceAll("\"{{#include \\$root[\\\\\"$5\\\\\"].$3}}\": {}")

            // Exclude a pattern that starts with $ => will be handled by resolve_local_reference
            // Исключить шаблон, который начинается с $ =>, будет обработано resol_local_reference
            val remote_pattern_without_path_str = "\"([+@])\"[ ]*:[ ]*\"([^$\"]+)\""
            val remote_pattern_without_path = Pattern.compile(remote_pattern_without_path_str)
            val remote_without_path_matcher = remote_pattern_without_path.matcher(str_jason)
            str_jason =
                remote_without_path_matcher.replaceAll("\"{{#include \\$root[\\\\\"$2\\\\\"]}}\": {}")
            val to_resolve = JSONObject(str_jason)
            refs!!.put("\$document", jason)

            // parse
            // разбирать
            JasonParser.Companion.getInstance(view)!!
                .setParserListener(JasonParserListener { resolved_jason ->
                    try {
                        resolve_and_build(resolved_jason.toString())
                    } catch (e: Exception) {
                        Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
                    }
                })
            JasonParser.Companion.getInstance(view)!!.parse("json", refs, to_resolve, view)
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    }

    private fun resolve_local_reference() {
        // convert "+": "$document.blah.blah"
        // to "{{#include $root.$document.blah.blah}}": {}
        var str_jason = jason.toString()
        try {
            val local_pattern_str = "\"[+@]\"[ ]*:[ ]*\"[ ]*(\\$document[^\"]*)\""
            val local_pattern = Pattern.compile(local_pattern_str)
            val local_matcher = local_pattern.matcher(str_jason)
            str_jason = local_matcher.replaceAll("\"{{#include \\$root.$1}}\": {}")
            val to_resolve = JSONObject(str_jason)
            refs!!.put("\$document", jason)

            // parse
            // разбирать
            JasonParser.Companion.getInstance(view)!!
                .setParserListener(JasonParserListener { resolved_jason ->
                    try {
                        resolve_and_build(resolved_jason.toString())
                    } catch (e: Exception) {
                        Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
                    }
                })
            JasonParser.Companion.getInstance(view)!!.parse("json", refs, to_resolve, view)
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    }

    operator fun set(name: String, data: JSONObject) {
        if (name.equals("jason", ignoreCase = true)) {
            jason = data
        } else if (name.equals("state", ignoreCase = true)) {
            try {
                // Construct variable state (var => $get, cache => $cache, params => $params, etc)
                // by default, take the inline data
                // Создаем состояние переменной (var => $ get, cache => $ cache, params => $ params и т. Д.)
                // по умолчанию берем встроенные данные
                state = if (jason!!.getJSONObject("\$jason")
                        .has("head") && jason!!.getJSONObject("\$jason").getJSONObject("head")
                        .has("data")
                ) {
                    jason!!.getJSONObject("\$jason").getJSONObject("head").getJSONObject("data")
                } else {
                    JSONObject()
                }
                if (data is JSONObject) {
                    val keys: Iterator<*> = data.keys()
                    while (keys.hasNext()) {
                        val key = keys.next() as String
                        val `val` = data[key]
                        state!!.put(key, `val`)
                    }
                }

                // merge with passed in data
                // объединить с переданными данными
                state!!.put("\$get", `var`)
                state!!.put("\$cache", cache)
                state!!.put("\$global", (view.applicationContext as Launcher).global)
                state!!.put("\$env", (view.applicationContext as Launcher).env)
                state!!.put("\$params", params)
            } catch (e: Exception) {
                Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
            }
        } else {
        }
    }

    init {
        client = (view.application as Launcher).getHttpClient(0)
        offline = false

        // $params
        params = JSONObject()
        if (intent != null && intent.hasExtra("params")) {
            try {
                params = JSONObject(intent.getStringExtra("params"))
            } catch (e: Exception) {
                Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
            }
        }

        // $get
        `var` = JSONObject()

        // $cache
        cache = JSONObject()
        val cache_pref = view.getSharedPreferences("cache", 0)
        if (cache_pref.contains(url)) {
            val str = cache_pref.getString(url, null)
            try {
                cache = JSONObject(str)
            } catch (e: Exception) {
                Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
            }
        }

        // session
        val session_pref = view.getSharedPreferences("session", 0)
        session = JSONObject()
        try {
            val uri_for_session = URI(url!!.toLowerCase())
            val session_domain = uri_for_session.host
            if (session_pref.contains(session_domain)) {
                val str = session_pref.getString(session_domain, null)
                session = JSONObject(str)
            }
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
        try {
            val v = JSONObject()
            v.put("url", url)
            (view.applicationContext as Launcher).setEnv("view", v)
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    }
}