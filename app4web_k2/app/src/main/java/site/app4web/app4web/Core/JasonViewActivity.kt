package site.app4web.app4web.Core

import android.app.SearchManager
import android.content.*
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.util.*
import android.view.*
import android.view.View.OnTouchListener
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.inputmethod.InputMethodManager
import android.webkit.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback
import androidx.core.view.MenuItemCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnItemTouchListener
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.aurelhubert.ahbottomnavigation.AHBottomNavigation
import com.aurelhubert.ahbottomnavigation.AHBottomNavigationItem
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.GlideDrawable
import com.bumptech.glide.request.animation.GlideAnimation
import com.bumptech.glide.request.target.SimpleTarget
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import site.app4web.app4web.Component.JasonComponentFactory
import site.app4web.app4web.Component.JasonImageComponent
import site.app4web.app4web.Core.JasonParser.JasonParserListener
import site.app4web.app4web.Core.JasonViewActivity
import site.app4web.app4web.Helper.JasonHelper
import site.app4web.app4web.Launcher.Launcher
import site.app4web.app4web.Lib.JasonToolbar
import site.app4web.app4web.Lib.MaterialBadgeTextView
import site.app4web.app4web.R
import site.app4web.app4web.Section.ItemAdapter
import site.app4web.app4web.Service.agent.JasonAgentService
import site.app4web.app4web.Service.vision.JasonVisionService
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

open class JasonViewActivity : AppCompatActivity(), OnRequestPermissionsResultCallback {
    // ActivityCompat.requestPermissions(Activity activity, String[] permissions, int requestCode)
    // - запрос разрешений от пользователя
    private var toolbar: JasonToolbar? = null
    private var listView: RecyclerView? = null
    var url: String? = null
    var model: JasonModel? = null
    var preload: JSONObject? = null
    private var loading: ProgressBar? = null
    var depth: Int? = null
    private var listViewOnItemTouchListeners: ArrayList<OnItemTouchListener>? = null
    private var firstResume = true
    var loaded = false
    private var fetched = false
    private var resumed = false
    private var header_height = 0
    private var logoView: ImageView? = null
    private var section_items: ArrayList<JSONObject>? = null
    private var bottomNavigationItems: HashMap<Int, AHBottomNavigationItem>? = null
    var modules: HashMap<String?, Any?>? = null
    private var swipeLayout // «резинка от трусов»  «потяни, чтобы обновить»
            : SwipeRefreshLayout? = null
    var sectionLayout: LinearLayout? = null
    var rootLayout: RelativeLayout? = null
    private var adapter: ItemAdapter? = null
    var backgroundCurrentView: View? = null
    var backgroundWebview: WebView? = null
    var backgroundImageView: ImageView? = null
    private var backgroundCameraView: SurfaceView? = null
    var cameraManager: JasonVisionService? = null
    private var bottomNavigation: AHBottomNavigation? = null
    private var footerInput: LinearLayout? = null
    private var footer_input_textfield: View? = null
    private var searchView: SearchView? = null
    private var divider: HorizontalDividerItemDecoration? = null
    private var previous_background: String? = null
    private var launch_action: JSONObject? = null
    private var event_queue: ArrayList<JSONObject>? = null
    var layer_items: ArrayList<View>? = null
    var focusView: View? = null
    var listState: Parcelable? = null
    var intent_to_resolve: JSONObject? = null
    var agents = JSONObject()
    private var isexecuting = false
    // 150-250 строки настройка макетов в 5 шагов строим контейнеры в контейнерах JasonObject и 6 шагом высвечиваем все это на пустой экран
    // 260-320 строки Разбор намерений Intent "href" или "action" + "url" + "depth" + "preload"
    // 320-360 строки  Создать модель в частности восстановить из save
    // Конец onCreate
    // 380-680 setup_agents ; clear_agents ; onRefresh; onSwitchTab ; onPause ; onResume ; onActivityResult ; onSaveInstanceState
    // 680-770 Обработчики событий. Правило ver2. : onShow ; onLoad
    //  1128 диспетчер JASON ACTION: call ; final_call ; trigger ; invoke_lambda ; simple_trigger ; exec
    //  -1200     BroadcastReceiver onSuccess , onError , onCall   ;   private JSONObject addToObject
    //  1230-   JASON CORE ACTION API   13 проц
    //  1843    JASON VIEW              11 проц
    //  3171    Слушатели событий        3 проц
    //  3230     Конец  JasonViewActivity
    /*************************************************************
     *
     * JASON ACTIVITY LIFECYCLE MANAGEMENT
     *
     * JASON УПРАВЛЕНИЕ ЖИЗНЕННЫМ ЦИКЛОМ ACTIVITY
     *
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loaded = false
        event_queue = ArrayList()

        // Initialize Parser instance
        // Инициализируем экземпляр Parser - static Core.JasonParser - new JasonParser
        JasonParser.Companion.getInstance(this)
        listViewOnItemTouchListeners = ArrayList()
        layer_items = ArrayList()
        // Setup Layouts
        // Настройка макетов

        // 1. Create root layout (Relative Layout)
        // 1. Создать корневой макет (Relative Layout)
        val rlp = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.MATCH_PARENT
        )
        if (rootLayout == null) {
            // Create the root layout
            // Создаем корневой макет если его еще нет
            rootLayout = RelativeLayout(this@JasonViewActivity)
            rootLayout!!.layoutParams = rlp // будет занимать все доступное ему пространство.
            rootLayout!!.fitsSystemWindows = true //  оставить пространство для системных окон
        }


        // 2. Add Swipe layout
        // 2. Добавить разметку Swipe -- «резинка от трусов»  «потяни, чтобы обновить»
        //  пользователь хочет часто обновлять, и может это сделать, просто потянув контент жестом вниз, а потом отпустив
        // https://habr.com/post/218365/
        if (swipeLayout == null) {
            swipeLayout = SwipeRefreshLayout(this@JasonViewActivity)
            swipeLayout!!.layoutParams = rlp
            rootLayout!!.addView(swipeLayout)
        }

        // 3. Create body.header
        // 3. Создать body.заголовок  Lib.JasonToolbar
        if (toolbar == null) {
            toolbar = JasonToolbar(this)
            setSupportActionBar(toolbar) // Этот метод назначает Toolbar выполнять функции ActonBar
            supportActionBar!!.setTitle("")
        } else {
            setSupportActionBar(toolbar)
        }

        // 4. Create body.sections
        // 4. Создаем body.sections

        // 4.1. RecyclerView
        // 4.1. androidx.RecyclerView - позволяет повысить производительность по сравнению со стандартным ListView.
        // работает через RecyclerView.Adapter см ниже,  установить атрибут app:layoutManager см ниже
        listView = RecyclerView(this)
        listView!!.setItemViewCacheSize(20)
        listView!!.isDrawingCacheEnabled = true
        listView!!.setHasFixedSize(true)

        // Create adapter passing in the sample user data
        // Создание адаптера, передающего пример пользовательских данных
        adapter = ItemAdapter(this, this, ArrayList())
        // Attach the adapter to the recyclerview to populate items
        // Прикрепите адаптер к представлению переработчика, чтобы заполнить элементы
        listView!!.adapter = adapter
        // Set layout manager to position the items
        // Установить менеджер макета для размещения элементов
        listView!!.layoutManager = LinearLayoutManager(this)

        // 4.2. LinearLayout
        // 4.2. LinearLayout
        if (sectionLayout == null) {
            // Create LinearLayout
            // Создать LinearLayout
            sectionLayout = LinearLayout(this@JasonViewActivity)
            sectionLayout!!.orientation = LinearLayout.VERTICAL
            val p = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            sectionLayout!!.layoutParams = p

            // Add toolbar to LinearLayout
            // Добавить панель инструментов в LinearLayout
            if (toolbar != null) sectionLayout!!.addView(toolbar)

            // Add RecyclerView to LinearLayout
            // Добавить RecyclerView в LinearLayout
            if (listView != null) sectionLayout!!.addView(listView)

            // Add LinearLayout to Swipe Layout
            // Добавить LinearLayout в Swipe Layout
            swipeLayout!!.addView(sectionLayout)
        }
        // В LinearLayout загружаем toolbar + RecyclerView и оборачиаем уго (LinearLayout) в «резинка от трусов»
        // Смысл 4 пункта: Создаем body.sections


        // 5. Start Loading
        // 5. Начало загрузки
        val loadingLayoutParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        )
        loadingLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)
        loading = ProgressBar(this)
        loading!!.layoutParams = loadingLayoutParams
        loading!!.visibility = View.INVISIBLE
        rootLayout!!.addView(loading)

        // 6. set root layout as content
        // 6. установить корневой макет как контент т.е. высветить все это наконец на телефон
        setContentView(rootLayout)
        firstResume = true
        modules = HashMap()


        // ***********************************************************************************************

        // Parsing Intent
        // Разбор намерений которые пришли наверное через вызов JasonViewActivity
        val intent = intent

        // Launch Action Payload Handling.
        // We will store this and queue it up at onLoad() after the first action call chain has finished
        // And then execute it on "unlock" of that call chain
        // Запустить действие Payload Handling. - хранение и доставка полезной нагрузки.- Обработка Больших Нагрузок
        // Мы будем хранить это и ставить в очередь в onLoad ()
        // после завершения цепочки вызовов первого действия
        // И затем выполнить его при «разблокировке» этой цепочки вызовов
        // т.е. это загрузка в фоновом режиме с экономией памяти в потоках
        launch_action = null
        if (intent.hasExtra("href")) {    // hypertext reference — гипертекстовая ссылка - похоже берет JSONObject
            try {
                val href = JSONObject(intent.getStringExtra("href"))
                launch_action = JSONObject()
                launch_action!!.put("type", "\$href")
                launch_action!!.put("options", href)
            } catch (e: Exception) {
                Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
            }
        } else if (intent.hasExtra("action")) {
            try {
                launch_action = JSONObject(intent.getStringExtra("action"))
            } catch (e: Exception) {
            }
        }
        url = if (intent.hasExtra("url")) {
            intent.getStringExtra("url")
        } else {
            getString(R.string.url)
        }
        depth = intent.getIntExtra("depth", 0)
        preload = null
        if (intent.hasExtra("preload")) {
            preload = try {
                JSONObject(intent.getStringExtra("preload"))
            } catch (e: Exception) {
                Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
                null
            }
        } else {
            if (intent.hasCategory(Intent.CATEGORY_LAUNCHER)) {
                // first time launch
                // первый запуск
                val launch_url = getString(R.string.launch)
                preload = if (launch_url != null && launch_url.length > 0) {
                    // if preload is specified, use that url
                    // если указана предварительная загрузка, используйте этот URL
                    JasonHelper.read_json(launch_url, this@JasonViewActivity) as JSONObject
                } else {
                    null
                }
            }
        }
        //  *********************************************************************************************
        // Create model
        // Создать модель
        model = JasonModel(url, intent, this)
        val uri = getIntent().data
        if (uri != null && uri.host!!.contains("oauth")) {
            loaded = true
            // in case of oauth process we need to set loaded to true
            // since we know it's already been loaded.
            // в случае oauth-процесса нам нужно установить значение true для загруженного,
            // поскольку мы знаем, что оно уже загружено.
            return
        }
        if (savedInstanceState != null) {
            // Restore model and url
            // Восстановить модель и URL
            // Then rebuild the view
            // Затем перестраиваем view
            try {
                url = savedInstanceState.getString("url")
                model!!.url = url
                if (savedInstanceState.getString("jason") != null) model!!.jason =
                    JSONObject(savedInstanceState.getString("jason"))
                if (savedInstanceState.getString("rendered") != null) model!!.rendered =
                    JSONObject(savedInstanceState.getString("rendered"))
                if (savedInstanceState.getString("state") != null) model!!.state =
                    JSONObject(savedInstanceState.getString("state"))
                if (savedInstanceState.getString("var") != null) model!!.`var` =
                    JSONObject(savedInstanceState.getString("var"))
                if (savedInstanceState.getString("cache") != null) model!!.cache =
                    JSONObject(savedInstanceState.getString("cache"))
                if (savedInstanceState.getString("params") != null) model!!.params =
                    JSONObject(savedInstanceState.getString("params"))
                if (savedInstanceState.getString("session") != null) model!!.session =
                    JSONObject(savedInstanceState.getString("session"))
                listState = savedInstanceState.getParcelable("listState")
                setup_body(model!!.rendered)
            } catch (e: Exception) {
                Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
            }
        } else {
            onRefresh()
        }
    } // end onCreate

    // *****************************************************************************************
    private fun setup_agents() {
        try {
            val head = model!!.jason!!.getJSONObject("\$jason").getJSONObject("head")
            if (head.has("agents")) {
                val agents = head.getJSONObject("agents")
                val iterator = agents.keys()
                while (iterator.hasNext()) {
                    val key = iterator.next()
                    runOnUiThread {
                        try {
                            val agentService =
                                (applicationContext as Launcher).services["JasonAgentService"] as JasonAgentService
                            val agent = agentService.setup(
                                this@JasonViewActivity,
                                agents.getJSONObject(key),
                                key
                            )
                        } catch (e: JSONException) {
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    }

    private fun clear_agents() {
        try {
            val head = model!!.jason!!.getJSONObject("\$jason").getJSONObject("head")
            val agents = head.getJSONObject("agents")
            val iterator = agents.keys()
            while (iterator.hasNext()) {
                val key = iterator.next()
                runOnUiThread {
                    try {
                        val agentService =
                            (applicationContext as Launcher).services["JasonAgentService"] as JasonAgentService
                        val clearAction = JSONObject()
                        val options = JSONObject()
                        options.put("id", key)
                        clearAction.put("options", options)
                        agentService.clear(clearAction, this@JasonViewActivity)
                    } catch (e: JSONException) {
                    }
                }
            }
        } catch (e: Exception) {
        }
    }

    private fun onRefresh() {
        // offline: true logic
        // 1. check if the url + params signature exists
        // 2. if it does, use that to construct the model and setup_body
        // 3. Go on to fetching (it will be re-rendered if fetch is successful)
        // offline: истинная логика
        // 1. проверить, существует ли подпись url + params
        // 2. если это так, используйте это для построения модели и setup_body
        // 3. Переходим к извлечению (оно будет перерисовано при успешном извлечении)

        // reset "offline mode"
        // сбросить «автономный режим»
        model!!.offline = false

        // Reset local variables when reloading
        // Сбрасываем локальные переменные при перезагрузке
        model!!.`var` = JSONObject()
        val pref = getSharedPreferences("offline", 0)
        val signature = model!!.url + model!!.params.toString()
        if (pref.contains(signature)) {
            val offline = pref.getString(signature, null)
            try {
                val offline_cache = JSONObject(offline)
                model!!.jason = offline_cache.getJSONObject("jason")
                model!!.rendered = offline_cache.getJSONObject("rendered")
                model!!.offline =
                    true // we confirm that this model is offline so it shouldn't trigger error.json when network fails
                setup_body(model!!.rendered)
            } catch (e: Exception) {
                Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
            }
        } else {
            if (preload != null) {
                setup_body(preload)
            }
        }

        // Fetch
        // Выборка
        model!!.fetch()
    }

    private fun onSwitchTab(newUrl: String, newParams: String, intent: Intent) {
        // if tab transition, restore from stored tab using this.build()
        // если переход с вкладки, восстановить с сохраненной вкладки используя this.build ()
        try {
            // remove all touch listeners before replacing
            // удалить все сенсорные слушатели перед заменой)
            // Use case : Tab bar
            // Вариант использования: панель вкладок
            removeListViewOnItemTouchListeners()
            // Store the current model
            // Сохранить текущую модель
            (applicationContext as Launcher).setTabModel(model!!.url + model!!.params, model)

            // clear agents
            // очистить агентов
            clear_agents()

            // Retrieve the new view's model
            // Получить модель нового вида
            val m = (applicationContext as Launcher).getTabModel(newUrl + newParams)
            if (m == null) {
                // refresh
                removeListViewOnItemTouchListeners()
                model = JasonModel(newUrl, intent, this)
                onRefresh()
            } else {
                // build
                model = m
                setup_agents()
                setup_body(m.rendered)
            }
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    }

    override fun onPause() {
        // Unregister since the activity is paused.
        // Отмена регистрации, поскольку действие приостановлено.
        LocalBroadcastManager.getInstance(this).unregisterReceiver(onSuccess)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(onError)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(onCall)

        // Clear agents
        // Очистить агентов
        clear_agents()


        // Store model to shared preference
        // Сохраняем модель по общему предпочтению
        val pref = getSharedPreferences("model", 0)
        val editor = pref.edit()
        val temp_model = JSONObject()
        try {
            if (model!!.url != null) temp_model.put("url", model!!.url)
            if (model!!.jason != null) temp_model.put("jason", model!!.jason)
            if (model!!.rendered != null) temp_model.put("rendered", model!!.rendered)
            if (model!!.state != null) temp_model.put("state", model!!.state)
            if (model!!.`var` != null) temp_model.put("var", model!!.`var`)
            if (model!!.cache != null) temp_model.put("cache", model!!.cache)
            if (model!!.params != null) temp_model.put("params", model!!.params)
            if (model!!.session != null) temp_model.put("session", model!!.session)
            if (model!!.action != null) temp_model.put("action", model!!.action)
            temp_model.put("depth", depth)
            if (model!!.url != null) {
                editor.putString(model!!.url, temp_model.toString())
                editor.commit()
            }
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
        super.onPause()
    }

    override fun onResume() {
        // Register to receive messages.
        // We are registering an observer (mMessageReceiver) to receive Intents
        // with actions named "custom-event-name".
        // Регистрация для получения сообщений.
        // Мы регистрируем наблюдателя (mMessageReceiver) для получения Intents
        // с действиями с именем "custom-event-name".
        Launcher.Companion.setCurrentContext(this)
        LocalBroadcastManager.getInstance(this).registerReceiver(onSuccess, IntentFilter("success"))
        LocalBroadcastManager.getInstance(this).registerReceiver(onError, IntentFilter("error"))
        LocalBroadcastManager.getInstance(this).registerReceiver(onCall, IntentFilter("call"))
        resumed = true
        val pref = getSharedPreferences("model", 0)
        if (model!!.url != null && pref.contains(model!!.url)) {
            val str = pref.getString(model!!.url, null)
            try {
                val temp_model = JSONObject(str)
                if (temp_model.has("url")) model!!.url = temp_model.getString("url")
                if (temp_model.has("jason")) model!!.jason = temp_model.getJSONObject("jason")
                if (temp_model.has("rendered")) model!!.rendered =
                    temp_model.getJSONObject("rendered")
                if (temp_model.has("state")) model!!.state = temp_model.getJSONObject("state")
                if (temp_model.has("var")) model!!.`var` = temp_model.getJSONObject("var")
                if (temp_model.has("cache")) model!!.cache = temp_model.getJSONObject("cache")
                if (temp_model.getInt("depth") == depth) {
                    if (temp_model.has("params")) model!!.params =
                        temp_model.getJSONObject("params")
                }
                if (temp_model.has("session")) model!!.session = temp_model.getJSONObject("session")
                if (temp_model.has("action")) model!!.action = temp_model.getJSONObject("action")

                // Delete shared preference after resuming
                val editor = pref.edit()
                editor.remove(model!!.url)
                editor.commit()
            } catch (e: Exception) {
                Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
            }
        }
        if (!firstResume) {
            onShow()
        }
        firstResume = false
        val uri = intent.data
        if (uri != null && uri.host!!.contains("oauth")) {
            try {
                intent_to_resolve = JSONObject()
                intent_to_resolve!!.put("type", "success")
                intent_to_resolve!!.put("name", "oauth")
                intent_to_resolve!!.put("intent", intent)
            } catch (e: JSONException) {
                Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
            }
        }

        // Intent Handler
        // This part is for handling return values from external Intents triggered
        // We set "intent_to_resolve" from onActivityResult() below, and then process it here.
        // It's because onCall/onSuccess/onError callbacks are not yet attached when onActivityResult() is called.
        // Need to wait till this point.
        // Intent Handler
        // Эта часть предназначена для обработки возвращаемых значений из внешних сработавших Intents
        // Мы устанавливаем "intent_to_resolve" из onActivityResult () ниже, а затем обрабатываем его здесь.
        // Это потому, что обратные вызовы onCall / onSuccess / onError еще не присоединены, когда вызывается onActivityResult ().
        // Нужно подождать до этого момента.
        try {
            if (intent_to_resolve != null) {
                if (intent_to_resolve!!.has("type")) {
                    (applicationContext as Launcher).trigger(
                        intent_to_resolve,
                        this@JasonViewActivity
                    )
                    intent_to_resolve = null
                }
            }
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
        super.onResume()
        if (listState != null) {
            listView!!.layoutManager!!.onRestoreInstanceState(listState)
        }
    }

    // This gets executed automatically when an external intent returns with result
    // Это выполняется автоматически, когда внешнее намерение возвращается с результатом
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        try {
            // We can't process the intent here because
            // we need to wait until onResume gets triggered (which comes after this callback)
            // onResume reattaches all the onCall/onSuccess/onError callbacks to the current Activity
            // so we need to wait until that happens.
            // Therefore here we only set the "intent_to_resolve", and the actual processing is
            // carried out inside onResume()
            // Мы не можем обработать намерение здесь, потому что
            // нам нужно дождаться срабатывания onResume (что происходит после этого обратного вызова)
            // onResume повторно присоединяет все обратные вызовы onCall / onSuccess / onError к текущей операции
            // поэтому нам нужно подождать, пока это не произойдет.
            // Поэтому здесь мы только устанавливаем "intent_to_resolve", и фактическая обработка
            // выполняется внутри onResume ()
            intent_to_resolve = JSONObject()
            if (resultCode == RESULT_OK) {
                intent_to_resolve!!.put("type", "success")
                intent_to_resolve!!.put("name", requestCode)
                intent_to_resolve!!.put("intent", intent)
            } else {
                intent_to_resolve!!.put("type", "error")
                intent_to_resolve!!.put("name", requestCode)
            }
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        if (model!!.url != null) savedInstanceState.putString("url", model!!.url)
        if (model!!.jason != null) savedInstanceState.putString("jason", model!!.jason.toString())
        if (model!!.rendered != null) savedInstanceState.putString(
            "rendered",
            model!!.rendered.toString()
        )
        if (model!!.state != null) savedInstanceState.putString("state", model!!.state.toString())
        if (model!!.`var` != null) savedInstanceState.putString("var", model!!.`var`.toString())
        if (model!!.cache != null) savedInstanceState.putString("cache", model!!.cache.toString())
        if (model!!.params != null) savedInstanceState.putString(
            "params",
            model!!.params.toString()
        )
        if (model!!.session != null) savedInstanceState.putString(
            "session",
            model!!.session.toString()
        )
        if (model!!.action != null) savedInstanceState.putString(
            "action",
            model!!.action.toString()
        )

        // Store RecyclerView state
        // Сохраняем состояние RecyclerView
        listState = listView!!.layoutManager!!.onSaveInstanceState()
        savedInstanceState.putParcelable("listState", listState)
        super.onSaveInstanceState(savedInstanceState)
    }

    /*************************************************************
     *
     * ## Event Handlers Rule ver2.
     * ## Обработчики событий. Правило ver2.
     *
     * 1. When there's only $show handler
     * - $show: Handles both initial load and subsequent show events
     * 1. Когда есть только $show обработчик
     * - $show: обрабатывает как начальную загрузку, так и последующие шоу события
     *
     * 2. When there's only $load handler
     * - $load: Handles Only the initial load event
     * 2. Когда есть только $load handler
     * - $load: обрабатывает только начальное событие загрузки
     *
     * 3. When there are both $show and $load handlers
     * - $load : handle initial load only
     * - $show : handle subsequent show events only
     * 3. Когда есть и обработчики $show и $load
     * - $load: обрабатывать только начальную загрузку
     * - $show: обрабатывать только последующие события шоу
     *
     *
     * ## Summary
     * ## Резюме
     *
     * $load:
     * - triggered when view loads for the first time.
     * $show:
     * - triggered at load time + subsequent show events (IF $load handler doesn't exist)
     * - NOT triggered at load time BUT ONLY at subsequent show events (IF $load handler exists)
     * $load::
     * - срабатывает при первом просмотре загрузок.
     * $show:
     * - срабатывает во время загрузки + последующие события показа (IF $обработчик загрузки не существует)
     * - НЕ запускается во время загрузки, НО ТОЛЬКО при последующих событиях шоу (IF $обработчик загрузки существует)
     *
     *
     */
    fun onShow() {
        loaded = true
        try {
            val head = model!!.jason!!.getJSONObject("\$jason").getJSONObject("head")
            val events = head.getJSONObject("actions")
            simple_trigger("\$show", JSONObject(), this)
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    }

    fun onLoad() {
        loaded = true
        simple_trigger("\$load", JSONObject(), this)
        try {
            val head = model!!.jason!!.getJSONObject("\$jason").getJSONObject("head")
            val events = head.getJSONObject("actions")
            if (events != null && events.has("\$load")) {
                // nothing
            } else {
                onShow()
            }
            if (launch_action != null) {
                val copy = JSONObject(launch_action.toString())
                launch_action = null
                if (head.has("actions")) {
                    model!!.jason!!.getJSONObject("\$jason").getJSONObject("head")
                        .getJSONObject("actions").put("\$launch", copy)
                } else {
                    val actions = JSONObject()
                    actions.put("\$launch", copy)
                    model!!.jason!!.getJSONObject("\$jason").getJSONObject("head")
                        .put("actions", actions)
                }
                simple_trigger("\$launch", JSONObject(), this@JasonViewActivity)
            }
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    }

    fun onForeground() {
        // Not implemented yet
    }

    /*************************************************************
     *
     * JASON ACTION DISPATCH
     * диспетчер JASON ACTION
     *
     */
    // How action calls work:
    // 1. First need to resolve the action in case the root level is an array => This means it's an if statment and needs to be parsed once before going forward.
    //      if array => "call" method parses the action once and then calls final_call
    //      else     => "call" immediately calls final_call
    // 2. Then need to parse the "option" part of the action, so that the options will have been filled in. (But NOT success and error, since they need to be parsed AFTER the current action is over)
    //
    // 3. Only then, we actually make the invocation.
    // Как работают вызовы действий:
    // 1. Сначала нужно разрешить действие в случае, если корневым уровнем является массив => Это означает, что это статическое условие if, и его нужно проанализировать один раз, прежде чем идти вперед.
    // if array => метод "call" анализирует действие один раз, а затем вызывает final_call
    // else => "call" немедленно вызывает final_call
    // 2. Затем необходимо проанализировать часть действия «option», чтобы опции были заполнены. (Но НЕ в случае успеха и ошибки, так как они должны быть проанализированы ПОСЛЕ окончания текущего действия)
    //
    // 3. Только тогда мы на самом деле делаем вызов.
    //public void call(final Object action, final JSONObject data, final Context context) {
    fun call(action_json: String?, data_json: String?, event_json: String?, context: Context) {
        try {
            val action = JasonHelper.objectify(action_json)
            val data = JasonHelper.objectify(data_json) as JSONObject
            val ev: JSONObject
            ev = try {
                JasonHelper.objectify(event_json) as JSONObject
            } catch (e: Exception) {
                JSONObject()
            }
            model!!["state"] = data
            if (action is JSONArray) {
                // resolve
                JasonParser.Companion.getInstance(this)!!
                    .setParserListener(JasonParserListener { reduced_action ->
                        final_call(
                            reduced_action,
                            data,
                            ev,
                            context
                        )
                    })
                JasonParser.Companion.getInstance(this)!!
                    .parse("json", model!!.state, action, context)
            } else {
                final_call(action as JSONObject, data, ev, context)
            }
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    }

    private fun final_call(
        action: JSONObject?,
        data: JSONObject,
        event: JSONObject?,
        context: Context
    ) {
        try {
            if (action.toString().equals("{}", ignoreCase = true)) {
                // no action to execute
                unlock(JSONObject(), JSONObject(), JSONObject(), context)
                return
            }

            // Handle trigger first
            // Обрабатываем триггер первым
            if (action!!.has("trigger")) {
                trigger(action, data, event, context)
            } else {
                if (action.length() == 0) {
                    return
                }
                // If not trigger, regular call
                // Если не триггер, обычный вызов
                if (action.has("options")) {
                    // if action has options, we need to parse out the options first
                    val options = action["options"]
                    JasonParser.Companion.getInstance(this)!!
                        .setParserListener(JasonParserListener { parsed_options ->
                            try {
                                val action_with_parsed_options = JSONObject(action.toString())
                                action_with_parsed_options.put("options", parsed_options)
                                exec(action_with_parsed_options, model!!.state, event, context)
                            } catch (e: Exception) {
                                Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
                            }
                        })
                    JasonParser.Companion.getInstance(this)!!
                        .parse("json", model!!.state, options, context)
                } else {
                    // otherwise we can just call immediately
                    // в противном случае мы можем просто позвонить немедленно
                    exec(action, model!!.state, event, context)
                }
            }
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    }

    private fun trigger(
        action: JSONObject?,
        data: JSONObject,
        event: JSONObject?,
        context: Context
    ) {
        /****************************************************************************************
         *
         * This method is a syntactic sugar for calling a $lambda action.
         * Этот метод является синтаксическим сахаром для вызова действия $ lambda.
         * The syntax is as follows:
         * Синтаксис выглядит следующим образом:
         *
         * {
         * "trigger": "twitter.get",
         * "options": {
         * "endpoint": "timeline"
         * },
         * "success": {
         * "type": "$render"
         * },
         * "error": {
         * "type": "$util.toast",
         * "options": {
         * "text": "Uh oh. Something went wrong"
         * }
         * }
         * }
         *
         * Above is a syntactic sugar for the below "$lambda" type action call:
         * Выше приведен синтаксический сахар для вызова действия типа «$lambda»:
         *
         * $lambda action is a special purpose action that triggers another action by name and waits until it returns.
         * Действие $ lambda - это действие специального назначения, которое запускает другое действие по имени и ожидает его возврата.
         * This way we can define a huge size action somewhere and simply call them as a subroutine and wait for its return value.
         * Таким образом, мы можем определить где-нибудь действие огромного размера и просто вызвать их как подпрограмму и ждать ее возвращаемого значения.
         * When the subroutine (the action that was triggered by name) returns via `"type": "$return.success"` action,
         * Когда подпрограмма (действие, которое было вызвано по имени) возвращается через `" type ":" $ return.success "` action,
         * the $lambda action picks off where it left off and starts executing its "success" action with the value returned from the subroutine.
         * действие $ lambda начинается там, где оно остановилось, и начинает выполнение своего действия «success» со значением, возвращаемым из подпрограммы.
         *
         * Notice that:
         * 1. we get rid of the "trigger" field and turn it into a regular action of `"type": "$lambda"`.
         * 2. the "trigger" value (`"twitter.get"`) gets mapped to "options.name"
         * 3. the "options" value (`{"endpoint": "timeline"}`) gets mapped to "options.options"
         * Заметить, что:
         *           1. мы избавляемся от поля «триггер» и превращаем его в обычное действие «type»: «$ lambda» `.
         *           2. значение "trigger" (`" twitter.get "`) отображается на "options.name"
         *           3. значение "options" (`{" endpoint ":" timeline "}`) сопоставляется с "options.options"
         *
         * {
         * "type": "$lambda",
         * "options": {
         * "name": "twitter.get",
         * "options": {
         * "endpoint": "timeline"
         * }
         * },
         * "success": {
         * "type": "$render"
         * },
         * "error": {
         * "type": "$util.toast",
         * "options": {
         * "text": "Uh oh. Something went wrong"
         * }
         * }
         * }
         *
         * The success / error actions get executed AFTER the triggered action has finished and returns with a return value.
         * Действия «успех / ошибка» выполняются ПОСЛЕ того, как запущенное действие завершено и возвращается с возвращаемым значением.
         *
         */
        try {

            // construct options
            if (action!!.has("options")) {
                val options = action["options"]
                JasonParser.Companion.getInstance(this)!!
                    .setParserListener(JasonParserListener { parsed_options ->
                        try {
                            invoke_lambda(action, data, parsed_options, context)
                        } catch (e: Exception) {
                            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
                        }
                    })
                JasonParser.Companion.getInstance(this)!!
                    .parse("json", model!!.state, options, context)
            } else {
                val options = JSONObject()
                invoke_lambda(action, data, null, context)
            }
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    }

    private fun invoke_lambda(
        action: JSONObject?,
        data: JSONObject,
        options: JSONObject?,
        context: Context
    ) {
        try {
            // construct lambda
            // построить лямбду
            val lambda = JSONObject()
            lambda.put("type", "\$lambda")
            val args = JSONObject()
            args.put("name", action!!.getString("trigger"))
            if (options != null) {
                args.put("options", options)
            }
            lambda.put("options", args)
            if (action.has("success")) {
                lambda.put("success", action["success"])
            }
            if (action.has("error")) {
                lambda.put("error", action["error"])
            }
            call(lambda.toString(), data.toString(), "{}", context)
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    }

    fun simple_trigger(event_name: String?, data: JSONObject, context: Context) {
        try {
            isexecuting = if ((isexecuting || !resumed) && event_queue!!.size > 0) {
                val event_store = JSONObject()
                event_store.put("event_name", event_name)
                event_store.put("data", data)
                event_queue!!.add(event_store)
                return
            } else {
                true
            }
            val head = model!!.jason!!.getJSONObject("\$jason").getJSONObject("head")
            val events = head.getJSONObject("actions")
            // Look up an action by event_name
            if (events.has(event_name)) {
                val action = events[event_name]
                call(action.toString(), data.toString(), "{}", context)
            } else {
                unlock(JSONObject(), JSONObject(), JSONObject(), this@JasonViewActivity)
            }
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    }

    private fun exec(action: JSONObject?, data: JSONObject?, event: JSONObject?, context: Context) {
        try {
            val type = action!!.getString("type")
            if (type.startsWith("$") || type.startsWith("@")) {
                val tokens = type.split("\\.").toTypedArray()
                val className: String
                val fileName: String
                val methodName: String
                if (tokens.size == 1) {
                    // Core
                    methodName = type.substring(1)
                    val method = JasonViewActivity::class.java.getMethod(
                        methodName,
                        JSONObject::class.java,
                        JSONObject::class.java,
                        JSONObject::class.java,
                        Context::class.java
                    )
                    method.invoke(this, action, model!!.state, event, context)
                } else {
                    className = type.substring(1, type.lastIndexOf('.'))


                    // Resolve classname by looking up the json files
                    // Разрешаем имя класса, просматривая файлы json
                    var resolved_classname: String? = null
                    var jr: String? = null
                    try {
                        val `is` = assets.open("file/$$className.json")
                        val size = `is`.available()
                        val buffer = ByteArray(size)
                        `is`.read(buffer)
                        `is`.close()
                        jr = String(buffer, StandardCharsets.UTF_8)
                        val jrjson = JSONObject(jr)
                        if (jrjson.has("classname")) {
                            resolved_classname = jrjson.getString("classname")
                        }
                    } catch (e: Exception) {
                        Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
                    }
                    fileName = if (resolved_classname != null) {
                        "site.app4web.app4web.Action.$resolved_classname"
                    } else {
                        "site.app4web.app4web.Action.Jason" + className.toUpperCase()[0] + className.substring(
                            1
                        ) + "Action"
                    }
                    methodName = type.substring(type.lastIndexOf('.') + 1)

                    // Look up the module registry to see if there's an instance already
                    // 1. If there is, use that
                    // 2. If there isn't:
                    //      A. Instantiate one
                    //      B. Add it to the registry
                    // Просмотр реестра модуля, чтобы увидеть, есть ли уже экземпляр
                    // 1. Если есть, используйте это
                    // 2. Если нет:
                    // A. Создать экземпляр
                    // B. Добавить его в реестр
                    val module: Any?
                    if (modules!!.containsKey(fileName)) {
                        module = modules!![fileName]
                    } else {
                        val classObject = Class.forName(fileName)
                        val constructor = classObject.getConstructor()
                        module = constructor.newInstance()
                        modules!![fileName] = module
                    }
                    val method = module!!.javaClass.getMethod(
                        methodName,
                        JSONObject::class.java,
                        JSONObject::class.java,
                        JSONObject::class.java,
                        Context::class.java
                    )
                    model!!.action = action
                    method.invoke(module, action, model!!.state, event, context)
                }
            }
        } catch (e: Exception) {
            // Action doesn't exist yet
            // Действие еще не существует
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
            try {
                val alert_action = JSONObject()
                alert_action.put("type", "\$util.banner")
                val options = JSONObject()
                options.put("title", "Not implemented")
                val type = action!!.getString("type")
                options.put("description", action.getString("type") + " is not implemented yet.")
                alert_action.put("options", options)
                call(alert_action.toString(), JSONObject().toString(), "{}", this@JasonViewActivity)
            } catch (e2: Exception) {
                Log.d("Warning", e2.stackTrace[0].methodName + " : " + e2.toString())
            }
        }
    }

    // Our handler for received Intents. This will be called whenever an Intent
    // with an action named "custom-event-name" is broadcasted.
    // Наш обработчик полученных Интентов. Это будет вызываться всякий раз, когда намерение
    // транслируется действие с именем «custom-event-name».
    var onSuccess: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            try {
                val action_string = intent.getStringExtra("action")
                val data_string = intent.getStringExtra("data")
                val event_string = intent.getStringExtra("event")

                // Wrap return value with $jason
                // Обернуть возвращаемое значение $jason
                val data = addToObject("\$jason", data_string)

                // call next
                // вызвать следующий
                call(action_string, data.toString(), event_string, this@JasonViewActivity)
            } catch (e: Exception) {
                Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
            }
        }
    }
    var onError: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            try {
                val action_string = intent.getStringExtra("action")
                val data_string = intent.getStringExtra("data")
                val event_string = intent.getStringExtra("event")

                // Wrap return value with $jason
                // Обернуть возвращаемое значение $jason
                val data = addToObject("\$jason", data_string)

                // call next
                // вызвать следующий
                call(action_string, data.toString(), event_string, this@JasonViewActivity)
            } catch (e: Exception) {
                Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
            }
        }
    }
    var onCall: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            try {
                val action_string = intent.getStringExtra("action")
                val event_string = intent.getStringExtra("event")
                var data_string = intent.getStringExtra("data")
                if (data_string == null) {
                    data_string = JSONObject().toString()
                }

                // Wrap return value with $jason
                // Обернуть возвращаемое значение $jason
                val data = addToObject("\$jason", data_string)

                // call next
                // вызвать следующий
                call(action_string, data.toString(), event_string, this@JasonViewActivity)
            } catch (e: Exception) {
                Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
            }
        }
    }

    private fun addToObject(prop: String, json_data: String?): JSONObject {
        var data = JSONObject()
        try {
            // Detect if the result is JSONObject, JSONArray, or String
            // Определить, является ли результат JSONObject, JSONArray или String
            data = if (json_data!!.trim { it <= ' ' }.startsWith("[")) {
                // JSONArray
                // JSONArray
                JSONObject().put("\$jason", JSONArray(json_data))
            } else if (json_data.trim { it <= ' ' }.startsWith("{")) {
                // JSONObject
                // JSONObject
                JSONObject().put("\$jason", JSONObject(json_data))
            } else {
                // String
                // Строка
                JSONObject().put("\$jason", json_data)
            }
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
        return data
    }
    /*************************************************************
     *
     * JASON CORE ACTION API
     * JASON CORE ACTION API
     *
     */
    /**
     * Renders a template using data
     * Визуализирует шаблон с использованием данных
     * @param {String} template_name - the name of the template to render
     * @param {JSONObject} data - the data object to render
     * @param {String} template_name - имя шаблона для отображения
     * @param {JSONObject} data - объект данных для рендеринга
     */
    fun lambda(action: JSONObject, data: JSONObject, event: JSONObject?, context: Context?) {

        /*

        # Similar to `trigger` keyword, but with a few differences:
        1. Trigger was just for one-off triggering and finish. Lambda waits until the subroutine returns and continues where it left off.
        2. `trigger` was a keyword, but lambda itself is just another type of action. `{"type": "$lambda"}`
        3. Lambda can pass arguments via `options`
        # Аналогично ключевому слову trigger, но с некоторыми отличиями:
        1. Триггер был только для одноразового запуска и финиша. Лямбда ждет, пока подпрограмма не вернется и продолжит с того места, где остановилась.
        2. «триггер» был ключевым словом, но сама лямбда - просто еще один тип действия. `{" type ":" $ lambda "}`
        3. Лямбда может передавать аргументы через `опции`

        # How it works
        1. Triggers another action by name
        2. Waits for the subroutine to return via `$return.success` or `$return.error`
        3. When the subroutine calls `$return.success`, continue executing from `success` action, using the return value from the subroutine
        4. When the subroutine calls `$return.error`, continue executing from `error` action, using the return value from the subroutine
        # Как это устроено
        1. Запускает другое действие по имени
        2. Ожидает возврата подпрограммы через `$ return.success` или` $ return.error`
        3. Когда подпрограмма вызывает `$ return.success`, продолжайте выполнение из действия` success`, используя возвращаемое значение из подпрограммы.
        4. Когда подпрограмма вызывает `$ return.error`, продолжайте выполнение из действия` error`, используя возвращаемое значение из подпрограммы.



         Похож на ключевое слово `trigger`, но с некоторыми отличиями:
         1. Триггер был только для одноразового запуска и финиша. Лямбда ждет, пока подпрограмма не вернется и продолжит с того места, где остановилась.
         2. «триггер» был ключевым словом, но сама лямбда - просто еще один тип действия. `{" type ":" $ lambda "}`
         3. Лямбда может передавать аргументы через `опции`

         # Как это устроено
         1. Запускает другое действие по имени
         2. Ожидает возврата подпрограммы через `$ return.success` или` $ return.error`
         3. Когда подпрограмма вызывает `$ return.success`, продолжайте выполнение из действия` success`, используя возвращаемое значение из подпрограммы.
         4. Когда подпрограмма вызывает `$ return.error`, продолжайте выполнение из действия` error`, используя возвращаемое значение из подпрограммы.

        # Example 1: Basic lambda (Same as trigger)
        # Пример 1. Базовая лямбда (такая же, как триггер)
        {
            "type": "$lambda",
            "options": {
                "name": "fetch"
            }
        }


        # Example 2: Basic lambda with success/error handlers
        # Пример 2. Базовая лямбда с обработчиками успеха / ошибок
        {
            "type": "$lambda",
            "options": {
                "name": "fetch"
            }
            "success": {
                "type": "$render"
            },
            "error": {
                "type": "$util.toast",
                "options": {
                    "text": "Error"
                }
            }
        }


        # Example 3: Passing arguments
        # Пример 3: передача аргументов
        {
            "type": "$lambda",
            "options": {
                "name": "fetch",
                "options": {
                    "url": "https://www.jasonbase.com/things/73g"
                }
            },
            "success": {
                "type": "$render"
            },
            "error": {
                "type": "$util.toast",
                "options": {
                    "text": "Error"
                }
            }
        }

        # Example 4: Using the previous action's return value
        # Пример 4. Использование возвращаемого значения предыдущего действия

        {
            "type": "$network.request",
            "options": {
                "url": "https://www.jasonbase.com/things/73g"
            },
            "success": {
                "type": "$lambda",
                "options": {
                    "name": "draw"
                },
                "success": {
                    "type": "$render"
                },
                "error": {
                    "type": "$util.toast",
                    "options": {
                        "text": "Error"
                    }
                }
            }
        }

        # Example 5: Using the previous action's return value as well as custom options
        # Пример 5. Использование возвращаемого значения предыдущего действия, а также пользовательских параметров

        {
            "type": "$network.request",
            "options": {
                "url": "https://www.jasonbase.com/things/73g"
            },
            "success": {
                "type": "$lambda",
                "options": {
                    "name": "draw",
                    "options": {
                        "p1": "another param",
                        "p2": "yet another param"
                    }
                },
                "success": {
                    "type": "$render"
                },
                "error": {
                    "type": "$util.toast",
                    "options": {
                        "text": "Error"
                    }
                }
            }
        }

        # Example 6: Using the previous action's return value as well as custom options
        # Пример 5. Использование возвращаемого значения предыдущего действия, а также пользовательских параметров

        {
            "type": "$network.request",
            "options": {
                "url": "https://www.jasonbase.com/things/73g"
            },
            "success": {
                "type": "$lambda",
                "options": [{
                    "{{#if $jason}}": {
                        "name": "draw",
                        "options": {
                            "p1": "another param",
                            "p2": "yet another param"
                        }
                    }
                }, {
                    "{{#else}}": {
                        "name": "err",
                        "options": {
                            "text": "No content to render"
                        }
                    }
                }],
                "success": {
                    "type": "$render"
                },
                "error": {
                    "type": "$util.toast",
                    "options": {
                        "text": "Error"
                    }
                }
            }
        }

         */
        try {
            if (action.has("options")) {
                val options = action.getJSONObject("options")
                // 1. Resolve the action by looking up from $jason.head.actions
                // 1. Разрешить действие, подняв взгляд от  $jason.head.actions
                val event_name = options.getString("name")
                val head = model!!.jason!!.getJSONObject("\$jason").getJSONObject("head")
                val events = head.getJSONObject("actions")
                val lambda = events[event_name]
                val caller = action.toString()

                // 2. If `options` exists, use that as the data to pass to the next action
                // 2. Если `options` существует, используйте его как данные для перехода к следующему действию
                if (options.has("options")) {
                    val new_options = options["options"]

                    // take the options and parse it with current model.state
                    // взять параметры и проанализировать их с текущим model.state
                    JasonParser.Companion.getInstance(this)!!
                        .setParserListener(JasonParserListener { parsed_options ->
                            try {
                                val wrapped = JSONObject()
                                wrapped.put("\$jason", parsed_options)
                                call(
                                    lambda.toString(),
                                    wrapped.toString(),
                                    caller,
                                    this@JasonViewActivity
                                )
                            } catch (e: Exception) {
                                JasonHelper.next(
                                    "error",
                                    action,
                                    JSONObject(),
                                    JSONObject(),
                                    this@JasonViewActivity
                                )
                            }
                        })
                    JasonParser.Companion.getInstance(this)!!
                        .parse("json", model!!.state, new_options, context)
                } else {
                    call(lambda.toString(), data.toString(), caller, this@JasonViewActivity)
                }
            }
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
            JasonHelper.next("error", action, JSONObject(), JSONObject(), this@JasonViewActivity)
        }
    }

    fun require(action: JSONObject, data: JSONObject?, event: JSONObject?, context: Context?) {
        /*

         {
            "type": "$require",
            "options": {
                "items": ["https://...", "https://...", ....],
                "item": "https://...."
            }
         }

         Crawl all the items in the array and assign it to the key
         Обойти все элементы в массиве и назначить его ключу

         */
        try {
            if (action.has("options")) {
                val options = action.getJSONObject("options")
                val urlSet = ArrayList<String>()
                val keys: Iterator<*> = options.keys()
                while (keys.hasNext()) {
                    val key = keys.next() as String
                    val `val` = options[key]

                    // must be either array or string
                    // должен быть либо массивом, либо строкой
                    if (`val` is JSONArray) {
                        for (i in 0 until `val`.length()) {
                            if (!urlSet.contains(`val`.getString(i))) {
                                urlSet.add(`val`.getString(i))
                            }
                        }
                    } else if (`val` is String) {
                        if (!urlSet.contains(`val`)) {
                            urlSet.add(`val`)
                        }
                    }
                }
                if (urlSet.size > 0) {
                    val refs = JSONObject()
                    val latch = CountDownLatch(urlSet.size)
                    val taskExecutor = Executors.newFixedThreadPool(urlSet.size)
                    for (key in urlSet) {
                        taskExecutor.submit(JasonRequire(key, latch, refs, model!!.client, this))
                    }
                    try {
                        latch.await()
                    } catch (e: Exception) {
                        Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
                    }
                    val res = JSONObject()
                    val ks: Iterator<*> = options.keys()
                    while (ks.hasNext()) {
                        val key = ks.next() as String
                        val `val` = options[key]
                        if (`val` is JSONArray) {
                            val ret = JSONArray()
                            for (i in 0 until `val`.length()) {
                                val url = `val`.getString(i)
                                ret.put(refs[url])
                            }
                            res.put(key, ret)
                        } else if (`val` is String) {
                            res.put(key, refs[`val`])
                        }
                    }
                    JasonHelper.next("success", action, res, event, context)
                }
            } else {
                JasonHelper.next("error", action, JSONObject(), event, context)
            }
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
            JasonHelper.next("error", action, JSONObject(), event, context)
        }

        // get all urls
        // получить все URL
    }

    fun render(action: JSONObject, data: JSONObject?, event: JSONObject?, context: Context?) {
        val activity = context as JasonViewActivity?
        try {
            var template_name = "body"
            var type = "json"
            if (action.has("options")) {
                val options = action.getJSONObject("options")
                if (options.has("template")) {
                    template_name = options.getString("template")
                }
                // parse the template with JSON
                if (options.has("data")) {
                    data!!.put("\$jason", options["data"])
                }
                if (options.has("type")) {
                    type = options.getString("type")
                }
            }
            val head = model!!.jason!!.getJSONObject("\$jason").getJSONObject("head")
            val templates = head.getJSONObject("templates")
            val template = templates.getJSONObject(template_name)
            JasonParser.Companion.getInstance(this)!!
                .setParserListener(JasonParserListener { body ->
                    setup_body(body)
                    JasonHelper.next("success", action, JSONObject(), event, context)
                })
            JasonParser.Companion.getInstance(this)!!.parse(type, data, template, context)
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
            JasonHelper.next("error", action, JSONObject(), event, context)
        }
    }

    operator fun set(action: JSONObject, data: JSONObject?, event: JSONObject?, context: Context?) {
        try {
            if (action.has("options")) {
                val options = action.getJSONObject("options")
                model!!.`var` = JasonHelper.merge(model!!.`var`, options)
            }
            JasonHelper.next("success", action, JSONObject(), event, context)
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    }

    fun href(action: JSONObject, data: JSONObject?, event: JSONObject?, context: Context?) {
        try {
            if (action.has("options")) {
                isexecuting = false
                resumed = false
                val url = action.getJSONObject("options").getString("url")
                var transition = "push"
                if (action.getJSONObject("options").has("transition")) {
                    transition = action.getJSONObject("options").getString("transition")
                }

                // "view": "web"
                // "view": "web"
                if (action.getJSONObject("options").has("view")) {
                    val view_type = action.getJSONObject("options").getString("view")
                    if (view_type.equals("web", ignoreCase = true) || view_type.equals(
                            "app",
                            ignoreCase = true
                        )
                    ) {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.data = Uri.parse(url)
                            startActivity(intent)
                        } catch (e: Exception) {
                            val intent = Intent(url)
                            startActivity(intent)
                        }
                        return
                    }
                }


                // "view": "jason" (default)
                // "view": "jason" (по умолчанию)
                // Set params for the next view (use either 'options' or 'params')
                // Установить параметры для следующего представления (используйте «опции» или «параметры»)
                var params: String? = null
                if (action.getJSONObject("options").has("options")) {
                    params = action.getJSONObject("options").getJSONObject("options").toString()
                } else if (action.getJSONObject("options").has("params")) {
                    params = action.getJSONObject("options").getJSONObject("params").toString()
                }

                // Reset SharedPreferences so it doesn't overwrite the model onResume
                // Сбрасываем SharedPreferences, чтобы он не переписывал модель onResume
                val pref = getSharedPreferences("model", 0)
                val editor = pref.edit()
                editor.remove(url)
                editor.commit()
                if (transition.equals("switchtab", ignoreCase = true)) {
                    if (action.getJSONObject("options").has("preload")) {
                        preload = action.getJSONObject("options").getJSONObject("preload")
                    }
                    val intent = Intent(this, JasonViewActivity::class.java)
                    intent.putExtra("depth", depth)
                    if (params != null) {
                        intent.putExtra("params", params)
                        onSwitchTab(url, params, intent)
                    } else {
                        params = "{}"
                        onSwitchTab(url, params, intent)
                    }
                } else if (transition.equals("replace", ignoreCase = true)) {
                    // remove all touch listeners before replacing
                    // удалить все сенсорные слушатели перед заменой
                    // Use case : Tab bar
                    // Вариант использования: панель вкладок
                    removeListViewOnItemTouchListeners()
                    val intent = Intent(this, JasonViewActivity::class.java)
                    if (params != null) {
                        intent.putExtra("params", params)
                    }
                    intent.putExtra("depth", depth)
                    model = JasonModel(url, intent, this)
                    if (action.getJSONObject("options").has("preload")) {
                        preload = action.getJSONObject("options").getJSONObject("preload")
                    }
                    onRefresh()
                } else {
                    val intent = Intent(this, JasonViewActivity::class.java)
                    intent.putExtra("url", url)
                    if (params != null) {
                        intent.putExtra("params", params)
                    }
                    if (action.getJSONObject("options").has("preload")) {
                        intent.putExtra(
                            "preload",
                            action.getJSONObject("options").getJSONObject("preload").toString()
                        )
                    }
                    intent.putExtra("depth", depth!! + 1)

                    // Start an Intent with a callback option:
                    // 1. call dispatchIntent
                    // 2. the intent will return with JasonCallback.href
                    // Начать намерение с опцией обратного вызова:
                    // 1. вызовите dispatchIntent
                    // 2. намерение вернется с JasonCallback.href
                    val callback = JSONObject()
                    callback.put("class", "JasonCallback")
                    callback.put("method", "href")
                    JasonHelper.dispatchIntent(action, data, event, context, intent, callback)
                }
            }
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    }

    fun back(action: JSONObject?, data: JSONObject?, event: JSONObject?, context: Context?) {
        finish()
    }

    fun close(action: JSONObject?, data: JSONObject?, event: JSONObject?, context: Context?) {
        finish()
    }

    fun ok(action: JSONObject, data: JSONObject?, event: JSONObject?, context: Context?) {
        try {
            val intent = Intent()
            if (action.has("options")) {
                intent.putExtra("return", action["options"].toString())
            }
            setResult(RESULT_OK, intent)
            finish()
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    }

    fun unlock(action: JSONObject?, data: JSONObject?, event: JSONObject?, context: Context) {
        if (event_queue!!.size > 0) {
            val next_action = event_queue!![0]
            event_queue!!.removeAt(0)
            try {
                isexecuting = false
                simple_trigger(
                    next_action.getString("event_name"),
                    next_action.getJSONObject("data"),
                    context
                )
            } catch (e: Exception) {
                Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
            }
        } else {
            isexecuting = false
            runOnUiThread {
                try {
                    loading!!.visibility = View.GONE
                    if (swipeLayout != null) {
                        swipeLayout!!.isRefreshing = false
                    }
                } catch (e: Exception) {
                    Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
                }
            }
        }
    }

    fun reload(action: JSONObject?, data: JSONObject?, event: JSONObject?, context: Context?) {
        if (model != null) {
            onRefresh()
            try {
                JasonHelper.next("success", action, JSONObject(), event, context)
            } catch (e: Exception) {
                Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
            }
        }
    }

    fun flush(action: JSONObject?, data: JSONObject?, event: JSONObject?, context: Context?) {
        // there's no default caching on Android. So don't do anything for now
        // на Android нет кэширования по умолчанию. Так что пока ничего не делай
        try {
            JasonHelper.next("success", action, JSONObject(), event, context)
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    }

    fun snapshot(action: JSONObject?, data: JSONObject?, event: JSONObject?, context: Context?) {
        val v1 = window.decorView.rootView
        v1.isDrawingCacheEnabled = true
        val bitmap = Bitmap.createBitmap(v1.drawingCache)
        v1.isDrawingCacheEnabled = false
        Thread {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            val byteArray = stream.toByteArray()
            val encoded = Base64.encodeToString(byteArray, Base64.NO_WRAP)
            val stringBuilder = StringBuilder()
            stringBuilder.append("data:image/jpeg;base64,")
            stringBuilder.append(encoded)
            val data_uri = stringBuilder.toString()
            try {
                val ret = JSONObject()
                ret.put("data", encoded)
                ret.put("data_uri", data_uri)
                ret.put("content_type", "image/png")
                JasonHelper.next("success", action, ret, event, context)
            } catch (e: Exception) {
                Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
            }
        }.start()
    }

    fun scroll(action: JSONObject, data: JSONObject?, event: JSONObject?, context: Context?) {
        try {
            if (action.has("options")) {
                val options = action.getJSONObject("options")
                if (options.has("position")) {
                    val position = options.getString("position")
                    if (position.equals("top", ignoreCase = true)) {
                        listView!!.smoothScrollToPosition(0)
                    } else if (position.equals("bottom", ignoreCase = true)) {
                        listView!!.smoothScrollToPosition(listView!!.adapter!!.itemCount - 1)
                    }
                }
            }
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
        JasonHelper.next("success", action, JSONObject(), event, context)
    }

    /*************************************************************
     *
     * JASON VIEW
     * JASON VIEW
     *
     */
    fun build(jason: JSONObject?) {
        // set fetched to true since build() is only called after network.request succeeds
        // устанавливаем fetched в true, так как build () вызывается только после успешного выполнения network.request
        fetched = true
        if (jason != null) {
            try {

                // Set up background
                // Установить фон
                if (jason.getJSONObject("\$jason").has("body")) {
                    val body: JSONObject
                    body = jason.getJSONObject("\$jason").getJSONObject("body")
                    model!!["state"] = JSONObject()
                    setup_body(body)
                }
                if (jason.getJSONObject("\$jason").has("head")) {
                    val head = jason.getJSONObject("\$jason").getJSONObject("head")
                    if (head.has("agents")) {
                        val agents = head.getJSONObject("agents")
                        val iterator = agents.keys()
                        while (iterator.hasNext()) {
                            val key = iterator.next()
                            runOnUiThread {
                                try {
                                    val agentService =
                                        (applicationContext as Launcher).services["JasonAgentService"] as JasonAgentService
                                    val agent = agentService.setup(
                                        this@JasonViewActivity,
                                        agents.getJSONObject(key),
                                        key
                                    )
                                    rootLayout!!.addView(agent)
                                } catch (e: JSONException) {
                                }
                            }
                        }
                    }
                    if (head.has("data")) {
                        if (head.has("templates")) {
                            if (head.getJSONObject("templates").has("body")) {
                                model!!["state"] = JSONObject()
                                render(JSONObject(), model!!.state, JSONObject(), this)

                                // return here so onLoad() below will NOT be triggered.
                                // onLoad() will be triggered after render has finished
                                return
                            }
                        }
                    }
                }
                onLoad()
            } catch (e: JSONException) {
                Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
            }
        }
    }

    private fun setup_body(body: JSONObject?) {

        // Store to offline cache in case head.offline == true
        // Сохраняем в автономный кеш в случае head.offline == true
        try {
            model!!.rendered = body
            invalidateOptionsMenu()
            if (model!!.jason != null && model!!.jason!!.has("\$jason") && model!!.jason!!.getJSONObject(
                    "\$jason"
                ).has("head") && model!!.jason!!.getJSONObject("\$jason").getJSONObject("head")
                    .has("offline")
            ) {
                val pref = getSharedPreferences("offline", 0)
                val editor = pref.edit()
                val signature = model!!.url + model!!.params.toString()
                val offline_cache = JSONObject()
                offline_cache.put("jason", model!!.jason)
                offline_cache.put("rendered", model!!.rendered)
                editor.putString(signature, offline_cache.toString())
                editor.commit()
            }
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
        runOnUiThread {
            try {
                // First need to remove all handlers because they will be reattached after render
                // Сначала нужно удалить все обработчики, потому что они будут присоединены после рендера
                removeListViewOnItemTouchListeners()
                if (swipeLayout != null) {
                    swipeLayout!!.isRefreshing = false
                }
                sectionLayout!!.setBackgroundColor(JasonHelper.parse_color("rgb(255,255,255)"))
                window.decorView.setBackgroundColor(JasonHelper.parse_color("rgb(255,255,255)"))
                var bg: Any? = null
                if (body!!.has("style") && body.getJSONObject("style").has("background")) {
                    bg = body.getJSONObject("style")["background"]
                } else if (body.has("background")) {
                    bg = body["background"]
                }

                // Background Logic
                // Фоновая логика
                if (bg != null) {
                    // sectionLayout must be transparent to see the background
                    // макет раздела должен быть прозрачным, чтобы видеть фон
                    sectionLayout!!.setBackgroundColor(JasonHelper.parse_color("rgba(0,0,0,0)"))

                    // we remove the current view from the root layout
                    // удаляем текущий вид из корневого макета
                    var needs_redraw = false
                    if (backgroundCurrentView != null) {
                        val current_background = bg.toString()
                        if (previous_background == null) {
                            needs_redraw = true
                            rootLayout!!.removeView(backgroundCurrentView)
                            backgroundCurrentView = null
                        } else if (current_background.equals(
                                previous_background,
                                ignoreCase = true
                            )
                        ) {
                            needs_redraw = false
                        } else {
                            needs_redraw = true
                            rootLayout!!.removeView(backgroundCurrentView)
                            backgroundCurrentView = null
                        }
                        previous_background = current_background
                    } else {
                        needs_redraw = true
                        rootLayout!!.removeView(backgroundCurrentView)
                        backgroundCurrentView = null
                    }
                    if (needs_redraw) {
                        if (bg is String) {
                            val background = bg
                            val c = JSONObject()
                            c.put("url", background)
                            if (background.matches("(file|http[s]?):\\/\\/.*")) {
                                if (backgroundImageView == null) {
                                    backgroundImageView = ImageView(this@JasonViewActivity)
                                }
                                backgroundCurrentView = backgroundImageView
                                var cacheStrategy = DiskCacheStrategy.RESULT
                                // gif doesn't work with RESULT cache strategy
                                // TODO: Check with Glide V4
                                if (background.matches(".*\\.gif")) {
                                    cacheStrategy = DiskCacheStrategy.SOURCE
                                }
                                Glide.with(this@JasonViewActivity)
                                    .load(
                                        JasonImageComponent.resolve_url(
                                            c,
                                            this@JasonViewActivity
                                        )
                                    )
                                    .diskCacheStrategy(cacheStrategy)
                                    .centerCrop()
                                    .into(backgroundImageView)
                            } else if (background.matches("data:image.*")) {
                                val base64: String
                                base64 = if (background.startsWith("data:image/jpeg")) {
                                    background.substring("data:image/jpeg;base64,".length)
                                } else if (background.startsWith("data:image/png")) {
                                    background.substring("data:image/png;base64,".length)
                                } else if (background.startsWith("data:image/gif")) {
                                    background.substring("data:image/gif;base64,".length)
                                } else {
                                    "" // exception
                                }
                                val bs = Base64.decode(base64, Base64.NO_WRAP)
                                Glide.with(this@JasonViewActivity).load(bs)
                                    .into(object : SimpleTarget<GlideDrawable?>() {
                                        override fun onResourceReady(
                                            resource: GlideDrawable,
                                            glideAnimation: GlideAnimation<in GlideDrawable>
                                        ) {
                                            sectionLayout!!.background = resource
                                        }
                                    })
                            } else {
                                if (background.equals("camera", ignoreCase = true)) {
                                    val side: Int = JasonVisionService.Companion.FRONT
                                    if (cameraManager == null) {
                                        cameraManager = JasonVisionService(this@JasonViewActivity)
                                        backgroundCameraView = cameraManager!!.view
                                    }
                                    cameraManager!!.setSide(side)
                                    backgroundCurrentView = backgroundCameraView
                                } else {
                                    sectionLayout!!.setBackgroundColor(
                                        JasonHelper.parse_color(
                                            background
                                        )
                                    )
                                    window.decorView.setBackgroundColor(
                                        JasonHelper.parse_color(
                                            background
                                        )
                                    )
                                }
                            }
                        } else {
                            val background = bg as JSONObject
                            val type = background.getString("type")
                            if (type.equals("html", ignoreCase = true)) {
                                // on Android the tabs work differently from iOS
                                // => All tabs share a single activity.
                                // therefore, unlike ios where each viewcontroller owns a web container through "$webcontainer" id,
                                // on android we need to distinguish between multiple web containers through URL
                                // на Android вкладки работают не так как на iOS
                                // => Все вкладки имеют одно действие.
                                // поэтому, в отличие от ios, где каждый viewcontroller владеет веб-контейнером через идентификатор "$ webcontainer",
                                // на андроиде нужно различать несколько веб-контейнеров по URL
                                background.put("id", "\$webcontainer@" + model!!.url)
                                val agentService =
                                    (applicationContext as Launcher).services["JasonAgentService"] as JasonAgentService
                                backgroundWebview = agentService.setup(
                                    this@JasonViewActivity,
                                    background,
                                    "\$webcontainer@" + model!!.url
                                )
                                backgroundWebview.setVisibility(View.VISIBLE)
                                // not interactive by default;
                                var responds_to_webview = false
                                /**
                                 *
                                 * if has an 'action' attribute
                                 * - if the action is "type": "$default"
                                 * => Allow touch. The visit will be handled in the agent handler
                                 * - if the action is everything else
                                 * => Allow touch. The visit will be handled in the agent handler
                                 * if it doesn't have an 'action' attribute
                                 * => Don't allow touch.
                                 * if имеет атрибут 'action'
                                 * - если действие "тип": "$ default"
                                 *   => Разрешить касание. Визит будет обработан в обработчике агента
                                 *   - если действие это все остальное
                                 *   => Разрешить касание. Визит будет обработан в обработчике агента
                                 *   - если у него нет атрибута 'action'
                                 *   => Не позволяйте касаться.
                                 *
                                 */
                                /**
                                 *
                                 * if has an 'action' attribute
                                 * - if the action is "type": "$default"
                                 * => Allow touch. The visit will be handled in the agent handler
                                 * - if the action is everything else
                                 * => Allow touch. The visit will be handled in the agent handler
                                 * if it doesn't have an 'action' attribute
                                 * => Don't allow touch.
                                 * if имеет атрибут 'action'
                                 * - если действие "тип": "$ default"
                                 *   => Разрешить касание. Визит будет обработан в обработчике агента
                                 *   - если действие это все остальное
                                 *   => Разрешить касание. Визит будет обработан в обработчике агента
                                 *   - если у него нет атрибута 'action'
                                 *   => Не позволяйте касаться.
                                 *
                                 */
                                if (background.has("action")) {
                                    responds_to_webview = true
                                }
                                if (responds_to_webview) {
                                    // webview receives click
                                    backgroundWebview.setOnTouchListener(null)
                                } else {
                                    // webview shouldn't receive click
                                    backgroundWebview.setOnTouchListener(OnTouchListener { v, event -> true })
                                }
                                backgroundCurrentView = backgroundWebview
                            } else if (type.equals("camera", ignoreCase = true)) {
                                var side: Int = JasonVisionService.Companion.FRONT
                                if (background.has("options")) {
                                    val options = background.getJSONObject("options")
                                    if (options.has("device") && options.getString("device") == "back") {
                                        side = JasonVisionService.Companion.BACK
                                    }
                                }
                                if (cameraManager == null) {
                                    cameraManager = JasonVisionService(this@JasonViewActivity)
                                    backgroundCameraView = cameraManager!!.view
                                }
                                cameraManager!!.setSide(side)
                                backgroundCurrentView = backgroundCameraView
                            }
                        }
                        if (backgroundCurrentView != null) {
                            val rlp = RelativeLayout.LayoutParams(
                                RelativeLayout.LayoutParams.MATCH_PARENT,
                                RelativeLayout.LayoutParams.MATCH_PARENT
                            )

                            // Update Layout after the rootLayout has finished rendering in order to change the background dimension
                            // Обновляем Layout после завершения рендеринга rootLayout, чтобы изменить размер фона
                            rootLayout!!.viewTreeObserver.addOnGlobalLayoutListener(object :
                                OnGlobalLayoutListener {
                                override fun onGlobalLayout() {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                        rootLayout!!.viewTreeObserver.removeOnGlobalLayoutListener(
                                            this
                                        )
                                    } else {
                                        rootLayout!!.viewTreeObserver.removeGlobalOnLayoutListener(
                                            this
                                        )
                                    }

                                    // header
                                    // заголовок
                                    var toolbarHeight = 0
                                    if (body.has("header")) {
                                        toolbarHeight = toolbar!!.height
                                    }

                                    // footer.tabs
                                    // нижний колонтитул.tabs
                                    var tabsHeight = 0
                                    if (bottomNavigation != null) {
                                        tabsHeight = bottomNavigation!!.height
                                    }

                                    // footer.input
                                    // нижний колонтитул.input
                                    var inputHeight = 0
                                    if (footerInput != null) {
                                        inputHeight = footerInput!!.height
                                    }
                                    val newrlp = RelativeLayout.LayoutParams(
                                        RelativeLayout.LayoutParams.MATCH_PARENT,
                                        RelativeLayout.LayoutParams.MATCH_PARENT
                                    )
                                    newrlp.setMargins(0, toolbarHeight, 0, tabsHeight + inputHeight)
                                    backgroundCurrentView!!.layoutParams = newrlp
                                }
                            })
                            rootLayout!!.addView(backgroundCurrentView, 0, rlp)
                        }
                    }
                }
                rootLayout!!.viewTreeObserver.addOnGlobalLayoutListener(object :
                    OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                            rootLayout!!.viewTreeObserver.removeOnGlobalLayoutListener(this)
                        } else {
                            rootLayout!!.viewTreeObserver.removeGlobalOnLayoutListener(this)
                        }
                        if (focusView != null) {
                            focusView!!.requestFocus()
                            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                            imm.toggleSoftInput(
                                InputMethodManager.SHOW_FORCED,
                                InputMethodManager.HIDE_IMPLICIT_ONLY
                            )
                        }
                    }
                })

                // Set header
                // Установить заголовок
                if (body.has("header")) {
                    setup_header(body.getJSONObject("header"))
                    toolbar!!.visibility = View.VISIBLE
                } else {
                    toolbar!!.visibility = View.GONE
                }
                // Set sections
                // Установить разделы
                if (body.has("sections")) {
                    setup_sections(body.getJSONArray("sections"))
                    var border = "#eaeaea" // Default color
                    if (body.has("style") && body.getJSONObject("style").has("border")) {
                        border = body.getJSONObject("style").getString("border")
                    }
                    if (divider != null) {
                        listView!!.removeItemDecoration(divider!!)
                        divider = null
                    }
                    if (!border.equals("none", ignoreCase = true)) {
                        val color = JasonHelper.parse_color(border)
                        listView!!.removeItemDecoration(divider!!)
                        divider = HorizontalDividerItemDecoration.Builder(this@JasonViewActivity)
                            .color(color)
                            .showLastDivider()
                            .positionInsideItem(true)
                            .build()
                        listView!!.addItemDecoration(divider)
                    }
                } else {
                    setup_sections(null)
                }
                swipeLayout!!.isEnabled = false
                if (model!!.jason != null && model!!.jason!!.has("\$jason") && model!!.jason!!.getJSONObject(
                        "\$jason"
                    ).has("head")
                ) {
                    val head = model!!.jason!!.getJSONObject("\$jason").getJSONObject("head")
                    if (head.has("actions") && head.getJSONObject("actions").has("\$pull")) {
                        // Setup refresh listener which triggers new data loading
                        swipeLayout!!.isEnabled = true
                        swipeLayout!!.setOnRefreshListener {
                            try {
                                val action = head.getJSONObject("actions").getJSONObject("\$pull")
                                call(
                                    action.toString(),
                                    JSONObject().toString(),
                                    "{}",
                                    this@JasonViewActivity
                                )
                            } catch (e: Exception) {
                                Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
                            }
                        }
                    }
                }
                if (body.has("style")) {
                    val style = body.getJSONObject("style")
                    if (style.has("align")) {
                        if (style.getString("align").equals("bottom", ignoreCase = true)) {
                            (listView!!.layoutManager as LinearLayoutManager?)!!.stackFromEnd = true
                            listView!!.addOnLayoutChangeListener { view, i, i1, i2, i3, i4, i5, i6, i7 ->
                                if (i3 < i7) {
                                    listView!!.postDelayed({
                                        if (listView!!.adapter!!.itemCount > 0) {
                                            listView!!.smoothScrollToPosition(
                                                listView!!.adapter!!.itemCount - 1
                                            )
                                        }
                                    }, 100)
                                }
                            }
                        }
                    }
                }

                // Set footer
                // Установить нижний колонтитул
                if (body.has("footer")) {
                    setup_footer(body.getJSONObject("footer"))
                }


                // Set layers
                // Установить слои
                if (body.has("layers")) {
                    setup_layers(body.getJSONArray("layers"))
                } else {
                    setup_layers(null)
                }
                rootLayout!!.requestLayout()

                // if the first time being loaded
                // если загружается первый раз
                if (!loaded) {
                    // and if the content has finished fetching (not via remote: true)
                    // и если контент закончил извлекаться (не через remote: true)
                    if (fetched) {
                        // trigger onLoad.
                        // вызвать onLoad.
                        // onLoad shouldn't be triggered when just drawing the offline cached view initially
                        // onLoad не должен запускаться при начальном рисовании автономного кэшированного представления
                        onLoad()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setup_header(header: JSONObject) {
        try {
            val backgroundColor = header.getJSONObject("style").getString("background")
            toolbar!!.setBackgroundColor(JasonHelper.parse_color(backgroundColor))
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
        try {
            val color = header.getJSONObject("style").getString("color")
            toolbar!!.setTitleTextColor(JasonHelper.parse_color(color))
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    }

    private fun setup_sections(sections: JSONArray?) {
        section_items = ArrayList()
        if (sections != null) {
            try {
                for (i in 0 until sections.length()) {
                    val section = sections.getJSONObject(i)

                    // Determine if it's a horizontal section or vertical section
                    // if it's vertical, simply keep adding to the section as individual items
                    // if it's horizontal, start a nested recyclerview
                    // Определяем, горизонтальное или вертикальное сечение
                    // если он вертикальный, просто продолжайте добавлять в раздел как отдельные элементы
                    // если он горизонтальный, запускаем вложенный просмотр
                    if (section.has("type") && section.getString("type") == "horizontal") {
                        // horizontal type
                        // горизонтальный тип
                        // TEMPORARY: Add header as an item
                        // TEMPORARY: добавить заголовок как элемент
                        if (section.has("header")) {
                            val header = section.getJSONObject("header")
                            section_items!!.add(header)
                        }
                        if (section.has("items")) {
                            // Let's add the entire section as an item, under:
                            // Давайте добавим весь раздел как элемент под:
                            // "horizontal_section": [items]
                            // "горизонтальное_сечение": [элементы]
                            val horizontal_section = JSONObject()
                            horizontal_section.put(
                                "horizontal_section",
                                section.getJSONArray("items")
                            )
                            section_items!!.add(horizontal_section)
                        }
                    } else {
                        // vertical type (default)
                        if (section.has("header")) {
                            val header = section.getJSONObject("header")
                            section_items!!.add(header)
                        }
                        if (section.has("items")) {
                            val items = section.getJSONArray("items")
                            for (j in 0 until items.length()) {
                                val item = items.getJSONObject(j)
                                section_items!!.add(item)
                            }
                        }
                    }
                }
            } catch (e: JSONException) {
                Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
            }
        }
        if (adapter == null || adapter!!.items.size == 0) {
            // Create adapter passing in the sample user data
            adapter = ItemAdapter(this, this, section_items)
            // Attach the adapter to the recyclerview to populate items
            listView!!.adapter = adapter
            // Set layout manager to position the items
            listView!!.layoutManager = LinearLayoutManager(this)
        } else {
            //ArrayList<JSONObject> old_section_items = adapter.getItems();
            adapter!!.updateItems(section_items)
            adapter!!.notifyDataSetChanged()
        }
    }

    private fun setup_footer(footer: JSONObject) {
        try {
            if (footer.has("tabs")) {
                setup_tabs(footer.getJSONObject("tabs"))
            } else {
                if (bottomNavigation != null) {
                    rootLayout!!.removeView(bottomNavigation)
                }
            }
            if (footer.has("input")) {
                setup_input(footer.getJSONObject("input"))
            }
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    }

    private fun setup_input(input: JSONObject) {
        // Set up a horizontal linearlayout
        // which sticks to the bottom
        // Установить горизонтальное линейное расположение
        // который прилипает ко дну
        rootLayout!!.removeView(footerInput)
        val height = JasonHelper.pixels(this@JasonViewActivity, "60", "vertical").toInt()
        val spacing = JasonHelper.pixels(this@JasonViewActivity, "5", "vertical").toInt()
        val outer_padding = JasonHelper.pixels(this@JasonViewActivity, "10", "vertical").toInt()
        footerInput = LinearLayout(this)
        footerInput!!.orientation = LinearLayout.HORIZONTAL
        footerInput!!.gravity = Gravity.CENTER_VERTICAL
        footerInput!!.setPadding(outer_padding, 0, outer_padding, 0)
        rootLayout!!.addView(footerInput)
        val params = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, height)
        params.bottomMargin = 0
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
        footerInput!!.layoutParams = params
        try {
            if (input.has("style")) {
                if (input.getJSONObject("style").has("background")) {
                    val color = JasonHelper.parse_color(
                        input.getJSONObject("style").getString("background")
                    )
                    footerInput!!.setBackgroundColor(color)
                }
            }
            if (input.has("left")) {
                val json = input.getJSONObject("left")
                val style: JSONObject
                style = if (json.has("style")) {
                    json.getJSONObject("style")
                } else {
                    JSONObject()
                }
                style.put("height", "25")
                if (json.has("image")) {
                    json.put("url", json.getString("image"))
                }
                json.put("type", "button")
                json.put("style", style)
                val leftButton: View =
                    JasonComponentFactory.Companion.build(null, json, null, this@JasonViewActivity)
                leftButton.setPadding(spacing, 0, spacing, 0)
                JasonComponentFactory.Companion.build(
                    leftButton,
                    input.getJSONObject("left"),
                    null,
                    this@JasonViewActivity
                )
                footerInput!!.addView(leftButton)
            }
            val textfield: JSONObject
            textfield = if (input.has("textfield")) {
                input.getJSONObject("textfield")
            } else {
                input
            }
            textfield.put("type", "textfield")
            // First build only creates the stub.
            // Первая сборка только создает заглушку.
            footer_input_textfield =
                JasonComponentFactory.Companion.build(null, textfield, null, this@JasonViewActivity)
            val padding = JasonHelper.pixels(this@JasonViewActivity, "10", "vertical").toInt()
            // Build twice because the first build only builds the stub.
            // Сборка дважды, потому что первая сборка создает только заглушку.
            JasonComponentFactory.Companion.build(
                footer_input_textfield,
                textfield,
                null,
                this@JasonViewActivity
            )
            footer_input_textfield!!.setPadding(padding, padding, padding, padding)
            footerInput!!.addView(footer_input_textfield)
            val layout_params = footer_input_textfield!!.layoutParams as LinearLayout.LayoutParams
            layout_params.height = LinearLayout.LayoutParams.MATCH_PARENT
            layout_params.weight = 1f
            layout_params.width = 0
            layout_params.leftMargin = spacing
            layout_params.rightMargin = spacing
            layout_params.topMargin = spacing
            layout_params.bottomMargin = spacing
            if (input.has("right")) {
                val json = input.getJSONObject("right")
                val style: JSONObject
                style = if (json.has("style")) {
                    json.getJSONObject("style")
                } else {
                    JSONObject()
                }
                if (!json.has("image") && !json.has("text")) {
                    json.put("text", "Send")
                }
                if (json.has("image")) {
                    json.put("url", json.getString("image"))
                }
                style.put("height", "25")
                json.put("type", "button")
                json.put("style", style)
                val rightButton: View =
                    JasonComponentFactory.Companion.build(null, json, null, this@JasonViewActivity)
                JasonComponentFactory.Companion.build(
                    rightButton,
                    input.getJSONObject("right"),
                    null,
                    this@JasonViewActivity
                )
                rightButton.setPadding(spacing, 0, spacing, 0)
                footerInput!!.addView(rightButton)
            }
            footerInput!!.requestLayout()
            listView!!.clipToPadding = false
            listView!!.setPadding(0, 0, 0, height)
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    }

    private fun setup_tabs(tabs: JSONObject) {
        try {
            val items = tabs.getJSONArray("items")
            if (bottomNavigation == null) {
                bottomNavigation = AHBottomNavigation(this)
                rootLayout!!.addView(bottomNavigation)
                val params = RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
                )
                params.bottomMargin = 0
                params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                bottomNavigation!!.layoutParams = params
            }
            bottomNavigation!!.titleState = AHBottomNavigation.TitleState.ALWAYS_HIDE
            bottomNavigation!!.isBehaviorTranslationEnabled = true
            bottomNavigation!!.defaultBackgroundColor = Color.parseColor("#FEFEFE")
            val style: JSONObject
            if (tabs.has("style")) {
                style = tabs.getJSONObject("style")
                if (style.has("color")) {
                    val color = JasonHelper.parse_color(style.getString("color"))
                    bottomNavigation!!.accentColor = color
                }
                if (style.has("color:disabled")) {
                    val disabled_color = JasonHelper.parse_color(style.getString("color:disabled"))
                    bottomNavigation!!.inactiveColor = disabled_color
                }
                if (style.has("background")) {
                    val background = JasonHelper.parse_color(style.getString("background"))
                    bottomNavigation!!.defaultBackgroundColor = background
                    bottomNavigation!!.setBackgroundColor(background)
                }
            }
            if (bottomNavigation!!.itemsCount == items.length()) {
                // if the same number as the previous state, try to fill in the items instead of re-instantiating them all
                // если номер совпадает с предыдущим состоянием, попробуйте заполнить элементы, а не создавать их все
                for (i in 0 until items.length()) {
                    val item = items.getJSONObject(i)
                    if (item.has("image")) {
                        var temptext = ""
                        try {
                            if (item.has("text")) {
                                temptext = item.getString("text")
                                bottomNavigation!!.titleState =
                                    AHBottomNavigation.TitleState.ALWAYS_SHOW
                            }
                        } catch (e: Exception) {
                            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
                        }
                        val text = temptext
                        val c = JSONObject()
                        c.put("url", item.getString("image"))
                        Glide
                            .with(this)
                            .load(JasonImageComponent.resolve_url(c, this@JasonViewActivity))
                            .asBitmap()
                            .into(object : SimpleTarget<Bitmap?>(100, 100) {
                                override fun onResourceReady(
                                    resource: Bitmap,
                                    glideAnimation: GlideAnimation<*>?
                                ) {
                                    val tab_item = bottomNavigation!!.getItem(i)
                                    bottomNavigationItems!![Integer.valueOf(i)] = tab_item
                                    val drawable: Drawable = BitmapDrawable(resources, resource)
                                    tab_item.setDrawable(drawable)
                                    tab_item.setTitle(text)
                                }

                                override fun onLoadFailed(e: Exception, errorDrawable: Drawable) {
                                    val tab_item = bottomNavigation!!.getItem(i)
                                    bottomNavigationItems!![Integer.valueOf(i)] = tab_item
                                    tab_item.setTitle(text)
                                }
                            })
                    } else if (item.has("text")) {
                        var text: String? = ""
                        try {
                            text = item.getString("text")
                            bottomNavigation!!.titleState =
                                AHBottomNavigation.TitleState.ALWAYS_SHOW
                        } catch (e: Exception) {
                            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
                        }
                        val tab_item = bottomNavigation!!.getItem(i)
                        bottomNavigationItems!![Integer.valueOf(i)] = tab_item
                        val d = ColorDrawable(Color.TRANSPARENT)
                        tab_item.setDrawable(d)
                        tab_item.setTitle(text)
                    }
                }
            } else {
                bottomNavigationItems = HashMap()
                for (i in 0 until items.length()) {
                    val item = items.getJSONObject(i)
                    if (item.has("image")) {
                        val c = JSONObject()
                        c.put("url", item.getString("image"))
                        Glide.with(this)
                            .load(JasonImageComponent.resolve_url(c, this@JasonViewActivity))
                            .asBitmap()
                            .into(object : SimpleTarget<Bitmap?>(100, 100) {
                                override fun onResourceReady(
                                    resource: Bitmap,
                                    glideAnimation: GlideAnimation<*>?
                                ) {
                                    var text: String? = ""
                                    try {
                                        if (item.has("text")) {
                                            text = item.getString("text")
                                            bottomNavigation!!.titleState =
                                                AHBottomNavigation.TitleState.ALWAYS_SHOW
                                        }
                                    } catch (e: Exception) {
                                        Log.d(
                                            "Warning",
                                            e.stackTrace[0].methodName + " : " + e.toString()
                                        )
                                    }
                                    val drawable: Drawable = BitmapDrawable(resources, resource)
                                    val item = AHBottomNavigationItem(text, drawable)
                                    bottomNavigationItems!![Integer.valueOf(i)] = item
                                    if (bottomNavigationItems!!.size >= items.length()) {
                                        for (j in 0 until bottomNavigationItems!!.size) {
                                            bottomNavigation!!.addItem(
                                                bottomNavigationItems!![Integer.valueOf(
                                                    j
                                                )]
                                            )
                                        }
                                    }
                                }

                                override fun onLoadFailed(
                                    exception: Exception,
                                    errorDrawable: Drawable
                                ) {
                                    var text: String? = ""
                                    try {
                                        if (item.has("text")) {
                                            text = item.getString("text")
                                            bottomNavigation!!.titleState =
                                                AHBottomNavigation.TitleState.ALWAYS_SHOW
                                        }
                                    } catch (e: Exception) {
                                        Log.d(
                                            "Warning",
                                            e.stackTrace[0].methodName + " : " + e.toString()
                                        )
                                    }
                                    val d = ColorDrawable(Color.TRANSPARENT)
                                    val tab_item = AHBottomNavigationItem(text, d)
                                    bottomNavigationItems!![Integer.valueOf(i)] = tab_item
                                    if (bottomNavigationItems!!.size >= items.length()) {
                                        for (j in 0 until bottomNavigationItems!!.size) {
                                            bottomNavigation!!.addItem(
                                                bottomNavigationItems!![Integer.valueOf(
                                                    j
                                                )]
                                            )
                                        }
                                    }
                                }
                            })
                    } else if (item.has("text")) {
                        var text: String? = ""
                        try {
                            if (item.has("text")) {
                                text = item.getString("text")
                                bottomNavigation!!.titleState =
                                    AHBottomNavigation.TitleState.ALWAYS_SHOW
                            }
                        } catch (e: Exception) {
                            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
                        }
                        val d = ColorDrawable(Color.TRANSPARENT)
                        val tab_item = AHBottomNavigationItem(text, d)
                        bottomNavigationItems!![Integer.valueOf(i)] = tab_item
                        if (bottomNavigationItems!!.size >= items.length()) {
                            for (j in 0 until bottomNavigationItems!!.size) {
                                bottomNavigation!!.addItem(bottomNavigationItems!![Integer.valueOf(j)])
                            }
                        }
                    }
                }
            }
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                if (item.has("badge")) {
                    bottomNavigation!!.setNotification(item["badge"].toString(), i)
                }
            }
            bottomNavigation!!.setOnTabSelectedListener(AHBottomNavigation.OnTabSelectedListener { position, wasSelected ->
                try {
                    val current = bottomNavigation!!.currentItem
                    val item = items.getJSONObject(position)
                    if (item.has("href")) {
                        val action = JSONObject()
                        val href = item.getJSONObject("href")
                        if (href.has("transition")) {
                            // nothing
                        } else {
                            href.put("transition", "switchtab")
                        }
                        action.put("options", href)
                        href(action, JSONObject(), JSONObject(), this@JasonViewActivity)
                    } else if (item.has("action")) {
                        call(item["action"].toString(), "{}", "{}", this@JasonViewActivity)
                        return@OnTabSelectedListener false
                    } else if (item.has("url")) {
                        val url = item.getString("url")
                        val action = JSONObject()
                        val options = JSONObject()
                        options.put("url", url)
                        options.put("transition", "switchtab")
                        if (item.has("preload")) {
                            options.put("preload", item.getJSONObject("preload"))
                        }
                        action.put("options", options)
                        href(action, JSONObject(), JSONObject(), this@JasonViewActivity)
                    }
                } catch (e: Exception) {
                    Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
                }
                true
            })
            listView!!.clipToPadding = false
            listView!!.setPadding(0, 0, 0, 160)
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    }

    private fun setup_layers(layers: JSONArray?) {
        try {
            if (layer_items != null) {
                for (j in layer_items!!.indices) {
                    val layerView = layer_items!![j]
                    rootLayout!!.removeView(layerView)
                }
                layer_items = ArrayList()
            }
            if (layers != null) {
                for (i in 0 until layers.length()) {
                    val layer = layers.getJSONObject(i)
                    if (layer.has("type")) {
                        val view: View = JasonComponentFactory.Companion.build(
                            null,
                            layer,
                            null,
                            this@JasonViewActivity
                        )
                        JasonComponentFactory.Companion.build(
                            view,
                            layer,
                            null,
                            this@JasonViewActivity
                        )
                        stylize_layer(view, layer)
                        rootLayout!!.addView(view)
                        layer_items!!.add(view)
                    }
                }
            }
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    }

    private fun stylize_layer(view: View, component: JSONObject) {
        try {
            val style = JasonHelper.style(component, this)
            val params = view.layoutParams as RelativeLayout.LayoutParams
            if (style.has("top")) {
                val top =
                    JasonHelper.pixels(this@JasonViewActivity, style.getString("top"), "vertical")
                        .toInt()
                params.topMargin = top
                params.addRule(RelativeLayout.ALIGN_PARENT_TOP)
            }
            if (style.has("left")) {
                val left = JasonHelper.pixels(
                    this@JasonViewActivity,
                    style.getString("left"),
                    "horizontal"
                )
                    .toInt()
                params.leftMargin = left
                params.addRule(RelativeLayout.ALIGN_PARENT_LEFT)
            }
            if (style.has("right")) {
                val right = JasonHelper.pixels(
                    this@JasonViewActivity,
                    style.getString("right"),
                    "horizontal"
                )
                    .toInt()
                params.rightMargin = right
                params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
            }
            if (style.has("bottom")) {
                val bottom = JasonHelper.pixels(
                    this@JasonViewActivity,
                    style.getString("bottom"),
                    "vertical"
                )
                    .toInt()
                params.bottomMargin = bottom
                params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
            }
            view.layoutParams = params
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    }

    // Menu
    // Меню
    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        var menu = menu
        try {
            menu = toolbar!!.menu
            if (model!!.rendered != null) {
                if (!model!!.rendered!!.has("header")) {
                    setup_title(JSONObject())
                }
                val header = model!!.rendered!!.getJSONObject("header")
                header_height = toolbar!!.height
                setup_title(header)
                if (header.has("search")) {
                    val searchManager = getSystemService(SEARCH_SERVICE) as SearchManager
                    val search = header.getJSONObject("search")
                    if (searchView == null) {
                        searchView = SearchView(this)
                        searchView!!.setSearchableInfo(searchManager.getSearchableInfo(componentName))

                        // put the search icon on the right hand side of the toolbar
                        // помещаем значок поиска в правой части панели инструментов
                        searchView!!.layoutParams = Toolbar.LayoutParams(Gravity.RIGHT)
                        toolbar!!.addView(searchView)
                    } else {
                        searchView!!.setSearchableInfo(searchManager.getSearchableInfo(componentName))
                    }

                    // styling
                    // стиль

                    // color
                    // цвет
                    val c: Int
                    c = if (search.has("style") && search.getJSONObject("style").has("color")) {
                        JasonHelper.parse_color(search.getJSONObject("style").getString("color"))
                    } else if (header.has("style") && header.getJSONObject("style").has("color")) {
                        JasonHelper.parse_color(header.getJSONObject("style").getString("color"))
                    } else {
                        -1
                    }
                    if (c > 0) {
                        // AA    ImageView searchButton = (ImageView) searchView.findViewById(androidx.appcompat.appcompat.R.id.search_button);
                        val searchButton =
                            searchView!!.findViewById<ImageView>(androidx.appcompat.R.id.search_button)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            searchButton.imageTintList = ColorStateList.valueOf(
                                JasonHelper.parse_color(
                                    header.getJSONObject("style").getString("color")
                                )
                            )
                        }
                    }

                    // background
                    // фон
                    if (search.has("style") && search.getJSONObject("style").has("background")) {
                        val bc = JasonHelper.parse_color(
                            search.getJSONObject("style").getString("background")
                        )
                        searchView!!.setBackgroundColor(bc)
                    }

                    // placeholder
                    // заполнитель
                    if (search.has("placeholder")) {
                        searchView!!.queryHint = search.getString("placeholder")
                    }
                    searchView!!.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                        override fun onQueryTextSubmit(s: String): Boolean {
                            // name
                            if (search.has("name")) {
                                try {
                                    val kv = JSONObject()
                                    kv.put(search.getString("name"), s)
                                    model!!.`var` = JasonHelper.merge(model!!.`var`, kv)
                                    if (search.has("action")) {
                                        call(
                                            search.getJSONObject("action").toString(),
                                            JSONObject().toString(),
                                            "{}",
                                            this@JasonViewActivity
                                        )
                                    }
                                } catch (e: Exception) {
                                    Log.d(
                                        "Warning",
                                        e.stackTrace[0].methodName + " : " + e.toString()
                                    )
                                }
                            }
                            return false
                        }

                        override fun onQueryTextChange(s: String): Boolean {
                            return if (search.has("action")) {
                                false
                            } else {
                                if (listView != null) {
                                    val adapter = listView!!.adapter as ItemAdapter?
                                    adapter!!.filter(s)
                                }
                                true
                            }
                        }
                    })
                }
                if (header.has("menu")) {
                    val json = header.getJSONObject("menu")
                    val item = menu.add("Menu")
                    MenuItemCompat.setShowAsAction(item, MenuItem.SHOW_AS_ACTION_ALWAYS)

                    // We're going to create a button.
                    // Мы собираемся создать кнопку.
                    json.put("type", "button")

                    // if it's an image button, both url and image should work
                    // если это кнопка с изображением, то и URL, и изображение должны работать
                    if (json.has("image")) {
                        json.put("url", json.getString("image"))
                    }

                    // let's override the style so the menu button size has a sane dimension
                    // давайте переопределим стиль, чтобы размер кнопки меню имел нормальное измерение
                    val style: JSONObject
                    style = if (json.has("style")) {
                        json.getJSONObject("style")
                    } else {
                        JSONObject()
                    }

                    // For image, limit the height so it doesn't look too big
                    // Для изображения ограничиваем высоту, чтобы она не выглядела слишком большой
                    if (json.has("url")) {
                        style.put("height", JasonHelper.pixels(this, "8", "vertical").toDouble())
                    }
                    json.put("style", style)

                    // Now creating the menuButton and itemview
                    // Теперь создаем menuButton и itemview
                    val itemView: FrameLayout
                    val menuButton: View
                    if (MenuItemCompat.getActionView(item) == null) {
                        // Create itemView if it doesn't exist yet
                        // Создать itemView, если он еще не существует
                        itemView = FrameLayout(this)
                        menuButton = JasonComponentFactory.Companion.build(
                            null,
                            json,
                            null,
                            this@JasonViewActivity
                        )
                        JasonComponentFactory.Companion.build(
                            menuButton,
                            json,
                            null,
                            this@JasonViewActivity
                        )
                        itemView.addView(menuButton)
                        MenuItemCompat.setActionView(item, itemView)
                    } else {
                        // Reuse the itemView if it already exists
                        // Повторно использовать itemView, если он уже существует
                        itemView = MenuItemCompat.getActionView(item) as FrameLayout
                        menuButton = itemView.getChildAt(0)
                        JasonComponentFactory.Companion.build(
                            menuButton,
                            json,
                            null,
                            this@JasonViewActivity
                        )
                    }
                    val lp = FrameLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT
                    )
                    menuButton.layoutParams = lp

                    // Set padding for the image menu button
                    // Установить отступ для кнопки меню изображения
                    if (json.has("image")) {
                        val padding = JasonHelper.pixels(this, "15", "vertical").toInt()
                        itemView.setPadding(padding, padding, padding, padding)
                    }
                    if (json.has("badge")) {
                        var badge_text = ""
                        val badge = json.getJSONObject("badge")
                        if (badge.has("text")) {
                            badge_text = badge["text"].toString()
                        }
                        val badge_style: JSONObject
                        badge_style = if (badge.has("style")) {
                            badge.getJSONObject("style")
                        } else {
                            JSONObject()
                        }
                        var color = JasonHelper.parse_color("#ffffff")
                        var background = JasonHelper.parse_color("#ff0000")
                        if (badge_style.has("color")) color =
                            JasonHelper.parse_color(badge_style.getString("color"))
                        if (badge_style.has("background")) background =
                            JasonHelper.parse_color(badge_style.getString("background"))
                        val v = MaterialBadgeTextView(this)
                        v.setBackgroundColor(background)
                        v.setTextColor(color)
                        v.text = badge_text
                        val layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.WRAP_CONTENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT
                        )
                        //layoutParams.gravity = Gravity.RIGHT | Gravity.TOP;
                        var left = JasonHelper.pixels(this, 30.toString(), "horizontal").toInt()
                        var top = JasonHelper.pixels(this, -3.toString(), "vertical").toInt()
                        if (badge_style.has("left")) {
                            left = JasonHelper.pixels(
                                this,
                                badge_style.getString("left"),
                                "horizontal"
                            )
                                .toInt()
                        }
                        if (badge_style.has("top")) {
                            top = JasonHelper.pixels(
                                this,
                                badge_style.getString("top").toInt().toString(),
                                "vertical"
                            )
                                .toInt()
                        }
                        layoutParams.setMargins(left, top, 0, 0)
                        itemView.addView(v)
                        v.layoutParams = layoutParams
                        itemView.clipChildren = false
                        itemView.clipToPadding = false
                    }
                    item.setOnMenuItemClickListener {
                        try {
                            val header = model!!.rendered!!.getJSONObject("header")
                            if (header.has("menu")) {
                                if (header.getJSONObject("menu").has("action")) {
                                    call(
                                        header.getJSONObject("menu").getJSONObject("action")
                                            .toString(),
                                        JSONObject().toString(),
                                        "{}",
                                        this@JasonViewActivity
                                    )
                                } else if (header.getJSONObject("menu").has("href")) {
                                    val action = JSONObject().put("type", "\$href").put(
                                        "options",
                                        header.getJSONObject("menu").getJSONObject("href")
                                    )
                                    call(
                                        action.toString(),
                                        JSONObject().toString(),
                                        "{}",
                                        this@JasonViewActivity
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
                        }
                        true
                    }
                }
            }
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
        return super.onPrepareOptionsMenu(menu)
    }

    fun setup_title(header: JSONObject) {
        try {
            // Default title values
            // Значения заголовка по умолчанию
            toolbar!!.title = ""
            toolbar!!.setTitleSize(20f)

            // Global font
            // Глобальный шрифт
            if (header.has("style")) {
                toolbar!!.setTitleFont(header.getJSONObject("style"))
            }
            if (header.has("title")) {
                val title = header["title"]

                // set align:center by default
                // установить выравнивание: центр по умолчанию
                toolbar!!.setAlignment(Gravity.CENTER)
                if (title is JSONObject) {
                    val t = title
                    val type = t.getString("type")
                    var style: JSONObject? = null
                    if (t.has("style")) {
                        style = t.getJSONObject("style")
                    }
                    if (style != null) {
                        // title alignment
                        // выравнивание заголовка
                        val align: String
                        toolbar!!.setAlignment(-1)
                        try {
                            align = style.getString("align")
                            if (align == "center") {
                                toolbar!!.setAlignment(Gravity.CENTER)
                            } else if (align == "left") {
                                toolbar!!.setAlignment(Gravity.LEFT)
                            }
                        } catch (e: JSONException) {
                        }

                        // offsets
                        // смещения
                        var leftOffset = 0
                        var topOffset = 0
                        try {
                            leftOffset = JasonHelper.pixels(
                                this@JasonViewActivity,
                                style.getString("left"),
                                "horizontal"
                            )
                                .toInt()
                        } catch (e: JSONException) {
                        }
                        try {
                            topOffset = JasonHelper.pixels(
                                this@JasonViewActivity,
                                style.getString("top"),
                                "vertical"
                            )
                                .toInt()
                        } catch (e: JSONException) {
                        }
                        toolbar!!.setLeftOffset(leftOffset)
                        toolbar!!.setTopOffset(topOffset)
                    }

                    // image options
                    // параметры изображения
                    if (type.equals("image", ignoreCase = true)) {
                        val url = t.getString("url")
                        val c = JSONObject()
                        c.put("url", url)
                        var height = header_height
                        var width = Toolbar.LayoutParams.WRAP_CONTENT
                        if (style != null) {
                            if (style.has("height")) {
                                try {
                                    height = JasonHelper.pixels(
                                        this,
                                        style.getString("height"),
                                        "vertical"
                                    )
                                        .toInt()
                                } catch (e: Exception) {
                                    Log.d(
                                        "Warning",
                                        e.stackTrace[0].methodName + " : " + e.toString()
                                    )
                                }
                            }
                            if (style.has("width")) {
                                try {
                                    width = JasonHelper.pixels(
                                        this,
                                        style.getString("width"),
                                        "horizontal"
                                    )
                                        .toInt()
                                } catch (e: Exception) {
                                    Log.d(
                                        "Warning",
                                        e.stackTrace[0].methodName + " : " + e.toString()
                                    )
                                }
                            }
                        }
                        toolbar!!.setImageHeight(height)
                        toolbar!!.setImageWidth(width)
                        toolbar!!.setImage(c)
                    } else if (type.equals("label", ignoreCase = true)) {
                        val text = title.getString("text")
                        if (style != null) {
                            // size
                            // размер
                            try {
                                toolbar!!.setTitleSize(style.getString("size").toFloat())
                            } catch (e: JSONException) {
                            }

                            // font
                            toolbar!!.setTitleFont(style)
                        }
                        toolbar!!.title = text
                        if (logoView != null) {
                            toolbar!!.removeView(logoView)
                            logoView = null
                        }
                    } else {
                        if (logoView != null) {
                            toolbar!!.removeView(logoView)
                            logoView = null
                        }
                    }
                } else {
                    val simple_title = header["title"].toString()
                    toolbar!!.title = simple_title
                    if (logoView != null) {
                        toolbar!!.removeView(logoView)
                        logoView = null
                    }
                }
            } else {
                if (logoView != null) {
                    toolbar!!.removeView(logoView)
                    logoView = null
                }
            }
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    }
    /******************
     * Event listeners
     * Слушатели событий
     */
    /**
     * Enables components, or anyone with access to this activity, to listen for item touch events
     * on listView. If the same listener is passed more than once, only the first listener is added.
     * @param listener
     * Позволяет компонентам или всем, кто имеет доступ к этому действию,
     * прослушивать события касания элемента на просмотр списка.
     * Если один и тот же слушатель пропущен более одного раза,
     * добавляется только первый слушатель.
     * @param listener
     */
    fun addListViewOnItemTouchListener(listener: OnItemTouchListener) {
        if (!listViewOnItemTouchListeners!!.contains(listener)) {
            listViewOnItemTouchListeners!!.add(listener)
            listView!!.addOnItemTouchListener(listener)
        }
    }

    /**
     * Removes all item touch listeners attached to this activity
     * Called when the activity
     * Удаляет всех слушателей касания элемента, связанных с этим действием
     * Вызывается, когда деятельность
     */
    fun removeListViewOnItemTouchListeners() {
        for (listener in listViewOnItemTouchListeners!!) {
            listView!!.removeOnItemTouchListener(listener)
            listViewOnItemTouchListeners!!.remove(listener)
        }
    }

    // Результат запроса разрешения у пользователя следует обрабатывать в
    // onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults).
    // Параметры requestCode и permissions содержат данные, которые вы передавали при запросе разрешений.
    // Основные данные здесь несет массив grantResults, в котором находится информация о том, получены разрешения или нет.
    // Каждому i-му элементу permissions соответствует i-ый элемент из grantResults.
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            cameraManager!!.startVision(this@JasonViewActivity)
        } else {
            Log.d("Warning", "Waiting for permission approval")
        }
    }
} // конец JasonViewActivity
