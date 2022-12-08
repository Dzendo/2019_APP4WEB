package site.app4web.app4web.UI

import android.content.Context
import android.content.Intent
import android.widget.Toast
import java.io.*

import site.app4web.app4web.Core.JasonViewActivity

// процедура получает строку заказа и если она НЕ .json то оборачивает в семь строк urltemplate
// далее вызывает Jasonette передавая ей:
// http:// ... .json на исполнение
// Добавлено мной file://read// - это ключ входа в read_json должен быть обрезан
// ссылку на внутр память dat://url_http.json
// ссылку на файл на карточке: sd://url_http.json
// ссылку на файл в assets: ass://url_http.json
// ссылку на файл на assets/file file://url_http.json
// строку с json объектом ram://str_json
// надо еще добавить raw res внешняя_память shared и какие еще бывают
fun DoHttp (context: Context, url_http_in: String) {
    //val setting = Setting.setting
    var url_http = url_http_in
    lateinit var jasonStr: String
    val urltemplate = "#jason.json" // setting.urltemplate //       "#jason.json"
    var template_jason_str : String =""
        template_jason_str = context.assets.open(urltemplate).bufferedReader().use { it.readText() }

        //if (url_http == "") url_http = getString(R.string.url)!!
        if (url_http.contains(".json", true)) {
            if (!url_http.trim().contains("http", true))
            url_http = "file://read//" + url_http

            val intent = Intent(context, JasonViewActivity::class.java)
            intent.putExtra("url", url_http)
            context.startActivity(intent)  // работает в Внешней процедуре
            return
        }


            Toast.makeText(context, url_http + "-->" + urltemplate, Toast.LENGTH_LONG).show()
            //jasonStr = context.assets.open(urltemplate).bufferedReader().use { it.readText() }
            jasonStr = template_jason_str.replace("http://####.###/", url_http)

            // записываем временный Json - входной для Jasonette в память приложения добавил context
            val bw = BufferedWriter(OutputStreamWriter(
                    context.openFileOutput(urltemplate, Context.MODE_PRIVATE)))
            bw.write(jasonStr)
            bw.close()
            url_http = urltemplate

            Toast.makeText(context, url_http, Toast.LENGTH_SHORT).show()
            // Добавлено мной file://read// - это ключ входа в read_json должен быть обрезан
            val intent = Intent(context, JasonViewActivity::class.java)
            //intent.putExtra("url", "file://read//dat://" + url_http)
            intent.putExtra("url", "file://read//ram://" + jasonStr)
            context.startActivity(intent)  // работает в Внешней процедуре
    }

fun DoHttp_old (context: Context, url_http_in: String) {
    //val setting = Setting.setting
    var url_http = url_http_in
    lateinit var jasonStr: String
    val urltemplate = "#jason.json" // setting.urltemplate //       "#jason.json"

    //if (url_http == "") url_http = getString(R.string.url)!!
    if (!url_http.contains(".json", true)) {
        Toast.makeText(context, url_http + "-->" + urltemplate, Toast.LENGTH_LONG).show()

        jasonStr = context.assets.open(urltemplate).bufferedReader().use { it.readText() }
        jasonStr = jasonStr.replace("http://####.###/", url_http)

        // записываем временный Json - входной для Jasonette в память приложения добавил context
        val bw = BufferedWriter(OutputStreamWriter(
            context.openFileOutput(urltemplate, Context.MODE_PRIVATE)))
        bw.write(jasonStr)
        bw.close()
        url_http = urltemplate
    } else if (!url_http.trim().startsWith("http")) {
        url_http = "/storage/sdcard1/" + url_http
    }
    if (!url_http.trim().startsWith("http")) {
        url_http = "file://" + url_http
    }
    Toast.makeText(context, url_http, Toast.LENGTH_SHORT).show()

    val intent = Intent(context, JasonViewActivity::class.java)
    intent.putExtra("url", url_http)
    context.startActivity(intent)  // работает в Внешней процедуре
}

