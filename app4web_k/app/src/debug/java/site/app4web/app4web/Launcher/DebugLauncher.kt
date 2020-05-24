package site.app4web.app4web.Launcher

import android.util.Log
import com.facebook.stetho.Stetho
import com.facebook.stetho.okhttp3.StethoInterceptor
import okhttp3.OkHttpClient
import site.app4web.app4web.R
import timber.log.Timber
import timber.log.Timber.DebugTree
import java.util.concurrent.TimeUnit

/**
 * Provides debug-build specific Application.
 * Предоставляет отладочную сборку конкретного приложения.
 *
 * To disable Stetho console logging change the setting in src/debug/res/values/bools.xml
 * Чтобы отключить ведение журнала консоли Stetho, измените настройку в src/debug/res/values/bools.xml
 */
// Этот модуль включает прослушиватель - Stetho - отладчик Chrome
// Предназначена для быстрой отладки приложения.
// Stetho — Вслушиваемся в работу приложения
// Stetho - это сложный отладочный мост для приложений Android.
// При включении разработчики получают доступ к функции инструментов разработчика Chrome,
// которая изначально является частью браузера Chrome для настольных ПК.
// Разработчики также могут включить дополнительный dumpapp инструмент,
// который предлагает мощный интерфейс командной строки для внутренних компонентов приложения.
//После того, как вы выполните приведенные ниже инструкции по настройке,
// просто запустите свое приложение и наведите указатель мыши
// на браузер своего ноутбука chrome://inspect. Нажмите кнопку «Осмотреть», чтобы начать.
// 2 - site.app4web.app4web.Launcher.DebugLauncher
class DebugLauncher : Launcher() {
    override fun onCreate() {
        super.onCreate()
        // инициализируется отладчик по простейшиму варианту :
        Stetho.initializeWithDefaults(this)
        val res = resources
        val enableStethoConsole = res.getBoolean(R.bool.enableStethoConsole)
        // считывает enableStethoConsole true - false из values/bools.xml
        if (enableStethoConsole) { // Надо выводить отладку в консоль
            Timber.plant(
                ConfigurableStethoTree(
                    ConfigurableStethoTree.Configuration.Builder()
                        .showTags(true)
                        .minimumPriority(Log.DEBUG)
                        .build()
                )
            )
            Log.i(LOGTAG, "Using Stetho console logging")
        } else {  // НЕ надо выводить отладку в консоль
            Timber.plant(DebugTree())
        }
        Timber.i("Initialised Stetho debugging$env")
    }

    // Stetho отслеживать сетевые запросы OkHttp 3.x  расписать что делает:
    override fun getHttpClient(timeout: Long): OkHttpClient {  // вызывается из Launcher getHttpClient
        return if (timeout > 0) {
            OkHttpClient.Builder()
                .readTimeout(timeout, TimeUnit.SECONDS)
                .writeTimeout(timeout, TimeUnit.SECONDS)
                .addNetworkInterceptor(StethoInterceptor())
                .build()
        } else {
            OkHttpClient.Builder()
                .addNetworkInterceptor(StethoInterceptor())
                .build()
        }
    }

    companion object {
        // DebugLauncher через super.onCreate(); вызывает Launcher как extends
        // Launcher через super.onCreate(); вызывает Application
        // и затем как-то стартует Activity из манифеста
        // name=android.intent.action.MAIN
        // android.intent.category.LAUNCHER
        // и через super.onCreate() будут вызваны по цепочке
        private val LOGTAG = DebugLauncher::class.java.simpleName
    }
}