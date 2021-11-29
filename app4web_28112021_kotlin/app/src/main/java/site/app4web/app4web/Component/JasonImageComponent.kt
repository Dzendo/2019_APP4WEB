package site.app4web.app4web.Component
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.bumptech.glide.load.resource.drawable.GlideDrawable
import com.bumptech.glide.request.animation.GlideAnimation
import com.bumptech.glide.request.target.BitmapImageViewTarget
import com.bumptech.glide.request.target.SimpleTarget
import org.json.JSONObject
import site.app4web.app4web.Helper.JasonHelper
import java.net.URI


object JasonImageComponent {
    private fun prepare(component: JSONObject, context: Context): LazyHeaders.Builder? {
        return try {
            // Constructing URL
            // Создание URL
            var url: GlideUrl
            val builder = LazyHeaders.Builder()

            // Add session if included
            // Добавить сессию, если включена
            val pref = context.getSharedPreferences("session", 0)
            var session: JSONObject? = null
            val uri_for_session = URI(component.getString("url").toLowerCase())
            val session_domain = uri_for_session.host
            if (pref.contains(session_domain)) {
                val str = pref.getString(session_domain, null)
                session = JSONObject(str)
            }
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
            if (component.has("header")) {
                val keys: Iterator<*> = component.getJSONObject("header").keys()
                while (keys.hasNext()) {
                    val key = keys.next() as String
                    val `val` = component.getJSONObject("header").getString(key)
                    builder.addHeader(key, `val`)
                }
            }
            builder
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
            null
        }
    }

    fun resolve_url(component: JSONObject, context: Context?): Any {
        return try {
            val url = component.getString("url")
            if (url.contains("file://")) {
                "file:///android_asset/file/" + url.substring(7)
            } else if (url.startsWith("data:image")) {
                url
            } else {
                val builder = JasonImageComponent.prepare(component, context)
                GlideUrl(url, builder.build())
            }
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
            ""
        }
    }

    private fun gif(component: JSONObject, view: View, context: Context) {
        val new_url = JasonImageComponent.resolve_url(component, context)
        Glide
            .with(context)
            .load(new_url)
            .asGif()
            .diskCacheStrategy(DiskCacheStrategy.SOURCE)
            .into(view as ImageView)
    }

    private fun rounded(
        component: JSONObject,
        view: View,
        corner_radius_float: Float,
        context: Context
    ) {
        val new_url = resolve_url(component, context)
        try {
            Glide
                .with(context)
                .load(new_url)
                .asBitmap()
                .fitCenter()
                .into(object : BitmapImageViewTarget(view as ImageView) {
                    override fun setResource(res: Bitmap) {
                        val bitmapDrawable =
                            RoundedBitmapDrawableFactory.create(context.resources, res)
                        bitmapDrawable.cornerRadius = corner_radius_float
                        (view as ImageView).setImageDrawable(bitmapDrawable)
                    }
                })
        } catch (e: java.lang.Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    }

    private fun normal(component: JSONObject, view: View, context: Context) {
        val new_url = JasonImageComponent.resolve_url(component, context)
        if (new_url.javaClass.toString()
                .equals("string", ignoreCase = true) && (new_url as String).startsWith("data:image")
        ) {
            val n = new_url
            val base64: String
            base64 = if (n.startsWith("data:image/jpeg")) {
                n.substring("data:image/jpeg;base64,".length)
            } else if (n.startsWith("data:image/png")) {
                n.substring("data:image/png;base64,".length)
            } else if (n.startsWith("data:image/gif")) {
                n.substring("data:image/gif;base64,".length)
            } else {
                "" // exception
            }
            val bs = Base64.decode(base64, Base64.NO_WRAP)
            Glide.with(context).load(bs)
                .into(object : SimpleTarget<GlideDrawable?>() {
                    override fun onResourceReady(
                        resource: GlideDrawable?,
                        glideAnimation: GlideAnimation<in GlideDrawable?>?
                    ) {
                        (view as ImageView).setImageDrawable(resource)
                    }
                })
        } else {
            Glide
                .with(context)
                .load(new_url)
                .into(view as ImageView)
        }
    }

    private fun tinted(component: JSONObject, view: View, context: Context) {
        try {
            val new_url = JasonImageComponent.resolve_url(component, context)
            val style = component.getJSONObject("style")
            Glide
                .with(context)
                .load(new_url)
                .asBitmap()
                .fitCenter()
                .into(object : BitmapImageViewTarget(view as ImageView) {
                    override fun setResource(res: Bitmap?) {
                        val d = BitmapDrawable(context.resources, res)
                        try {
                            val wrapper = DrawableCompat.wrap(d)
                            DrawableCompat.setTint(
                                wrapper,
                                JasonHelper.parse_color(style.getString("color"))
                            )
                            (view as ImageView).setImageDrawable(wrapper)
                        } catch (e: Exception) {
                            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
                            (view as ImageView).setImageDrawable(d)
                        }
                    }
                })
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    }

    fun build(view: View?, component: JSONObject, parent: JSONObject?, context: Context?): View {
        return if (view == null) {
            try {
                val imageview: ImageView
                imageview = ImageView(context)
                imageview.adjustViewBounds = true
                imageview
            } catch (e: Exception) {
                Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
                View(context)
            }
        } else {
            JasonComponent.Companion.build(view, component, parent, context)
            val width = 0
            val height = 0
            var corner_radius = 0f
            try {
                val type: String
                val style = JasonHelper.style(component, context)
                if (style.has("corner_radius")) {
                    corner_radius =
                        JasonHelper.pixels(context, style.getString("corner_radius"), "horizontal")
                }
                type = component.getString("type")
                return if (component.has("url")) {
                    if (component.getString("url").contains("file://")) {
                        if (corner_radius == 0f) {
                            try {
                                if (component.getString("url").matches(".*\\.gif".toRegex())) {
                                    JasonImageComponent.gif(component, view, context)
                                } else {
                                    if (style.has("color")) {
                                        JasonImageComponent.tinted(component, view, context)
                                    } else {
                                        JasonImageComponent.normal(component, view, context)
                                    }
                                }
                            } catch (e: Exception) {
                                Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
                            }
                        } else {
                            val corner_radius_float = corner_radius
                            try {
                                JasonImageComponent.rounded(
                                    component,
                                    view,
                                    corner_radius_float,
                                    context
                                )
                            } catch (e: Exception) {
                                Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
                            }
                        }
                        JasonComponent.Companion.addListener(view, context)
                        view.requestLayout()
                        view
                    } else {
                        if (corner_radius == 0f) {
                            try {
                                if (component.getString("url").matches(".*\\.gif".toRegex())) {
                                    JasonImageComponent.gif(component, view, context)
                                } else {
                                    if (style.has("color")) {
                                        JasonImageComponent.tinted(component, view, context)
                                    } else {
                                        JasonImageComponent.normal(component, view, context)
                                    }
                                }
                            } catch (e: Exception) {
                                Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
                            }
                        } else {
                            val corner_radius_float = corner_radius
                            try {
                                JasonImageComponent.rounded(
                                    component,
                                    view,
                                    corner_radius_float,
                                    context
                                )
                            } catch (e: Exception) {
                                Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
                            }
                        }
                        JasonComponent.Companion.addListener(view, context)
                        view.requestLayout()
                        view
                    }
                } else {
                    View(context)
                }
            } catch (e: Exception) {
                Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
            }
            View(context)
        }
    }
}