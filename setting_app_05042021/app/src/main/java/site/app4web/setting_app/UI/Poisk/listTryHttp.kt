package site.app4web.setting_app.UI.Poisk

import java.net.URL
import java.net.MalformedURLException

fun listTryHttp(url_zapros: String): ArrayList<String> {
    var url_site = url_zapros

    val TryList = ArrayList<String>()                      // 2 Лист сайтов

    // Если точно не САЙТ - в ПОИСК (empty)
    if (!url_site.contains("."))    return TryList
    url_site = url_site.trim { it <= ' ' }.toLowerCase()
    if (url_site.startsWith("."))   return TryList
    if (url_site.endsWith("."))     return TryList
    if (url_site.contains(" "))     return TryList

    // Если это все таки URL - проверка на правильность и http <--> https
    try {
        URL(url_site)
        // Это наконец есть ли указанный "точно" сайт в Сети
        TryList.add(url_site)  // Простая прямая проверка на то что ввели в url_site
        if (url_site.startsWith("http:"))  TryList.add(url_site.replace("http://", "https://"))
        if (url_site.startsWith("https:")) TryList.add(url_site.replace("https://", "http://"))
    } catch (e: MalformedURLException) {
      // Toast.makeText(applicationContext, "Запрос не является URL: $url_site", Toast.LENGTH_SHORT).show()
    }

    // Если сайт без HTTH то приклеть и проверить Сайт или в Поиск
    if (!url_site.startsWith("http")) TryList.add("https://$url_site")
    if (!url_site.startsWith("http")) TryList.add("http://$url_site")

    // Если WWW или м.б. сайт без HTTH то удалить приклеть и проверить Сайтб если нет то в Поиск
    if (url_site.contains("www.")) TryList.add(url_site.replace("www.", ""))
    if (url_site.startsWith("www.")) TryList.add("http://$url_site")
    if (url_site.startsWith("www.")) TryList.add("https://$url_site")
    if (url_site.startsWith("www.")) TryList.add(url_site.replace("www.", "http://"))
    if (url_site.startsWith("www.")) TryList.add(url_site.replace("www.", "http://"))

    return TryList
}
