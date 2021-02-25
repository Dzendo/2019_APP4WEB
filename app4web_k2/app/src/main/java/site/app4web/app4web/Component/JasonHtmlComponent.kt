package site.app4web.app4web.Component

import android.content.*
import android.util.Log
import android.view.*
import android.view.View.OnTouchListener
import android.webkit.*
import org.json.JSONObject
import site.app4web.app4web.Core.JasonViewActivity


object JasonHtmlComponent {
    fun build(view: View?, component: JSONObject, parent: JSONObject?, context: Context): View {
        if (view == null) {
            try {
                val webview = WebView(context)
                webview.settings.defaultTextEncodingName = "utf-8"
                return webview
            } catch (e: Exception) {
                Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
            }
        } else {
            JasonComponent.build(view, component, parent, context)
            try {
                val text = component.getString("text")
                CookieManager.getInstance().setAcceptCookie(true)
                (view as WebView).loadDataWithBaseURL(
                    "http://localhost/",
                    text,
                    "text/html",
                    "utf-8",
                    null
                )
                view.webChromeClient = WebChromeClient()
                view.setVerticalScrollBarEnabled(false)
                view.setHorizontalScrollBarEnabled(false)
                val settings = view.settings
                settings.layoutAlgorithm = WebSettings.LayoutAlgorithm.SINGLE_COLUMN
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.javaScriptCanOpenWindowsAutomatically = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.setAppCachePath(context.cacheDir.absolutePath)
                settings.allowFileAccess = true
                settings.setAppCacheEnabled(true)
                settings.cacheMode = WebSettings.LOAD_DEFAULT


                // not interactive by default;
                // по умолчанию не интерактивен;
                var responds_to_webview = false
                if (component.has("action")) {
                    if (component.getJSONObject("action").has("type")) {
                        val action_type = component.getJSONObject("action").getString("type")
                        if (action_type.equals("\$default", ignoreCase = true)) {
                            responds_to_webview = true
                        }
                    }
                }
                if (responds_to_webview) {
                    // webview receives click
                    // веб-просмотр получает клик
                    view.setOnTouchListener(null)
                    // Don't add native listener to this component
                    // Не добавлять нативный слушатель в этот компонент
                } else {
                    // webview shouldn't receive click
                    // веб-просмотр не должен получать клик
                    view.setOnTouchListener(OnTouchListener { v, event ->
                        val component = v.tag as JSONObject
                        try {
                            // 1. if the action type $default is specified, do what's default for webview
                            // 1. если указан тип действия $default, сделать то, что по умолчанию для веб-просмотра
                            if (component.has("action")) {
                                val action = component.getJSONObject("action")
                                if (action.has("type")) {
                                    if (action.getString("type")
                                            .equals("\$default", ignoreCase = true)
                                    ) {
                                        return@OnTouchListener false
                                    }
                                }
                            }

                            // But only trigger on UP motion
                            // Но запускаем только при движении UP
                            if (event.action == MotionEvent.ACTION_UP) {
                                if (component.has("action")) {
                                    // if the current component contains an action, run that one
                                    // если текущий компонент содержит действие, запустите его
                                    val action = component.getJSONObject("action")
                                    (context as JasonViewActivity).call(
                                        action.toString(),
                                        JSONObject().toString(),
                                        "{}",
                                        v.context
                                    )
                                } else {
                                    // otherwise, bubble up the event to the closest parent view with an 'action' attribute
                                    // в противном случае накапливаем событие до ближайшего родительского представления с атрибутом 'action'
                                    var cursor = v
                                    while (cursor.parent != null) {
                                        val item = (cursor.parent as View).tag as JSONObject
                                        cursor =
                                            if (item != null && (item.has("action") || item.has("href"))) {
                                                (cursor.parent as View).performClick()
                                                break
                                            } else {
                                                cursor.parent as View
                                            }
                                    }
                                }
                            }
                            return@OnTouchListener true
                        } catch (e: Exception) {
                            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
                        }
                        true
                    })
                }
                view.requestLayout()
                return view
            } catch (e: Exception) {
                Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
            }
        }
        return View(context)
    }
}