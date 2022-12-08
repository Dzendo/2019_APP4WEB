package site.app4web.app4web.UI

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast

import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.GsonBuilder
import org.json.JSONException

import site.app4web.app4web.Launcher.Launcher.Companion.setting
import site.app4web.app4web.MainActivity
import site.app4web.app4web.R
import site.app4web.app4web.UI.Poisk.Poisk
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * https://metanit.com/java/android/5.11.php
 *
 *
 * https://metanit.com/java/android/5.11.php - создание фрагментов
 * Считывает список сайтов-json в Вертикаль из файла указанного в settings
 * Если в settings пусто то по умолчанию ??? можно взять из assets такой же json но тогда откуда его наполнение?
 * тоже из assets или drawable ? можно переделать под file:// т.е из asset
 * см read_json из Helper.JasonHelper
 *
 * Вызывает поиск Google и передает его результаты адапрету RecyclerView для вывода
 */
class RV_Vertical : Fragment(), MainActivity.OnSearch {
    private lateinit var sites: Array<Site?>
    private lateinit var rootView: View
    private lateinit var recyclerView :RecyclerView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,savedInstanceState: Bundle?): View? {

        // Асинхронное считывание файла результат content должен быть не Null - проверять - не сделано
        val content = ProgressTask().execute(setting?.http_vertical).get()
        // разбор считанного JSON
        try {
          sites = GsonBuilder().create().fromJson(content, Array<Site?>::class.java)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        Toast.makeText(activity, "Vertical загружен", Toast.LENGTH_SHORT).show()

        // Сначала создали сам контейнер фрагмента из разметки чтобы его напонять
         rootView = inflater.inflate(R.layout.rv_vertical, container, false)
         recyclerView = rootView.findViewById<View>(R.id.rv_vertical) as RecyclerView // ссылка на RecyclerView
        // создаем и устанавливаем для списка адаптер false - Вертикальный: использовать item_vertical.xml
        recyclerView.adapter = Adapter(activity!!, sites,false)

        //Заполненный rootView с адаптером возвращаем материанскому классе Fragment, который его высветит.
        return rootView
    }
    override fun onSearch(search: String) {
        val poisk = Poisk().execute(search)
        try {
            val resultitems = poisk.get(15, TimeUnit.SECONDS)
            if (resultitems != null) {
                sites = Array(resultitems.size, {_->null} )

                (0 until resultitems.size).forEach { i ->
                    val site = Site()
                    site.title = resultitems[i]["title"].toString()
                    site.link =  resultitems[i]["link"].toString()
                 // site.icon = "https://web4app.site/web4app.png"
                    sites[i]=site
                }
              /* Вариант с ArrayList не проходит т.к. Gson работает только с Array
                val sites1:ArrayList<Site?> = ArrayList(resultitems.size)
                resultitems.forEach { ri ->
                    val site :Site = Site()
                    site.head = ri["title"].toString()
                    site.http = ri["link"].toString()
                    sites1.add(site)
                }
                sites=sites1.toArray(sites)
                */
            }
            recyclerView.adapter = Adapter(activity!!, sites,false)
            Toast.makeText(activity, "++++ Найдено ${resultitems!!.size} для $search +++ ", Toast.LENGTH_LONG).show()
        } catch (e: TimeoutException) {
            poisk.cancel(true)
            Toast.makeText(activity, " Вышло Время Поиска ", Toast.LENGTH_SHORT).show()

            recyclerView.adapter = null

        }

    }
}
