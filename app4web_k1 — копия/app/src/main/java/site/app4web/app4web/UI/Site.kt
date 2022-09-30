package site.app4web.app4web.UI

// класс описания web ресурса - сайта, json, и.т.п
class Site {

    var link = "https://web4app.site/web4app.json"  // Ссылка на ресурс в сети
    var icon = "https://DinaDurykina.github.io/icons8-windows-8-24.png"   // Ссылка на иконку

    //  @see #getDrawable (int, Theme)
    //     * @deprecated Используйте {@link #getDrawable (int, Theme)} вместо этого.
    //var icon =  getCurrentContext().getApplicationContext().getResources().getDrawable(R.drawable.ph)   // Ссылка на иконку
    var name = "WEB4APP"                            // Имя ресурса Язык??
    var title = "Браузер web4app"                    // Заголовок сайта в одну строку
    var anotation: Array<String>? = null            // Аннотация к сайту - массив строк

    var ru_json = true                          // Признак - ресурс сайт или Json файл
    var online = false                          // Доступен ли в сети
    var name_lite = "name lite"                 // Краткое имя ресурса Язык??
    var money = false                           // Оплачен
    var reklama = true                          // Всавлять рекламу да/нет

    fun setOnline() {
        val content = ProgressTask().execute(link).get()
        online = content != "Null"
    }
}
