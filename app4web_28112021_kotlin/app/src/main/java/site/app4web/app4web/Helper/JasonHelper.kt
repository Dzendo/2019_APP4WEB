package site.app4web.app4web.Helper

import android.content.Context
import android.content.Intent
import android.content.res.AssetManager
import android.graphics.Color
import android.graphics.Typeface
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.WindowManager
import android.widget.TextView
import site.app4web.app4web.Core.JasonViewActivity
import site.app4web.app4web.Launcher.Launcher
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.*
import java.nio.charset.StandardCharsets
import java.util.ArrayList
import java.util.regex.Matcher
import java.util.regex.Pattern
import timber.log.Timber


object JasonHelper {
    fun style(component: JSONObject, root_context: Context): JSONObject {
        val style = JSONObject()
        try {
            if (component.has("class")) {
                val style_class_string = component.getString("class")
                val style_classes = style_class_string.split("\\s+").toTypedArray()
                for (i in style_classes.indices) {
                    val astyle =
                        (root_context as JasonViewActivity).model!!.jason!!.getJSONObject("\$jason")
                            .getJSONObject("head").getJSONObject("styles").getJSONObject(
                            style_classes[i]
                        )
                    val iterator: Iterator<*> = astyle.keys()
                    var style_key: String?
                    while (iterator.hasNext()) {
                        style_key = iterator.next() as String?
                        style.put(style_key, astyle[style_key])
                    }
                }
            }
        } catch (e: Exception) {
            Timber.w(e)
        }
        try {
            // iterate through inline style and overwrite
            // перебираем встроенный стиль и перезаписываем
            if (component.has("style")) {
                val inline_style = component.getJSONObject("style")
                val iterator: Iterator<*> = inline_style.keys()
                var style_key: String?
                while (iterator.hasNext()) {
                    style_key = iterator.next() as String?
                    style.put(style_key, inline_style[style_key])
                }
            }
        } catch (e: Exception) {
            Timber.w(e)
        }
        return style
    }

    fun merge(old: JSONObject, add: JSONObject): JSONObject {
        return try {
            val stub = JSONObject(old.toString())
            val keysIterator = add.keys()
            while (keysIterator.hasNext()) {
                val key = keysIterator.next()
                val `val` = add[key]
                stub.put(key, `val`)
            }
            stub
        } catch (e: Exception) {
            Timber.w(e)
            JSONObject()
        }
    }

    fun next(type: String?, action: JSONObject, data: Any, event: JSONObject, context: Context?) {
        try {
            if (action.has(type)) {
                val intent = Intent(type)
                intent.putExtra("action", action[type].toString())
                intent.putExtra("data", data.toString())
                intent.putExtra("event", event.toString())
                LocalBroadcastManager.getInstance(context!!).sendBroadcast(intent)
            } else {
                // Release everything and finish
                val intent = Intent("call")
                val unlock_action = JSONObject()
                unlock_action.put("type", "\$unlock")
                intent.putExtra("action", unlock_action.toString())
                intent.putExtra("event", event.toString())
                LocalBroadcastManager.getInstance(context!!).sendBroadcast(intent)
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    /**
     * Parse a string as either a JSON Object or Array.
     * Анализировать строку как объект JSON или массив.
     *
     * @param json
     * @return a JSONObject or JSONArray based on the Json string,
     * return an emptry JSONObject if json param is null or on parsing supplied json string.
     * @ вернуть JSONObject или JSONArray на основе строки Json,
     * вернуть emptry JSONObject, если параметр json равен null или при разборе предоставленной строки json.
     */
    fun objectify(json: String?): Any {
        return try {
            if (json == null) {
                return JSONObject()
            }
            if (json.trim { it <= ' ' }.startsWith("[")) {
                // JSONArray
                JSONArray(json)
            } else if (json.trim { it <= ' ' }.startsWith("{")) {
                JSONObject(json)
            } else {
                JSONObject()
            }
        } catch (e: Exception) {
            Timber.w(e, "error objectifying: %s", json)
            JSONObject()
        }
    }

    fun toArrayList(jsonArray: JSONArray): ArrayList<JSONObject> {
        val list = ArrayList<JSONObject>()
        try {
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getJSONObject(i))
            }
        } catch (e: Exception) {
            Timber.w(e)
        }
        return list
    }

    fun ratio(ratio: String): Float {
        val regex = "^[ ]*([0-9]+)[ ]*[:/][ ]*([0-9]+)[ ]*$"
        val pat = Pattern.compile(regex)
        val m = pat.matcher(ratio)
        return if (m.matches()) {
            val w = m.group(1).toFloat()
            val h = m.group(2).toFloat()
            w / h
        } else {
            ratio.toFloat()
        }
    }

    fun pixels(context: Context, size: String, direction: String): Float {
        val regex_percent_and_pixels = "^([0-9.]+)%[ ]*([+-]?)[ ]*([0-9]+)$"
        val percent_pixels = Pattern.compile(regex_percent_and_pixels)
        var m = percent_pixels.matcher(size)
        return if (m.matches()) {
            val percentage = m.group(1).toFloat()
            val sign = m.group(2)
            var pixels = m.group(3).toFloat()
            pixels = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                pixels,
                context.resources.displayMetrics
            )
            val displayMetrics = DisplayMetrics()
            val windowmanager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowmanager.defaultDisplay.getMetrics(displayMetrics)
            val percent_height: Float
            val percent_width: Float
            var s: Float
            if (direction.equals("vertical", ignoreCase = true)) {
                val full = displayMetrics.heightPixels
                percent_height = full * percentage / 100
                s = percent_height
            } else {
                val full = displayMetrics.widthPixels
                percent_width = full * percentage / 100
                s = percent_width
            }
            s = if (sign.equals("+", ignoreCase = true)) {
                s + pixels
            } else {
                s - pixels
            }
            s
        } else {
            val regex = "(\\d+)%"
            val percent = Pattern.compile(regex)
            m = percent.matcher(size)
            val s: Float
            if (m.matches()) {
                val percentage = m.group(1).toFloat()
                val displayMetrics = DisplayMetrics()
                val windowmanager =
                    context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                windowmanager.defaultDisplay.getMetrics(displayMetrics)
                s = if (direction.equals("vertical", ignoreCase = true)) {
                    val full = displayMetrics.heightPixels
                    full * percentage / 100
                } else {
                    val full = displayMetrics.widthPixels
                    full * percentage / 100
                }
                s
            } else {
                s = size.toFloat()
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    s,
                    context.resources.displayMetrics
                )
            }
        }
    }

    fun parse_color(color_string: String?): Int {
        val rgb = Pattern.compile("rgb *\\( *([0-9]+), *([0-9]+), *([0-9]+) *\\)")
        val rgba = Pattern.compile("rgba *\\( *([0-9]+), *([0-9]+), *([0-9]+), *([0-9.]+) *\\)")
        val rgba_m = rgba.matcher(color_string)
        val rgb_m = rgb.matcher(color_string)
        return if (rgba_m.matches()) {
            val a = java.lang.Float.valueOf(rgba_m.group(4))
            val alpha = Math.round(a * 255)
            var hex = Integer.toHexString(alpha).toUpperCase()
            if (hex.length == 1) hex = "0$hex"
            hex = "0000$hex"
            Color.argb(
                hex.toInt(16),
                Integer.valueOf(rgba_m.group(1)),
                Integer.valueOf(rgba_m.group(2)),
                Integer.valueOf(rgba_m.group(3))
            )
        } else if (rgb_m.matches()) {
            Color.rgb(
                Integer.valueOf(rgb_m.group(1)),
                Integer.valueOf(rgb_m.group(2)),
                Integer.valueOf(rgb_m.group(3))
            )
        } else {
            // Otherwise assume hex code
            Color.parseColor(color_string)
        }
    }

    fun get_font(font: String, context: Context): Typeface {
        return Typeface.createFromAsset(context.assets, "fonts/$font.ttf")
    }

    @Throws(IOException::class)
    fun read_file_scheme(filename: String, context: Context?): String {
        var filename = filename
        filename = filename.replace("file://", "file/")
        return JasonHelper.read_file(filename, context)
    }

    @Throws(IOException::class)
    fun read_file(filename: String?, context: Context): String {
        val assets = context.assets
        val inputStream = assets.open(filename!!)
        val reader = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
        val stringBuilder = StringBuilder()
        var done = false
        while (!done) {
            val line = reader.readLine()
            done = line == null
            if (line != null) {
                stringBuilder.append("\n")
                stringBuilder.append(line)
            }
        }
        reader.close()
        inputStream.close()
        return stringBuilder.toString()
    }

    fun read_json(fn: String, context: Context): Any { //  throws IOException {
        // we're expecting a filename that looks like "file://..."
        // мы ожидаем имя файла, которое выглядит как "file://имя.json" - т.е из Assets:> /file/имя.json или

        // http:// ... .json на исполнение
        // Добавлено мной file://read// - это ключ входа в read_json должен быть обрезан
        // ссылку на внутр память dat://url_http.json
        // ссылку на файл на карточке: sd://url_http.json
        // ссылку на файл в assets: ass://url_http.json
        // ссылку на файл на assets/file file://url_http.json
        // строку с json объектом ram://str_json
        // надо еще добавить raw res внешняя_память shared и какие еще бывают
        var jr = ""
        val `is`: InputStream
        val sdFile: File
        var filename = fn.replace("file://read//", "")
        try {
            if (filename.startsWith("file://")) {
                filename = filename.replace("file://", "file/")
                `is` = context.openFileInput(filename)
                val size = `is`.available()
                val buffer = ByteArray(size)
                `is`.read(buffer)
                `is`.close()
                jr = String(buffer, StandardCharsets.UTF_8)
                return JasonHelper.str_to_json(jr)
            }
            if (filename.startsWith("ass://")) {
                filename = filename.replace("ass://", "")
                val assets = context.assets
                val inputStream = assets.open(filename)
                val reader = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
                val stringBuilder = StringBuilder()
                var done = false
                while (!done) {
                    val line = reader.readLine()
                    done = line == null
                    if (line != null) {
                        stringBuilder.append("\n")
                        stringBuilder.append(line)
                    }
                }
                reader.close()
                inputStream.close()
                jr = stringBuilder.toString()
                return JasonHelper.str_to_json(jr)
            }
            if (filename.startsWith("dat://")) {
                filename = filename.replace("dat://", "")
                `is` = context.openFileInput(filename)
                val size = `is`.available()
                val buffer = ByteArray(size)
                `is`.read(buffer)
                `is`.close()
                jr = String(buffer, StandardCharsets.UTF_8)
                return JasonHelper.str_to_json(jr)
            }
            if (filename.startsWith("sd://")) {
                filename = filename.replace("sd://", "")
                sdFile = File(filename)
                `is` = FileInputStream(sdFile)
                val size = `is`.available()
                val buffer = ByteArray(size)
                `is`.read(buffer)
                `is`.close()
                jr = String(buffer, StandardCharsets.UTF_8)
                return JasonHelper.str_to_json(jr)
            }
            if (filename.startsWith("ram://")) {
                jr = filename.replace("ram://", "")
                return JasonHelper.str_to_json(jr)
            }
        } catch (e: Exception) {
            Timber.w(e)
            return JSONObject()
        }
        return jr
    }

    // вызывается из read_json расшифровывает строку в json object DD
    private fun str_to_json(jr: String): Any {
        val ret: Any
        ret = try {
            if (jr.trim { it <= ' ' }.startsWith("[")) {        // array  массив
                JSONArray(jr)
            } else if (jr.trim { it <= ' ' }.startsWith("{")) { // object объект
                JSONObject(jr)
            } else {                                // string  строка
                jr
            }
        } catch (e: Exception) {
            Timber.w(e)
            return JSONObject()
        }
        return ret
    }

    fun read_json_old(fn: String, context: Context): Any { // throws IOException {
        // we're expecting a filename that looks like "file://..."
        // мы ожидаем имя файла, которое выглядит как "file://имя.json" - т.е из Assets:> /file/имя.json
        // или добавила ожидание file://#имя.json  - /т.е. из памяти приложения файл:  имя.json - нужно для обертки
        // нет добавила ожидание file:///путь/имя.json  - /т.е. с карты телефона файл:  путь/имя.json - нужно для отладки
        // или добавила ожидание file:///путь/имя.json  - /т.е. с SD карты файл: путь/имя.json - нужно для отладки
        var jr = ""
        val ret: Any
        val `is`: InputStream
        val sdFile: File
        var filename = fn.replace("file://", "")
        try {
            if (filename.trim { it <= ' ' }.startsWith("#")) {           // из памяти приложения
                `is` = context.openFileInput(filename)
            } else if (filename.trim { it <= ' ' }.startsWith("/")) {    // из SD памяти
                sdFile = File(filename)
                `is` = FileInputStream(sdFile)
            } else {                                        // из Assets/file памяти
                filename = "file/$filename"
                `is` = context.assets.open(filename)
            }
            val size = `is`.available()
            val buffer = ByteArray(size)
            `is`.read(buffer)
            `is`.close()
            jr = String(buffer, StandardCharsets.UTF_8)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        ret = try {
            if (jr.trim { it <= ' ' }.startsWith("[")) {        // array  массив
                JSONArray(jr)
            } else if (jr.trim { it <= ' ' }.startsWith("{")) { // object объект
                JSONObject(jr)
            } else {                                // string  строка
                jr
            }
        } catch (e: Exception) {
            Timber.w(e)
            return JSONObject()
        }
        return ret
    }

    /*    исходный модуль для удаления м изменения в IOS
    public static Object read_json_old(String fn, Context context) {// throws IOException {

        // we're expecting a filename that looks like "file://..."
        String filename = fn.replace("file://", "file/");
        String jr = null;
        Object ret;
        try {
            InputStream is = context.getAssets().open(filename);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            jr = new String(buffer, "UTF-8");

            if(jr.trim().startsWith("[")) {
                // array
                ret = new JSONArray(jr);
            } else if(jr.trim().startsWith("{")){
                // object
                ret = new JSONObject(jr);
            } else {
                // string
                ret = jr;
            }
        } catch (Exception e) {
            Timber.w(e);
            return new JSONObject();
        }
        return ret;
    }
*/
    fun permission_exception(actionName: String, context: Context?) {
        try {
            val intent = Intent("call")
            val alert_action = JSONObject()
            alert_action.put("type", "\$util.alert")
            val options = JSONObject()
            options.put("title", "Turn on Permissions")
            options.put(
                "description",
                "$actionName requires additional permissions. Go to AndroidManifest.xml file and turn on the permission"
            )
            alert_action.put("options", options)
            intent.putExtra("action", alert_action.toString())
            LocalBroadcastManager.getInstance(context!!).sendBroadcast(intent)
        } catch (e: Exception) {
            Timber.w(e)
        }
    }

    @Throws(IOException::class)
    fun readBytes(inputStream: InputStream): ByteArray {
        // this dynamically extends to take the bytes you read
        // это динамически расширяется, чтобы взять прочитанные вами байты
        val byteBuffer = ByteArrayOutputStream()

        // this is storage overwritten on each iteration with bytes
        // это хранилище перезаписывается на каждой итерации байтами
        val bufferSize = 1024
        val buffer = ByteArray(bufferSize)

        // we need to know how may bytes were read to write them to the byteBuffer
        // нам нужно знать, как могут быть прочитаны байты, чтобы записать их в byteBuffer
        var len = 0
        while (inputStream.read(buffer).also { len = it } != -1) {
            byteBuffer.write(buffer, 0, len)
        }

        // and then we can return your byte array.
        // и тогда мы можем вернуть ваш байтовый массив.
        return byteBuffer.toByteArray()
    }

    // dispatchIntent method
    // 1. triggers an external Intent
    // 2. attaches a callback with all the payload so that we can pick it up where we left off when the intent returns
    // the callback needs to specify the class name and the method name we wish to trigger after the intent returns
    // метод dispatchIntent
    // 1. запускает внешнее намерение
    // 2. присоединяет обратный вызов со всей полезной нагрузкой, чтобы мы могли забрать его там, где остановились, когда намерение вернулось
    // обратный вызов должен указать имя класса и имя метода, которое мы хотим вызвать после возврата намерения
    fun dispatchIntent(
        name: String,
        action: JSONObject?,
        data: JSONObject?,
        event: JSONObject?,
        context: Context,
        intent: Intent?,
        handler: JSONObject
    ) {
        // Generate unique identifier for return value
        // This will be used to name the handlers
        // Генерируем уникальный идентификатор для возвращаемого значения
        // Это будет использоваться для именования обработчиков
        val requestCode: Int
        requestCode = try {
            name.toInt()
        } catch (e: NumberFormatException) {
            -1
        }
        try {
            // handler looks like this:
            // обработчик выглядит так:
            /*
                  {
                    "class": [class name],
                    "method": [method name],
                    "options": {
                        [options to preserve]
                    }
                  }
             */
            val options = JSONObject()
            options.put("action", action)
            options.put("data", data)
            options.put("event", event)
            options.put("context", context)
            handler.put("options", options)
            (context.applicationContext as Launcher).once(name, handler)
        } catch (e: Exception) {
            Timber.w(e)
        }
        if (intent != null) {
            // Start the activity
            (context as JasonViewActivity).startActivityForResult(intent, requestCode)
        } else {
            // if intent is null,
            // it means we are manually going to deal with opening a new Intent
            // если намерение равно нулю,
            // это означает, что мы вручную будем иметь дело с открытием нового намерения
        }
    }

    fun dispatchIntent(
        action: JSONObject?,
        data: JSONObject?,
        event: JSONObject?,
        context: Context?,
        intent: Intent?,
        handler: JSONObject?
    ) {
        JasonHelper.dispatchIntent(
            ((System.currentTimeMillis() % 10000) as Int).toString(),
            action,
            data,
            event,
            context,
            intent,
            handler
        )
    }

    fun callback(callback: JSONObject?, result: String?, context: Context) {
        (context.applicationContext as Launcher).callback(
            callback,
            result,
            context as JasonViewActivity
        )
    }

    fun preserve(
        callback: JSONObject,
        action: JSONObject?,
        data: JSONObject?,
        event: JSONObject?,
        context: Context?
    ): JSONObject {
        return try {
            val callback_options = JSONObject()
            callback_options.put("action", action)
            callback_options.put("data", data)
            callback_options.put("event", event)
            callback_options.put("context", context)
            callback.put("options", callback_options)
            callback
        } catch (e: Exception) {
            Timber.e(e, "wasn't able to preserve stack for action: %s", action)
            callback
        }
    }

    fun setTextViewFont(view: TextView, style: JSONObject, context: Context) {
        try {
            if (style.has("font:android")) {
                val f = style.getString("font:android")
                if (f.equals("bold", ignoreCase = true)) {
                    view.typeface = Typeface.DEFAULT_BOLD
                } else if (f.equals("sans", ignoreCase = true)) {
                    view.typeface = Typeface.SANS_SERIF
                } else if (f.equals("serif", ignoreCase = true)) {
                    view.typeface = Typeface.SERIF
                } else if (f.equals("monospace", ignoreCase = true)) {
                    view.typeface = Typeface.MONOSPACE
                } else if (f.equals("default", ignoreCase = true)) {
                    view.typeface = Typeface.DEFAULT
                } else {
                    try {
                        val font_type = Typeface.createFromAsset(
                            context.assets,
                            "fonts/" + style.getString("font:android") + ".ttf"
                        )
                        view.typeface = font_type
                    } catch (e: Exception) {
                    }
                }
            } else if (style.has("font")) {
                if (style.getString("font").toLowerCase().contains("bold")) {
                    if (style.getString("font").toLowerCase().contains("italic")) {
                        view.setTypeface(Typeface.DEFAULT_BOLD, Typeface.ITALIC)
                    } else {
                        view.typeface = Typeface.DEFAULT_BOLD
                    }
                } else {
                    if (style.getString("font").toLowerCase().contains("italic")) {
                        view.setTypeface(Typeface.DEFAULT, Typeface.ITALIC)
                    } else {
                        view.typeface = Typeface.DEFAULT
                    }
                }
            }
        } catch (e: JSONException) {
        }
    }
}