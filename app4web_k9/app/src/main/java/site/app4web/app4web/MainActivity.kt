package site.app4web.app4web

/** web4app - browser json файлов для "нативного" показа сайтов на Android
 * Собран на Android Studio 3.4.0 rc02 на библиотеках Android Q (29) preview
 * опирается на androidx app compact; рассчитан на android 22-29 target 28
 */

import android.content.Context
import android.net.ConnectivityManager
import androidx.appcompat.app.AppCompatActivity

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager

import android.widget.Toast


import com.google.android.material.snackbar.Snackbar
import com.google.android.gms.ads.AdRequest
//import com.google.android.gms.ads.InterstitialAd
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.rewarded.RewardedAd
//import com.google.android.gms.ads.reward.RewardedVideoAd
import com.google.firebase.analytics.FirebaseAnalytics

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*

import site.app4web.app4web.UI.DoHttp
import site.app4web.app4web.UI.Poisk.listTryHttp
import site.app4web.app4web.UI.Poisk.tryHttp1
import site.app4web.app4web.UI.Poisk.tryHttp2
import site.app4web.app4web.UI.RV_Vertical


/** WEB4APP - Browser json sites
 * Функция web4app - пообщаться с user--> выбать сайт --> если не json "Обернуть" в json -->
 * JSON передать на исполнение Core.JasonViewActivity пакета Jasonette
 * повторить выбор если вернулись из сайта
 * Все Json будут браться с web4app.site в т.ч settings.json или откуда указано
 *
 *  Главный и единственнывй эран web4app c Application Launcher + DebugLauncher (Stetho - отладчик Chrome)
 * построен на основе шаблона Basic AS 3.4
 *
 * разметка layout/activity_main.xml (AppBarLayout c Toolbar и Navigation + Icon; gms.ads.AdView - реклама; FAB;)
 * с include layout\content_main.xml (Icon ; Поле ввода Http ; Два fragments RecyclerView: sites.json and sites search
 * Предполагается menu settings по настройке броузера
 * Предполагается menu templates по выбору шаблона "обертки" сайтов в Json для броузера
 * Предполагается поиск сайтов по отсуствию вызываемого сайта с выводом результата поиска в второй fragment
 *
 * вспомогательные модули лежат в подпакете UI:
 * UI.Setting: Объект который считывает и хранит настроки Web4App
 * UI.DoHttp.kt: Получает Http сайта --> оборачивает в JSON(по необх) --> вызывает Jasonette для этого Json
 * UI.ProgressTask.kt - Получает Http файла --> Асинхронно считывает его --> возвращает содержимое файла в строке
 * UI.RV_Gorizont UI.RV_Vertical and UI\Site.kt классы для хранения и высветки имен сайтов в фрагментах RecyclerView
 *
 * UI.Adapter - Adapter для RecyclerView (один на двоих) с выбором сайта клиентом.
 * Для высветки использует layout/item_gorizont.xml и layout/item_vertical.xml
 * По выбору сайта ухлодит на DoHttp.kt - оборачивание в json и вызова Jasonette
 *
 * Вмешался в read_json из Helper\JasonHelper.java для Считывания "обернутого" #jason и с внешней памяти Json
 */

// Изменил программер HUB
class MainActivity : AppCompatActivity() {
    // переменные для показа рекламных блоков от FirebaseAnalytics and MobileAds
    private lateinit var mFirebaseAnalytics: FirebaseAnalytics
    private lateinit var mInterstitialAd: InterstitialAd
    private lateinit var mRewardedVideoAd: RewardedAd //RewardedVideoAd

    private lateinit var urltemplate : String  // текущий Шаблон для "оборачивания" должен выбираться
    private  var url_http : String ="" //setting.url_http    // Строка ввода с которой работаем с клиентом

    private var mListener: OnSearch? = null
    interface OnSearch { fun onSearch(search: String) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // setting.Read_Setting(this,getString(R.string.http_setting))  //в Launcher\Launcher.java Считываение
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val fm = supportFragmentManager
        var fragment = fm.findFragmentById(R.id.rv_vertical)
        if (fragment == null) {
            fragment = RV_Vertical()
            fm.beginTransaction()
                .add(R.id.rv_vertical, fragment)
                .commit()
        }
        if (fragment is OnSearch) {
            mListener = fragment
        } else { //throw RuntimeException("$fragment must implement onActivityDataListener")
        }

        // Obtain the FirebaseAnalytics and MobileAds Ainstance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)
       // MobileAds.initialize(this, getString(R.string.app_ad_unit_id_0))
        MobileAds.initialize(this) //, getString(R.string.app_ad_unit_id_0))
        // запрос баннера для первого экрана поле : adView01 - Баннер
        val madRequest = AdRequest.Builder().build()
        adView01.loadAd(madRequest)  // sugar

        // Слушатель нажатия на FAB - шаблон
        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
            // просто для теста передает событие FirebaseAnalytics - это образец
            val bundle = Bundle()  // Передача события FirebaseAnalytics
            bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "web4app")
            bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "Main")
            bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "FAB")
            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)
        }
        // прослушиваем клавиатуру для реакции на нажатие "ВВОД"
        http.setOnEditorActionListener { _, actionId, _ ->
            if (actionId != EditorInfo.IME_ACTION_GO) return@setOnEditorActionListener false
            hideKeyboard(http)
            url_http = http.text.toString()
            // Проверять что ввели на наличие в сети и если есть то Зовем Jasonette
            // Если в сети нет то зовем Фрагмент RV-Vertical для перезаполнения его Google Search
            var httpEdit :String?
            httpEdit = http.text.toString()
            // Строится лист запросов для PING
            val Zapros = listTryHttp(httpEdit)
            // PING первым способом
            httpEdit = tryHttp1(Zapros)
            if (httpEdit !=null) { // сайт пингуется
                http.setText(httpEdit) ; url_http = httpEdit
                Toast.makeText(applicationContext, "tryHttp1 Сайт доступен $httpEdit", Toast.LENGTH_SHORT).show()
                DoHttp(this,url_http)
                return@setOnEditorActionListener false
            } else {   // сайт НЕ пингуется
                Toast.makeText(applicationContext, "tryHttp1 Сайт НЕ доступен  ", Toast.LENGTH_SHORT).show()
            }
            // PING вторым способом
            httpEdit = tryHttp2(Zapros)
            if (httpEdit !=null) { // сайт пингуется
                http.setText(httpEdit) ; url_http = httpEdit
                Toast.makeText(applicationContext, "!!!!!!!!Сайт доступен  $httpEdit", Toast.LENGTH_SHORT).show()
                DoHttp(this,url_http)
                return@setOnEditorActionListener false
            } else {
                Toast.makeText(applicationContext, "tryHttp2 Сайт НЕ доступен####### ", Toast.LENGTH_SHORT).show()
            }
            // Если сайт не нашелся то это запрос на поиск к фрвгменту RV_Fragment
            Toast.makeText(applicationContext, "+++++++++++ Вызов поиска $url_http для фрагмента +++++++++ ", Toast.LENGTH_LONG).show()
            // Вызов GOOGLE Search
            val searchQuery = url_http.toString()  //The query to search
            mListener!!.onSearch(searchQuery)

            return@setOnEditorActionListener false
        }
    }

    // Создается меню с тремя точками в тулбар недоделано
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    // Обработка выбора в меню с тремя точками - тем более не доделано
    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            R.id.action_settings -> true
            R.id.action_invois -> true
            R.id.action_chat -> true
            R.id.action_order -> true

            R.id.action_template1 -> { urltemplate = getString(R.string.template1) ; true }
            R.id.action_template2 -> { urltemplate = getString(R.string.template2) ; true }
            R.id.action_template3 -> { urltemplate = getString(R.string.template3) ; true }

            else -> super.onOptionsItemSelected(item)
        }
    private fun hideKeyboard(view: View) {
        // прячем клавиатуру. view - это кнопка
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
    }
    fun isConnected(): Boolean {
        val ni = (getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager)
            .activeNetworkInfo
        return ni != null && ni.isConnected
    }

}

