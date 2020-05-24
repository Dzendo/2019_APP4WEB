package site.app4web.setting_app.UI

import android.os.AsyncTask
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import javax.net.ssl.HttpsURLConnection
import java.net.URL

// Класс для хранения информации, которая нужна для обработки задачи,
// Тип объектов, которые используются для индикации процесса выполнения задачи
// Тип результата задачи
class ProgressTask : AsyncTask<String, Void, String>() {
    override fun doInBackground(vararg path: String): String {

        var content: String
        try {
            content = getContent(path[0])

        } catch (ex: IOException) {
            content = "Null"
        }
      //  content= delComment (content)
        return content
    }

  /*  override fun onPostExecute(content: String) {
        super.onPostExecute(content)
    } */
}

// https://metanit.com/java/android/15.1.php
@Throws(IOException::class)
private fun getContent(path: String): String {
   var reader : BufferedReader? = null
   val httpConnection: HttpURLConnection
    try {
        val url = URL(path)
        if (path.toLowerCase().contains("https".toLowerCase())) {
            httpConnection = url.openConnection() as HttpsURLConnection
        } else {
            httpConnection = url.openConnection() as HttpURLConnection
        }
        httpConnection.requestMethod = "GET"
        httpConnection.readTimeout = 10000
        httpConnection.connect()
        reader = BufferedReader(InputStreamReader(httpConnection.inputStream))

        val buf = StringBuilder()
        var line: String?
        while (true){
            line = reader.readLine()
            if (line == null)  break
            buf.append(line + "\n")
        }
        return buf.toString()
    }
      finally {reader?.close()}
}
/*
private fun delComment(content_In: String): String {
    lateinit var content_Out:  StringBuffer
    var newlenght: Int = -1
    var apostrov = false
    var comment = false
    for (i in 0..content_In.length-1) {
        if (i<content_In.length-1) if (content_In[i].equals('/')     and content_In[i + 1].equals('*')) comment = true
        if (i>0)                   if (content_In[i - 1].equals('*') and content_In[i].equals('/'))     comment = false
        if (comment) continue
      /*  if (!apostrov and content_In[i].equals('"')) apostrov = true
        if (apostrov and  content_In[i].equals('"')) apostrov = false
        if (!apostrov and content_In[i].equals(' ')) continue */
            newlenght++
        }

        content_Out = StringBuffer(newlenght++)
        apostrov = false
        comment = false
        for (i in 0..content_In.length - 1) {
            if (i < content_In.length - 1) if (content_In[i].equals('/') and content_In[i + 1].equals('*')) comment = true
            if (i > 0)                     if (content_In[i - 1].equals('*') and content_In[i].equals('/')) comment = false
            if (comment) continue
           /* if (!apostrov and content_In[i].equals('"')) apostrov = true
            if (apostrov and  content_In[i].equals('"')) apostrov = false
            if (!apostrov and content_In[i].equals(' ')) continue */
            content_Out.append(content_In[i])
        }

        return  content_Out.toString()
}
*/

