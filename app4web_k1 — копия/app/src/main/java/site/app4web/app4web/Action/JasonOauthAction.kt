package site.app4web.app4web.Action

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.util.Log
import com.github.scribejava.core.builder.ServiceBuilder
import com.github.scribejava.core.builder.api.DefaultApi10a
import com.github.scribejava.core.builder.api.DefaultApi20
import com.github.scribejava.core.model.*
import okhttp3.*
import okhttp3.Credentials.basic
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import site.app4web.app4web.Helper.JasonHelper
import site.app4web.app4web.Launcher.Launcher
import java.io.IOException
import java.nio.charset.Charset
import java.util.*

class JasonOauthAction {
    @SuppressLint("StaticFieldLeak")
    fun auth(
        action: JSONObject,
        data: JSONObject?,
        event: JSONObject?,
        context: Context
    ) {
        try {
            val options = action.getJSONObject("options")
            if (options.has("version") && options.getString("version") == "1") {
                //
                //OAuth 1
                //
                val request_options = options.getJSONObject("request")
                val authorize_options = options.getJSONObject("authorize")
                val client_id = request_options.getString("client_id")
                val client_secret = request_options.getString("client_secret")
                if (!request_options.has("scheme") || request_options.getString("scheme").length == 0 || !request_options.has(
                        "host"
                    ) || request_options.getString("host").length == 0 || !request_options.has("path") || request_options.getString(
                        "path"
                    ).length == 0 || !authorize_options.has("scheme") || authorize_options.getString(
                        "scheme"
                    ).length == 0 || !authorize_options.has("host") || authorize_options.getString("host").length == 0 || !authorize_options.has(
                        "path"
                    ) || authorize_options.getString("path").length == 0
                ) {
                    JasonHelper.next("error", action, data, event, context)
                } else {
                    val request_options_data = request_options.getJSONObject("data")
                    val uriBuilder = Uri.Builder()
                    uriBuilder.scheme(request_options.getString("scheme"))
                        .encodedAuthority(request_options.getString("host"))
                        .encodedPath(request_options.getString("path"))
                    val requestUri = uriBuilder.build().toString()
                    val authorizeUriBuilder = Uri.Builder()
                    authorizeUriBuilder.scheme(authorize_options.getString("scheme"))
                        .encodedAuthority(authorize_options.getString("host"))
                        .encodedPath(authorize_options.getString("path"))
                    val callback_uri = request_options_data.getString("oauth_callback")
                    val oauthApi: DefaultApi10a = object : DefaultApi10a() {
                        override fun getRequestTokenEndpoint(): String? {
                            return requestUri
                        }

                        override fun getAccessTokenEndpoint(): String? {
                            return null
                        }

                        // @Override  // Добавлено для версии >4.0
                        protected val authorizationBaseUrl: String?
                            get() = null

                        override fun getAuthorizationUrl(requestToken: OAuth1RequestToken): String {
                            return authorizeUriBuilder
                                .appendQueryParameter("oauth_token", requestToken.token)
                                .build().toString()
                        }
                    }
                    val oauthService = ServiceBuilder()
                        .apiKey(client_id)
                        .apiSecret(client_secret)
                        .callback(callback_uri)
                        .build(oauthApi)
                    object : AsyncTask<String?, Void?, Void?>() {
                        protected override fun doInBackground(vararg params: String?): Void? {
                            try {
                                val client_id = params[0]
                                val request_token =
                                    oauthService.requestToken
                                val preferences = context.getSharedPreferences(
                                    "oauth",
                                    Context.MODE_PRIVATE
                                )
                                preferences.edit().putString(
                                    client_id + "_request_token_secret",
                                    request_token.tokenSecret
                                ).apply()
                                val auth_url =
                                    oauthService.getAuthorizationUrl(request_token)
                                val intent = Intent(Intent.ACTION_VIEW)
                                intent.data = Uri.parse(auth_url)
                                val callback = JSONObject()
                                callback.put("class", "JasonOauthAction")
                                callback.put("method", "oauth_callback")
                                JasonHelper.dispatchIntent(
                                    "oauth",
                                    action,
                                    data,
                                    event,
                                    context,
                                    intent,
                                    callback
                                )
                            } catch (e: Exception) {
                                handleError(e, action, event, context)
                            }
                            return null
                        }
                    }.execute(client_id)
                }
            } else {
                //
                //OAuth 2
                //
                val authorize_options = options.getJSONObject("authorize")
                var authorize_options_data = JSONObject()
                if (authorize_options.has("data")) {
                    authorize_options_data = authorize_options.getJSONObject("data")
                }
                if (authorize_options_data.has("grant_type") && authorize_options_data.getString("grant_type") == "password") {
                    val client_id = authorize_options.getString("client_id")
                    var client_secret = ""
                    if (authorize_options.has("client_secret")) {
                        client_secret = authorize_options.getString("client_secret")
                    }
                    if (!authorize_options.has("scheme") || authorize_options.getString("scheme").length == 0 || !authorize_options.has(
                            "host"
                        ) || authorize_options.getString(
                            "host"
                        ).length == 0 || !authorize_options.has("path") || authorize_options.getString(
                            "path"
                        ).length == 0 || !authorize_options_data.has("username") || authorize_options_data.getString(
                            "username"
                        ).length == 0 || !authorize_options_data.has("password") || authorize_options_data.getString(
                            "password"
                        ).length == 0
                    ) {
                        JasonHelper.next("error", action, data, event, context)
                    } else {
                        val username = authorize_options_data.getString("username")
                        val password = authorize_options_data.getString("password")
                        val builder = Uri.Builder()
                        builder.scheme(authorize_options.getString("scheme"))
                            .encodedAuthority(authorize_options.getString("host"))
                            .encodedPath(authorize_options.getString("path"))
                        val uri = builder.build()
                        val oauthApi: DefaultApi20 = object : DefaultApi20() {
                            override fun getAccessTokenEndpoint(): String {
                                return uri.toString()
                            }

                            override fun getAuthorizationBaseUrl(): String? {
                                return null
                            }
                        }
                        val serviceBuilder = ServiceBuilder()
                        serviceBuilder.apiKey(client_id)
                        if (client_secret.length > 0) {
                            serviceBuilder.apiSecret(client_secret)
                        }
                        if (authorize_options_data.has("scope") && authorize_options_data.getString(
                                "scope"
                            ).length > 0
                        ) {
                            serviceBuilder.scope(authorize_options_data.getString("scope"))
                        }
                        if (authorize_options_data.has("state") && authorize_options_data.getString(
                                "state"
                            ).length > 0
                        ) {
                            serviceBuilder.state(authorize_options_data.getString("state"))
                        }
                        val oauthService =
                            serviceBuilder.build(oauthApi)
                        val additionalParams: MutableMap<String, String> =
                            HashMap()
                        val paramKeys: Iterator<*> =
                            authorize_options_data.keys()
                        while (paramKeys.hasNext()) {
                            val key = paramKeys.next() as String
                            if (key !== "redirect_uri" && key !== "response_type" && key !== "scope" && key !== "state") {
                                val value = authorize_options_data.getString(key)
                                additionalParams[key] = value
                            }
                        }
                        object : AsyncTask<String?, Void?, Void?>() {
                            protected override fun doInBackground(vararg params: String?): Void? {
                                try {
                                    val username = params[0]
                                    val password = params[1]
                                    val client_id = params[2]
                                    val access_token =
                                        oauthService.getAccessTokenPasswordGrant(username, password)
                                            .accessToken
                                    val preferences =
                                        context.getSharedPreferences(
                                            "oauth",
                                            Context.MODE_PRIVATE
                                        )
                                    preferences.edit().putString(client_id, access_token).apply()
                                    val result = JSONObject()
                                    try {
                                        result.put("token", access_token)
                                        JasonHelper.next("success", action, result, event, context)
                                    } catch (e: JSONException) {
                                        handleError(e, action, event, context)
                                    }
                                } catch (e: Exception) {
                                    handleError(e, action, event, context)
                                }
                                return null
                            }
                        }.execute(username, password, client_id)
                    }
                } else {
                    //
                    //Assuming code auth
                    // Предполагается, что код аутентификации
                    //
                    val client_id = authorize_options.getString("client_id")
                    val sharedPreferences =
                        context.getSharedPreferences("oauth", Context.MODE_PRIVATE)
                    if (sharedPreferences.contains(client_id) &&
                        sharedPreferences.contains(client_id + "_refresh_token") &&
                        (sharedPreferences.contains(client_id + "_expires_in") &&
                                sharedPreferences.getInt(
                                    client_id + "_created_at",
                                    0
                                ) + sharedPreferences.getInt(client_id + "_expires_in", 0)
                                <
                                (System.currentTimeMillis() / 1000).toInt())
                    ) {
                        request_oauth20_access_token(
                            action,
                            data,
                            event,
                            context,
                            null,
                            sharedPreferences.getString(client_id + "_refresh_token", null)
                        )
                    } else {
                        if (authorize_options.has("data")) {
                            authorize_options_data = authorize_options.getJSONObject("data")
                            if (authorize_options.length() == 0) {
                                JasonHelper.next("error", action, data, event, context)
                            } else {
                                var client_secret = ""
                                var redirect_uri: String? = ""

                                //Secret can be missing in implicit authentication
                                if (authorize_options.has("client_secret")) {
                                    client_secret = authorize_options.getString("client_secret")
                                }
                                if (authorize_options_data.has("redirect_uri")) {
                                    redirect_uri = authorize_options_data.getString("redirect_uri")
                                }
                                if (!authorize_options.has("scheme") || authorize_options.getString(
                                        "scheme"
                                    ).length == 0 || !authorize_options.has("host") || authorize_options.getString(
                                        "host"
                                    ).length == 0 || !authorize_options.has("path") || authorize_options.getString(
                                        "path"
                                    ).length == 0
                                ) {
                                    JasonHelper.next("error", action, data, event, context)
                                } else {
                                    val builder = Uri.Builder()
                                    builder.scheme(authorize_options.getString("scheme"))
                                        .encodedAuthority(authorize_options.getString("host"))
                                        .encodedPath(authorize_options.getString("path"))
                                    val uri = builder.build()
                                    val oauthApi: DefaultApi20 = object : DefaultApi20() {
                                        override fun getAccessTokenEndpoint(): String? {
                                            return null
                                        }

                                        override fun getAuthorizationBaseUrl(): String {
                                            return uri.toString()
                                        }
                                    }
                                    val serviceBuilder = ServiceBuilder()
                                    serviceBuilder.apiKey(client_id)
                                    if (client_secret.length > 0) {
                                        serviceBuilder.apiSecret(client_secret)
                                    }
                                    if (authorize_options_data.has("scope") && authorize_options_data.getString(
                                            "scope"
                                        ).length > 0
                                    ) {
                                        serviceBuilder.scope(authorize_options_data.getString("scope"))
                                    }
                                    if (authorize_options_data.has("state") && authorize_options_data.getString(
                                            "state"
                                        ).length > 0
                                    ) {
                                        serviceBuilder.state(authorize_options_data.getString("state"))
                                    }
                                    serviceBuilder.callback(redirect_uri)
                                    val oauthService =
                                        serviceBuilder.build(oauthApi)
                                    val additionalParams: MutableMap<String, String> =
                                        HashMap()
                                    val paramKeys: Iterator<*> =
                                        authorize_options_data.keys()
                                    while (paramKeys.hasNext()) {
                                        val key = paramKeys.next() as String
                                        if (key !== "redirect_uri" && key !== "response_type" && key !== "scope" && key !== "state") {
                                            val value =
                                                authorize_options_data.getString(key)
                                            additionalParams[key] = value
                                        }
                                    }
                                    val intent = Intent(Intent.ACTION_VIEW)
                                    intent.data = Uri.parse(
                                        oauthService.getAuthorizationUrl(
                                            additionalParams
                                        )
                                    )
                                    val callback = JSONObject()
                                    callback.put("class", "JasonOauthAction")
                                    callback.put("method", "oauth_callback")
                                    JasonHelper.dispatchIntent(
                                        "oauth",
                                        action,
                                        data,
                                        event,
                                        context,
                                        intent,
                                        callback
                                    )
                                }
                            }
                        } else {
                            val error = JSONObject()
                            error.put("data", "Authorize data missing")
                            JasonHelper.next("error", action, error, event, context)
                        }
                    }
                }
            }
        } catch (e: JSONException) {
            handleError(e, action, event, context)
        }
    }

    fun access_token(
        action: JSONObject,
        data: JSONObject?,
        event: JSONObject?,
        context: Context
    ) {
        try {
            val options = action.getJSONObject("options")
            val client_id = options.getJSONObject("access").getString("client_id")
            val sharedPreferences =
                context.getSharedPreferences("oauth", Context.MODE_PRIVATE)
            val access_token = sharedPreferences.getString(client_id, null)
            if (access_token != null) {
                val result = JSONObject()
                result.put("token", access_token)
                JasonHelper.next("success", action, result, event, context)
            } else {
                val error = JSONObject()
                error.put("data", "access token not found")
                JasonHelper.next("error", action, error, event, context)
            }
        } catch (e: JSONException) {
            handleError(e, action, event, context)
        }
    }

    fun oauth_callback(intent: Intent, intent_options: JSONObject) {
        try {
            val action = intent_options.getJSONObject("action")
            val data = intent_options.getJSONObject("data")
            val event = intent_options.getJSONObject("event")
            val context =
                intent_options["context"] as Context
            val options = action.getJSONObject("options")
            val uri = intent.data
            if (options.has("version") && options.getString("version") == "1") {
                //OAuth 1
                val oauth_token = uri!!.getQueryParameter("oauth_token")
                val oauth_verifier = uri.getQueryParameter("oauth_verifier")
                val access_options = options.getJSONObject("access")
                if (oauth_token!!.length > 0 && oauth_verifier!!.length > 0 && access_options.has("scheme") && access_options.getString(
                        "scheme"
                    ).length > 0 && access_options.has("host") && access_options.getString("host").length > 0 && access_options.has(
                        "path"
                    ) && access_options.getString("path").length > 0 && access_options.has("path") && access_options.getString(
                        "path"
                    ).length > 0 && access_options.has("client_id") && access_options.getString("client_id").length > 0 && access_options.has(
                        "client_secret"
                    ) && access_options.getString("client_secret").length > 0
                ) {
                    val client_id = access_options.getString("client_id")
                    val client_secret = access_options.getString("client_secret")
                    val uriBuilder = Uri.Builder()
                    uriBuilder.scheme(access_options.getString("scheme"))
                        .encodedAuthority(access_options.getString("host"))
                        .encodedPath(access_options.getString("path"))
                    val accessUri = uriBuilder.build().toString()
                    val oauthApi: DefaultApi10a = object : DefaultApi10a() {
                        override fun getAuthorizationUrl(requestToken: OAuth1RequestToken): String? {
                            return null
                        }

                        override fun getRequestTokenEndpoint(): String? {
                            return null
                        }

                        override fun getAccessTokenEndpoint(): String? {
                            return accessUri
                        }

                        // @Override  // добавленно для перехода на версию >4.0
                        protected val authorizationBaseUrl: String?
                             get() = null
                    }
                    val oauthService = ServiceBuilder()
                        .apiKey(client_id)
                        .apiSecret(client_secret)
                        .build(oauthApi)
                    object : AsyncTask<String?, Void?, Void?>() {
                        protected override fun doInBackground(vararg params: String?): Void? {
                            try {
                                val preferences = context.getSharedPreferences(
                                    "oauth",
                                    Context.MODE_PRIVATE
                                )
                                val string_oauth_token = params[0]
                                val oauth_verifier = params[1]
                                val client_id = params[2]
                                val oauth_token_secret =
                                    preferences.getString(client_id + "_request_token_secret", null)
                                val oauthToken =
                                    OAuth1RequestToken(string_oauth_token, oauth_token_secret)
                                val access_token =
                                    oauthService.getAccessToken(oauthToken, oauth_verifier)
                                preferences.edit().putString(client_id, access_token.token)
                                    .apply()
                                preferences.edit().putString(
                                    client_id + "_access_token_secret",
                                    access_token.tokenSecret
                                ).apply()
                                val result = JSONObject()
                                result.put("token", access_token.token)
                                JasonHelper.next("success", action, result, event, context)
                            } catch (e: Exception) {
                                handleError(e, action, event, context)
                            }
                            return null
                        }
                    }.execute(oauth_token, oauth_verifier, client_id)
                } else {
                    JasonHelper.next("error", action, data, event, context)
                }
            } else {
                // OAuth 2
                val access_token =
                    uri!!.getQueryParameter("access_token") // get access token from url here
                val authorize_options = options.getJSONObject("authorize")
                if (access_token != null && access_token.length > 0) {
                    val client_id = authorize_options.getString("client_id")
                    val preferences =
                        context.getSharedPreferences("oauth", Context.MODE_PRIVATE)
                    preferences.edit().putString(client_id, access_token).apply()
                    val result = JSONObject()
                    result.put("token", access_token)
                    JasonHelper.next("success", action, result, event, context)
                } else {
                    val code = uri.getQueryParameter("code")
                    request_oauth20_access_token(action, data, event, context, code, null)
                }
            }
        } catch (err: JSONException) {
            try {
                val error = JSONObject()
                error.put("data", err.toString())
                JasonHelper.next(
                    "error",
                    intent_options.getJSONObject("action"),
                    error,
                    intent_options.getJSONObject("event"),
                    intent_options["context"] as Context
                )
            } catch (e: JSONException) {
                Log.d(
                    "Warning",
                    e.stackTrace[0].methodName + " : " + e.toString()
                )
            }
        }
    }

    private fun request_oauth20_access_token(
        action: JSONObject,
        data: JSONObject?,
        event: JSONObject?,
        context: Context,
        code: String?,
        refresh_token: String?
    ) {
        try {
            val options = action.getJSONObject("options")
            val access_options = options.getJSONObject("access")
            val access_options_data =
                if (access_options.has("data")) access_options.getJSONObject("data") else JSONObject()
            val client_id = access_options.getString("client_id")
            //
            // also in access_options_data
            //
            val client_secret =
                if (access_options.has("client_secret")) access_options.getString("client_secret") else ""
            val redirect_uri =
                if (access_options_data.has("redirect_uri")) access_options_data.getString("redirect_uri") else ""
            val grant_type: String
            grant_type = if (refresh_token != null) {
                "refresh_token"
            } else {
                if (access_options_data.has("grant_type")) access_options_data.getString("grant_type") else ""
            }
            if (access_options.length() == 0 || !access_options.has("scheme") || access_options.getString(
                    "scheme"
                ).length == 0 || !access_options.has("host") || access_options.getString("host").length == 0 || !access_options.has(
                    "path"
                ) || access_options.getString("path").length == 0
            ) {
                JasonHelper.next("error", action, data, event, context)
            } else {
                val uri_builder = Uri.Builder()
                uri_builder.scheme(access_options.getString("scheme"))
                    .authority(access_options.getString("host"))
                    .appendEncodedPath(access_options.getString("path"))
                if (redirect_uri !== "") {
                    uri_builder.appendQueryParameter("redirect_uri", redirect_uri)
                }
                if (grant_type !== "") {
                    uri_builder.appendQueryParameter("grant_type", grant_type)
                }
                if (code != null) {
                    uri_builder.appendQueryParameter("code", code)
                }
                if (refresh_token != null) {
                    uri_builder.appendQueryParameter("refresh_token", refresh_token)
                }
                var client: OkHttpClient?
                client = if (access_options.has("basic") && access_options.getBoolean("basic")) {
                    val b = OkHttpClient.Builder()
                    b.authenticator(object : Authenticator {
                        @Throws(IOException::class)
                        override fun authenticate(
                            route: Route?,
                            response: Response
                        ): Request? {
                            if (response.request.header("Authorization") != null) {
                                return null
                            }
                            val credential = basic(client_id, client_secret)
                            return response.request.newBuilder().header("Authorization", credential)
                                .build()
                        }
                    })
                    b.build()
                } else {
                    uri_builder.appendQueryParameter("client_id", client_id)
                    uri_builder.appendQueryParameter("client_secret", client_secret)
                    (context.applicationContext as Launcher).getHttpClient(0)
                }
                val request: Request
                val requestBuilder = Request.Builder()
                    .url(uri_builder.build().toString())
                    .method("POST", RequestBody.create(null, ByteArray(0)))
                request = requestBuilder.build()
                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        handleError(e, action, event, context)
                    }

                    @Throws(IOException::class)
                    override fun onResponse(
                        call: Call,
                        response: Response
                    ) {
                        try {
                            val jsonResponse = JSONObject(
                                response.body!!.source().readString(Charset.defaultCharset())
                            )
                            val access_token = jsonResponse.getString("access_token")
                            val refresh_token =
                                if (jsonResponse.has("refresh_token")) jsonResponse.getString("refresh_token") else ""
                            val expires_in =
                                if (jsonResponse.has("expires_in")) jsonResponse.getInt("expires_in") else -1
                            val preferences = context.getSharedPreferences(
                                "oauth",
                                Context.MODE_PRIVATE
                            )
                            preferences.edit().putString(client_id, access_token).apply()
                            preferences.edit().putInt(
                                client_id + "_created_at",
                                (System.currentTimeMillis() / 1000).toInt()
                            ).apply()
                            preferences.edit().putInt(client_id + "_expires_in", expires_in).apply()
                            if (refresh_token.length > 0) {
                                preferences.edit()
                                    .putString(client_id + "_refresh_token", refresh_token).apply()
                            }
                            val result = JSONObject()
                            result.put("token", access_token)
                            JasonHelper.next("success", action, result, event, context)
                        } catch (e: JSONException) {
                            handleError(e, action, event, context)
                        }
                    }
                })
            }
        } catch (e: JSONException) {
            handleError(e, action, event, context)
        }
    }

    fun reset(
        action: JSONObject,
        data: JSONObject?,
        event: JSONObject?,
        context: Context
    ) {
        try {
            val options = action.getJSONObject("options")
            val client_id = options.getString("client_id")
            if (options.has("version") && options.getString("version") == "1") {
                val preferences =
                    context.getSharedPreferences("oauth", Context.MODE_PRIVATE)
                preferences.edit().remove(client_id).apply()
                if (preferences.contains(client_id + "_request_token_secret")) {
                    preferences.edit().remove(client_id + "_request_token_secret")
                }
                if (preferences.contains(client_id + "_access_token_secret")) {
                    preferences.edit().remove(client_id + "_access_token_secret")
                }
                JasonHelper.next("success", action, data, event, context)
            } else {
                val preferences =
                    context.getSharedPreferences("oauth", Context.MODE_PRIVATE)
                preferences.edit().remove(client_id).apply()
                if (preferences.contains(client_id + "_refresh_token")) {
                    preferences.edit().remove(client_id + "_refresh_token")
                }
                if (preferences.contains(client_id + "_expires_in")) {
                    preferences.edit().remove(client_id + "_expires_in")
                }
                if (preferences.contains(client_id + "_created_at")) {
                    preferences.edit().remove(client_id + "_created_at")
                }
                JasonHelper.next("success", action, data, event, context)
            }
        } catch (e: JSONException) {
            handleError(e, action, event, context)
        }
    }

    fun request(
        action: JSONObject,
        data: JSONObject?,
        event: JSONObject?,
        context: Context
    ) {
        try {
            val options = action.getJSONObject("options")
            val client_id = options.getString("client_id")
            var client_secret = ""
            if (options.has("client_secret") && options.getString("client_secret").length > 0) {
                client_secret = options.getString("client_secret")
            }
            val sharedPreferences =
                context.getSharedPreferences("oauth", Context.MODE_PRIVATE)
            val access_token = sharedPreferences.getString(client_id, null)
            val path = options.getString("path")
            val scheme = options.getString("scheme")
            val host = options.getString("host")
            val method: String
            method = if (options.has("method")) {
                options.getString("method")
            } else {
                "GET"
            }
            if (access_token != null && access_token.length > 0) {
                var params = JSONObject()
                if (options.has("data")) {
                    params = options.getJSONObject("data")
                }
                var headers = JSONObject()
                if (options.has("headers")) {
                    headers = options.getJSONObject("headers")
                }
                val uriBuilder = Uri.Builder()
                uriBuilder.scheme(scheme)
                uriBuilder.encodedAuthority(host)
                uriBuilder.path(path)
                val uri = uriBuilder.build()
                val url = uri.toString()
                val request = OAuthRequest(Verb.valueOf(method), url)
                val paramKeys: Iterator<*> = params.keys()
                while (paramKeys.hasNext()) {
                    val key = paramKeys.next() as String
                    val value = params.getString(key)
                    request.addParameter(key, value)
                }
                val headerKeys: Iterator<*> = headers.keys()
                while (headerKeys.hasNext()) {
                    val key = headerKeys.next() as String
                    val value = headers.getString(key)
                    request.addHeader(key, value)
                }
                if (options.has("version") && options.getString("version") == "1") {
                    val oauthApi: DefaultApi10a = object : DefaultApi10a() {
                        override fun getRequestTokenEndpoint(): String? {
                            return null
                        }

                        override fun getAccessTokenEndpoint(): String? {
                            return null
                        }

                        // @Override // Добавлено для перехода на версию > 4.0
                        protected val authorizationBaseUrl: String?
                             get() = null

                        override fun getAuthorizationUrl(requestToken: OAuth1RequestToken): String? {
                            return null
                        }
                    }
                    val serviceBuilder = ServiceBuilder()
                    serviceBuilder.apiKey(client_id)
                    if (client_secret.length > 0) {
                        serviceBuilder.apiSecret(client_secret)
                    }
                    val oauthService =
                        serviceBuilder.build(oauthApi)
                    val access_token_secret =
                        sharedPreferences.getString(client_id + "_access_token_secret", null)
                    oauthService.signRequest(
                        OAuth1AccessToken(access_token, access_token_secret),
                        request
                    )
                    object : AsyncTask<Void?, Void?, Void?>() {
                        protected override fun doInBackground(vararg voids: Void?): Void? {
                            try {
                                val response =
                                    oauthService.execute(request)
                                if (response.code == 200) {
                                    JasonHelper.next(
                                        "success",
                                        action,
                                        response.body,
                                        event,
                                        context
                                    )
                                } else {
                                    JasonHelper.next(
                                        "error",
                                        action,
                                        response.body,
                                        event,
                                        context
                                    )
                                }
                            } catch (e: Exception) {
                                handleError(e, action, event, context)
                            }
                            return null
                        }
                    }.execute()
                } else {
                    val oauthApi: DefaultApi20 = object : DefaultApi20() {
                        override fun getAccessTokenEndpoint(): String? {
                            return null
                        }

                        override fun getAuthorizationBaseUrl(): String? {
                            return null
                        }
                    }
                    val serviceBuilder = ServiceBuilder()
                    serviceBuilder.apiKey(client_id)
                    if (client_secret.length > 0) {
                        serviceBuilder.apiSecret(client_secret)
                    }
                    val oauthService =
                        serviceBuilder.build(oauthApi)
                    oauthService.signRequest(OAuth2AccessToken(access_token), request)
                    object : AsyncTask<Void?, Void?, Void?>() {
                        protected override fun doInBackground(vararg voids: Void?): Void? {
                            try {
                                val response =
                                    oauthService.execute(request)
                                if (response.code == 200) {
                                    JasonHelper.next(
                                        "success",
                                        action,
                                        response.body,
                                        event,
                                        context
                                    )
                                } else {
                                    JasonHelper.next(
                                        "error",
                                        action,
                                        response.body,
                                        event,
                                        context
                                    )
                                }
                            } catch (e: Exception) {
                                handleError(e, action, event, context)
                            }
                            return null
                        }
                    }.execute()
                }
            } else {
                JasonHelper.next("error", action, data, event, context)
            }
            //change exception
            // изменить исключение
        } catch (e: JSONException) {
            handleError(e, action, event, context)
        }
    }

    private fun handleError(
        err: Exception,
        action: JSONObject,
        event: JSONObject?,
        context: Context
    ) {
        try {
            val error = JSONObject()
            error.put("data", err.toString())
            JasonHelper.next("error", action, error, event, context)
        } catch (e: JSONException) {
            Log.d(
                "Warning",
                e.stackTrace[0].methodName + " : " + e.toString()
            )
        }
    }
}