package site.app4web.app4web.Core

import android.content.Context
import android.net.Uri
import android.util.Log
import okhttp3.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import site.app4web.app4web.Helper.JasonHelper
import java.io.IOException
import java.net.URI
import java.util.concurrent.CountDownLatch

class JasonRequire(
    url: String,
    latch: CountDownLatch,
    refs: JSONObject?,
    client: OkHttpClient?,
    context: Context
) : Runnable {
    val URL: String
    val latch: CountDownLatch
    val context: Context
    val client: OkHttpClient?
    var private_refs: JSONObject?
    override fun run() {
        if (URL.contains("file://")) {
            local()
        } else {
            remote()
        }
    }

    private fun local() {
        try {
            val r = Runnable {
                val json = JasonHelper.read_json(URL, context)
                try {
                    private_refs!!.put(URL, json)
                } catch (e: Exception) {
                    Log.d(
                        "Warning",
                        e.stackTrace[0].methodName + " : " + e.toString()
                    )
                }
                latch.countDown()
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

    private fun remote() {
        val request: Request
        val builder = Request.Builder()

        // Session Handling
        // Обработка сессии
        try {
            val pref = context.getSharedPreferences("session", 0)
            var session: JSONObject? = null
            val uri_for_session = URI(URL)
            val session_domain = uri_for_session.host
            if (pref.contains(session_domain)) {
                val str = pref.getString(session_domain, null)
                session = JSONObject(str)
            }

            // session.header
            // заголовок сессии
            if (session != null && session.has("header")) {
                val keys: Iterator<*> = session.getJSONObject("header").keys()
                while (keys.hasNext()) {
                    val key = keys.next() as String
                    val `val` = session.getJSONObject("header").getString(key)
                    builder.addHeader(key, `val`)
                }
            }

            // session.body
            // тело сеанса
            val b = Uri.parse(URL).buildUpon()
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
            val uri = b.build()
            val url_with_session = uri.toString()
            request = builder
                .url(url_with_session)
                .build()


            // Actual call
            // Фактический звонок
            client!!.newCall(request).enqueue(object : Callback {
                override fun onFailure(
                    call: Call,
                    e: IOException
                ) {
                    latch.countDown()
                    e.printStackTrace()
                }

                @Throws(IOException::class)
                override fun onResponse(
                    call: Call,
                    response: Response
                ) {
                    if (!response.isSuccessful) {
                        latch.countDown()
                        throw IOException("Unexpected code $response")
                    }
                    try {
                        val res = response.body!!.string()
                        // store the res under
                        // сохранить результат под
                        if (res.trim { it <= ' ' }.startsWith("[")) {
                            // array
                            // массив
                            private_refs!!.put(URL, JSONArray(res))
                        } else if (res.trim { it <= ' ' }.startsWith("{")) {
                            // object
                            // объект
                            private_refs!!.put(URL, JSONObject(res))
                        } else {
                            // string
                            // строка
                            private_refs!!.put(URL, res)
                        }
                        latch.countDown()
                    } catch (e: JSONException) {
                        Log.d(
                            "Warning",
                            e.stackTrace[0].methodName + " : " + e.toString()
                        )
                    }
                }
            })
        } catch (e: Exception) {
            Log.d(
                "Warning",
                e.stackTrace[0].methodName + " : " + e.toString()
            )
        }
    }

    init {
        URL = url.replace("\\", "")
        this.latch = latch
        private_refs = refs
        this.context = context
        this.client = client
    }
}