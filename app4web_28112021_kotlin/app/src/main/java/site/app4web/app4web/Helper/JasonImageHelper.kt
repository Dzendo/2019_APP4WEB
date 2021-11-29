package site.app4web.app4web.Helper
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.bumptech.glide.request.animation.GlideAnimation
import com.bumptech.glide.request.target.SimpleTarget
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URI


class JasonImageHelper {
    interface JasonImageDownloadListener {
        fun onLoaded(data: ByteArray?, uri: Uri?)
    }

    private var listener: JasonImageDownloadListener?
    private var url: String? = null
    private var context: Context
    private lateinit var data: ByteArray

    constructor(url: String?, context: Context) {
        // set null or default listener or accept as argument to constructor
        // установить нулевой или прослушиватель по умолчанию или принять в качестве аргумента конструктор
        listener = null
        this.url = url
        this.context = context
    }

    constructor(data: ByteArray, context: Context) {
        listener = null
        this.data = data
        this.context = context
    }

    fun setListener(listener: JasonImageDownloadListener?) {
        this.listener = listener
    }

    fun load() {
        try {
            val file = File(
                context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                "share_image_" + System.currentTimeMillis() + ".png"
            )
            val out = FileOutputStream(file)
            out.write(data)
            out.close()
            val bitmapUri = Uri.fromFile(file)
            listener!!.onLoaded(data, bitmapUri)
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    }

    fun fetch() {
        try {
            // Constructing URL
            // Создание URL
            val url: GlideUrl
            val builder = LazyHeaders.Builder()

            // Add session if included
            // Добавить сессию, если включена
            val pref = context.getSharedPreferences("session", 0)
            var session: JSONObject? = null
            val uri_for_session = URI(this.url!!.toLowerCase())
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
            url = GlideUrl(this.url, builder.build())
            if (this.url!!.matches("\\.gif".toRegex())) {
                /*
                Glide
                        .with(context)
                        .load(url)
                        .asGif()
                        .into((ImageView)view);
                        */
            } else {
                val width = 512
                val height = 384
                Glide
                    .with(context)
                    .load(url)
                    .asBitmap()
                    .into(object : SimpleTarget<Bitmap?>(width, height) {
                        override fun onResourceReady(bitmap: Bitmap?, anim: GlideAnimation<in Bitmap?>?) {
                            var bitmapUri: Uri? = null
                            try {
                                val file = File(
                                    context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                                    "share_image_" + System.currentTimeMillis() + ".png"
                                )
                                val out = FileOutputStream(file)
                                bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
                                out.close()
                                bitmapUri = Uri.fromFile(file)
                                val stream = ByteArrayOutputStream()
                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                                val byteArray = stream.toByteArray()
                                listener!!.onLoaded(byteArray, bitmapUri)
                            } catch (e: java.lang.Exception) {
                                Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
                            }
                        }
                    })
            }
        } catch (e: java.lang.Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    }
}