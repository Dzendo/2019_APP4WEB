package site.app4web.app4web.Action

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import com.commonsware.cwac.cam2.AbstractCameraActivity
import com.commonsware.cwac.cam2.CameraActivity
import com.commonsware.cwac.cam2.VideoRecorderActivity
import com.commonsware.cwac.cam2.ZoomStyle
import site.app4web.app4web.Helper.JasonHelper
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date


class JasonMediaAction {
    /**********************************
     *
     * Play
     * Играть
     *
     */
    fun play(action: JSONObject, data: JSONObject?, event: JSONObject?, context: Context) {
        try {
            if (action.has("options")) {
                val intent = Intent(Intent.ACTION_VIEW)
                if (action.getJSONObject("options").has("url")) {
                    intent.setDataAndType(
                        Uri.parse(
                            action.getJSONObject("options").getString("url")
                        ), "video/mp4"
                    )
                }
                if (action.getJSONObject("options").has("muted")) {
                    val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        am.adjustStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            AudioManager.ADJUST_MUTE,
                            0
                        )
                    } else {
                        am.setStreamMute(AudioManager.STREAM_MUSIC, true)
                    }
                }
                val callback = JSONObject()
                callback.put("class", "JasonMediaAction")
                callback.put("method", "finishplay")
                JasonHelper.dispatchIntent(action, data, event, context, intent, callback)
            }
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    }

    // Util for play
    // Используем для игры
    fun finishplay(intent: Intent?, options: JSONObject) {
        try {
            val action = options.getJSONObject("action")
            val event = options.getJSONObject("event")
            val context = options["context"] as Context

            // revert mute
            if (action.getJSONObject("options").has("muted")) {
                val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0)
                } else {
                    am.setStreamMute(AudioManager.STREAM_MUSIC, false)
                }
            }
            JasonHelper.next("success", action, JSONObject(), event, context)
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    }

    /**********************************
     *
     * Picker + Camera
     * Сборщик + Камера
     *
     */
    fun picker(action: JSONObject, data: JSONObject?, event: JSONObject?, context: Context?) {

        // Image picker intent
        // Выбор изображения
        try {
            var type = "image"
            if (action.has("options")) {
                if (action.getJSONObject("options").has("type")) {
                    type = action.getJSONObject("options").getString("type")
                }
            }
            val intent: Intent
            intent = if (type.equals("video", ignoreCase = true)) {
                // video
                // видео
                Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            } else {
                // image
                // изображение
                Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            }

            // dispatchIntent method
            // 1. triggers an external Intent
            // 2. attaches a callback with all the payload so that we can pick it up where we left off when the intent returns
            // метод dispatchIntent
            // 1. запускает внешнее намерение
            // 2. присоединяет обратный вызов со всей полезной нагрузкой, чтобы мы могли забрать его там, где остановились, когда намерение вернулось

            // the callback needs to specify the class name and the method name we wish to trigger after the intent returns
            // обратный вызов должен указать имя класса и имя метода, которое мы хотим вызвать после возврата намерения
            val callback = JSONObject()
            callback.put("class", "JasonMediaAction")
            callback.put("method", "process")
            JasonHelper.dispatchIntent(action, data, event, context, intent, callback)
        } catch (e: SecurityException) {
            JasonHelper.permission_exception("\$media.picker", context)
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    }

    fun camera(action: JSONObject, data: JSONObject?, event: JSONObject?, context: Context) {

        // Image picker intent
        // Выбор изображения
        try {
            var q = AbstractCameraActivity.Quality.LOW
            var type = "photo"
            var edit = false
            if (action.has("options")) {
                val options = action.getJSONObject("options")

                // type
                if (options.has("type")) {
                    type = options.getString("type")
                }

                // quality
                q = if (type.equals("video", ignoreCase = true)) {
                    // video
                    // high by default
                    AbstractCameraActivity.Quality.HIGH
                } else {
                    // photo
                    // high by default
                    AbstractCameraActivity.Quality.HIGH
                }
                if (options.has("quality")) {
                    val quality = options.getString("quality")
                    if (quality.equals("low", ignoreCase = true)) {
                        q = AbstractCameraActivity.Quality.LOW
                    } else if (quality.equals("medium", ignoreCase = true)) {
                        q = AbstractCameraActivity.Quality.HIGH
                    }
                }

                // edit
                // редактировать
                if (options.has("edit")) {
                    edit = true
                }
            }
            val intent: Intent
            intent = if (type.equals("video", ignoreCase = true)) {
                // video
                // видео
                val builder = VideoRecorderActivity.IntentBuilder(context)
                    .to(createFile("video", context))
                    .zoomStyle(ZoomStyle.SEEKBAR)
                    .updateMediaStore()
                    .quality(q)
                builder.build()
            } else {
                // photo
                // Фото
                val builder = CameraActivity.IntentBuilder(context)
                    .to(createFile("image", context))
                    .zoomStyle(ZoomStyle.SEEKBAR)
                    .updateMediaStore()
                    .quality(q)
                if (!edit) {
                    builder.skipConfirm()
                }
                builder.build()
            }

            // dispatchIntent method
            // 1. triggers an external Intent
            // 2. attaches a callback with all the payload so that we can pick it up where we left off when the intent returns
            // метод dispatchIntent
            // 1. запускает внешнее намерение
            // 2. присоединяет обратный вызов со всей полезной нагрузкой, чтобы мы могли забрать его там, где остановились, когда намерение вернулось

            // the callback needs to specify the class name and the method name we wish to trigger after the intent returns
            // обратный вызов должен указать имя класса и имя метода, которое мы хотим вызвать после возврата намерения
            val callback = JSONObject()
            callback.put("class", "JasonMediaAction")
            callback.put("method", "process")
            JasonHelper.dispatchIntent(action, data, event, context, intent, callback)
        } catch (e: SecurityException) {
            JasonHelper.permission_exception("\$media.camera", context)
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    }

    // util
    // утилита
    fun process(intent: Intent, options: JSONObject) {
        try {
            val action = options.getJSONObject("action")
            val data = options.getJSONObject("data")
            val event = options.getJSONObject("event")
            val context = options["context"] as Context
            val uri = intent.data

            // handling image
            // обработка изображения
            var type = "image"
            if (action.has("options")) {
                if (action.getJSONObject("options").has("type")) {
                    type = action.getJSONObject("options").getString("type")
                }
            }
            if (type.equals("video", ignoreCase = true)) {
                // video
                // видео
                try {
                    val ret = JSONObject()
                    ret.put("file_url", uri.toString())
                    ret.put("content_type", "video/mp4")
                    JasonHelper.next("success", action, ret, event, context)
                } catch (e: Exception) {
                    Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
                }
            } else {
                // image
                // изображение
                val stream = context.contentResolver.openInputStream(uri!!)
                val byteArray = JasonHelper.readBytes(stream)
                val encoded = Base64.encodeToString(byteArray, Base64.NO_WRAP)
                val stringBuilder = StringBuilder()
                stringBuilder.append("data:image/jpeg;base64,")
                stringBuilder.append(encoded)
                val data_uri = stringBuilder.toString()
                try {
                    val ret = JSONObject()
                    ret.put("data", encoded)
                    ret.put("data_uri", data_uri)
                    ret.put("content_type", "image/jpeg")
                    JasonHelper.next("success", action, ret, event, context)
                } catch (e: Exception) {
                    Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
                }
            }
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    }

    @Throws(IOException::class)
    private fun createFile(type: String, context: Context): File {
        // Create an image file name
        // Создать имя файла изображения
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val fileName = "" + timeStamp + "_"
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val f: File
        f = if (type.equals("image", ignoreCase = true)) {
            File.createTempFile(fileName, ".jpg", storageDir)
        } else if (type.equals("video", ignoreCase = true)) {
            File.createTempFile(fileName, ".mp4", storageDir)
        } else {
            File.createTempFile(fileName, ".txt", storageDir)
        }
        return f
    }
}