package site.app4web.app4web.Action

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import okhttp3.*
import org.json.JSONObject
import site.app4web.app4web.Helper.JasonHelper
import site.app4web.app4web.Helper.JasonHelper.callback
import site.app4web.app4web.Helper.JasonHelper.next
import site.app4web.app4web.Launcher.Launcher
import timber.log.Timber
import java.io.IOException
import java.net.URI
import java.util.*


class JasonNetworkAction {
    private fun _request(
        callback: JSONObject?,
        action: JSONObject,
        data: JSONObject,
        event: JSONObject,
        context: Context
    ) {
        try {
            val options = action.getJSONObject("options")
            if (options.has("url")) {
                var url = options.getString("url")

                // method
                // метод
                var method = "GET"
                if (options.has("method")) {
                    method = options.getString("method").toUpperCase()
                }

                // Attach session if it exists
                // Присоединить сессию, если она существует
                val pref = context.getSharedPreferences("session", 0)
                var session: JSONObject? = null
                val uri_for_session = URI(url.toLowerCase())
                val session_domain = uri_for_session.host
                if (pref.contains(session_domain)) {
                    val str = pref.getString(session_domain, null)
                    session = JSONObject(str)
                }
                val request: Request
                val builder = Request.Builder()
                // Attach Header from Session
                // Прикрепить заголовок из сессии
                if (session != null && session.has("header")) {
                    val keys: Iterator<*> = session.getJSONObject("header").keys()
                    while (keys.hasNext()) {
                        val key = keys.next() as String
                        val `val` = session.getJSONObject("header").getString(key)
                        builder.addHeader(key, `val`)
                    }
                }

                // Attach header passed in as options
                // Прикрепить заголовок, переданный в качестве параметров
                if (options.has("header")) {
                    val header = options.getJSONObject("header")
                    val keys = header.keys()
                    try {
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val `val` = header.getString(key)
                            builder.addHeader(key, `val`)
                        }
                    } catch (e: Exception) {
                    }
                }
                if (method.equals("get", ignoreCase = true)) {
                    val b = Uri.parse(url).buildUpon()

                    // Attach Params from Session
                    // Прикрепить параметры из сессии
                    if (session != null && session.has("body")) {
                        val keys: Iterator<*> = session.getJSONObject("body").keys()
                        while (keys.hasNext()) {
                            val key = keys.next() as String
                            val `val` = session.getJSONObject("body").getString(key)
                            b.appendQueryParameter(key, `val`)
                        }
                    }

                    // params
                    // params
                    if (options.has("data")) {
                        val d = options.getJSONObject("data")
                        val keysIterator = d.keys()
                        try {
                            while (keysIterator.hasNext()) {
                                val key = keysIterator.next()
                                val `val` = d.getString(key)
                                b.appendQueryParameter(key, `val`)
                            }
                        } catch (e: Exception) {
                            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
                        }
                    }
                    val uri = b.build()
                    url = uri.toString()
                    request = builder
                        .url(url)
                        .build()
                } else {
                    if (options.has("header") && options.getJSONObject("header")
                            .has("content_type")
                    ) {
                        val content_type = options.getJSONObject("header").getString("content_type")
                        val mediaType: MediaType
                        val d: ByteArray
                        if (content_type.equals("json", ignoreCase = true)) {
                            mediaType = MediaType.parse("application/json; charset=utf-8")
                            d = options.getString("data").toByteArray()
                        } else {
                            mediaType = MediaType.parse(content_type)
                            d = Base64.decode(options.getString("data"), Base64.DEFAULT)
                        }
                        val requestBuilder = Request.Builder()
                        request = requestBuilder
                            .url(url)
                            .method(method, RequestBody.create(mediaType, d))
                            .build()
                    } else {
                        // Params
                        // params
                        val bodyBuilder = FormBody.Builder()
                        if (options.has("data")) {
                            // default json
                            // по умолчанию json
                            val d = options.getJSONObject("data")
                            val keysIterator = d.keys()
                            try {
                                while (keysIterator.hasNext()) {
                                    val key = keysIterator.next()
                                    val `val` = d.getString(key)
                                    bodyBuilder.add(key, `val`)
                                }
                            } catch (e: Exception) {
                                Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
                            }
                        }
                        // Attach Params from Session
                        // Прикрепить параметры из сессии
                        if (session != null && session.has("body")) {
                            val keys: Iterator<*> = session.getJSONObject("body").keys()
                            while (keys.hasNext()) {
                                val key = keys.next() as String
                                val `val` = session.getJSONObject("body").getString(key)
                                bodyBuilder.add(key, `val`)
                            }
                        }
                        val requestBody: RequestBody = bodyBuilder.build()
                        request = builder
                            .method(method, requestBody)
                            .url(url)
                            .build()
                    }
                }
                val client: OkHttpClient
                client = if (options.has("timeout")) {
                    val timeout = options["timeout"]
                    if (timeout is Long) {
                        (context.applicationContext as Launcher).getHttpClient(timeout)
                    } else if (timeout is String) {
                        val timeout_int: Long = (timeout as String?). toLong ()
                        (context.applicationContext as Launcher).getHttpClient(timeout_int)
                    } else {
                        (context.applicationContext as Launcher).getHttpClient(0)
                    }
                } else {
                    (context.applicationContext as Launcher).getHttpClient(0)
                }
                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Timber.e(e)
                        try {
                            if (action.has("error")) {
                                val error = JSONObject()
                                error.put("data", e.toString())
                                if (callback != null) {
                                    callback(callback, null, context)
                                } else {
                                    next("error", action, error, event, context)
                                }
                            }
                        } catch (e2: java.lang.Exception) {
                            Log.d("Warning", e2.stackTrace[0].methodName + " : " + e2.toString())
                        }
                    }

                    @Throws(IOException::class)
                    override fun onResponse(call: Call, response: Response) {
                        if (!response.isSuccessful) {
                            try {
                                if (action.has("error")) {
                                    val error = JSONObject()
                                    error.put("data", response.toString())
                                    if (callback != null) {
                                        callback(callback, null, context)
                                    } else {
                                        next("error", action, error, event, context)
                                    }
                                }
                            } catch (e: java.lang.Exception) {
                                Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
                            }
                        } else {
                            try {
                                val jsonData = response.body!!.string()
                                if (callback != null) {
                                    callback(callback, jsonData, context)
                                } else {
                                    next("success", action, jsonData, event, context)
                                }
                            } catch (e: java.lang.Exception) {
                                Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
                            }
                        }
                    }
                })
            }
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    }

    fun request(action: JSONObject, data: JSONObject, event: JSONObject, context: Context) {
        _request(null, action, data, event, context)
    }

    fun upload(action: JSONObject, data: JSONObject, event: JSONObject, context: Context) {
        try {
            val options = action.getJSONObject("options")
            if (options.has("data")) {
                var stack = JSONObject()
                stack.put("class", "JasonNetworkAction")
                stack.put("method", "process")
                stack = JasonHelper.preserve(stack, action, data, event, context)
                val params = JSONObject()
                params.put("bucket", options.getString("bucket"))
                val uniqueId = UUID.randomUUID().toString()
                if (options.has("path")) {
                    params.put("path", options.getString("path") + "/" + uniqueId)
                } else {
                    params.put("path", "/$uniqueId")
                }
                stack.put("filename", params.getString("path"))
                params.put("content-type", options.getString("content_type"))
                val new_options = JSONObject()
                new_options.put("url", options.getString("sign_url"))
                new_options.put("data", params)
                val upload_action = JSONObject()
                upload_action.put("options", new_options)
                _request(stack, upload_action, data, event, context)
            }
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    }

    // util
    // утилита
    fun process(stack: JSONObject, result: String?) {
        try {
            if (result != null) {
                val o = stack.getJSONObject("options")
                val action = o.getJSONObject("action")
                val data = o.getJSONObject("data")
                val event = o.getJSONObject("event")
                val context = o["context"] as Context
                val options = action.getJSONObject("options")
                val signed_url_object = JSONObject(result)
                val signed_url =
                    signed_url_object.getString("\$jason") // must return a signed_url wrapped with $jason
                stack.put("class", "JasonNetworkAction")
                stack.put("method", "uploadfinished")
                val header = JSONObject()
                header.put("content_type", options.getString("content_type"))
                val new_options = JSONObject()
                new_options.put("url", signed_url)
                new_options.put("data", options["data"])
                new_options.put("method", "put")
                new_options.put("header", header)
                action.put("options", new_options)
                _request(stack, action, data, event, context)
            }
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    }

    fun uploadfinished(stack: JSONObject, result: String?) {
        try {
            val ret = JSONObject()
            ret.put("filename", stack.getString("filename"))
            ret.put("file_name", stack.getString("filename"))
            val o = stack.getJSONObject("options")
            val action = o.getJSONObject("action")
            val event = o.getJSONObject("event")
            val context = o["context"] as Context
            JasonHelper.next("success", action, ret, event, context)
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    }
}