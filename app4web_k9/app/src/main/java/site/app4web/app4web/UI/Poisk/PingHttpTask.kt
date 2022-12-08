package site.app4web.app4web.UI.Poisk

import android.os.AsyncTask
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
/* Этот класс проверяет доступность в инете сайта
    Вызов:
    var pingHttpTask: PingHttpTask
    pingHttpTask = PingHttpTask()
    pingHttpTask.execute(Str_URL)
    Ответ: адрес если он доступен
           null - усли адрес недоступен в инете
    также может проверять лист адресов
 */
class PingHttpTask : AsyncTask<String, Void, String?>() {
    override fun doInBackground(vararg urls: String): String? {
        urls.forEach { url ->
            try {
                val httpConnection = URL(url).openConnection() as HttpURLConnection
                httpConnection.requestMethod = "HEAD"
                // httpConnection.setReadTimeout(10000)
                val code = httpConnection.responseCode
                if (code == HttpURLConnection.HTTP_OK) { // (200) HttpURLConnection.HTTP_NOT_FOUND
                    try {
                        httpConnection.disconnect()
                    } catch (e: Exception) {  }
                    return url
                }
            } catch (e: MalformedURLException) {
            } catch (e: IOException) {
            }
        }
        return null
    }
}
