package site.app4web.app4web.UI

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast

import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException

import site.app4web.app4web.Launcher.Launcher.Companion.setting
import site.app4web.app4web.R


/**
 * https://metanit.com/java/android/5.11.php - создание фрагментов
 * Считывает список сайтов-json в Горизонт из файла указанного в settings
 * Если в settings пусто то по умолчанию ??? можно взять из assets такой же json но тогда откуда его наполнение?
 * тоже из assets или drawable ? можно переделать под file:// т.е из asset
 * см read_json из Helper.JasonHelper
 */
class RV_Gorizont : Fragment() {
    private lateinit var sites: Array<Site?>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,savedInstanceState: Bundle?): View? {

        // Асинхронное считывание файла результат content должен быть не Null - проверять - не сделано
        val content = ProgressTask().execute(setting.http_gorizont).get()
        // разбор считанного JSON
        try {
            sites = GsonBuilder().create().fromJson(content, Array<Site?>::class.java)
        } catch (e: JsonSyntaxException) {
            e.printStackTrace()
        }
        Toast.makeText(activity, "Gorizont загружен", Toast.LENGTH_SHORT).show()

        // Сначала создали сам контейнер фрагмента из разметки чтобы его напонять
        val rootView = inflater.inflate(R.layout.rv_gorizont, container, false)
        val recyclerView = rootView.findViewById<View>(R.id.rv_gorizont) as RecyclerView // ссылка на RecyclerView
        // создаем и устанавливаем для списка адаптер: true - горизонтальный: использовать item_gorizont.xml
        recyclerView.adapter = Adapter(activity!!, sites, true)

        //Заполненный rootView с адаптером возвращаем материанскому классе Fragment, который его высветит.
        return rootView
    }
}