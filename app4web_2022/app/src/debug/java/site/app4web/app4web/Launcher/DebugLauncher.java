package site.app4web.app4web.Launcher;

import android.content.res.Resources;
import android.util.Log;
import com.facebook.stetho.Stetho;
import com.facebook.stetho.okhttp3.StethoInterceptor;
import site.app4web.app4web.R;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import timber.log.Timber;

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

public class DebugLauncher extends Launcher {
    // DebugLauncher через super.onCreate(); вызывает Launcher как extends
    // Launcher через super.onCreate(); вызывает Application
    // и затем как-то стартует Activity из манифеста
    // name=android.intent.action.MAIN
    // android.intent.category.LAUNCHER
    // и через super.onCreate() будут вызваны по цепочке

    private static final String LOGTAG = DebugLauncher.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();
        // инициализируется отладчик по простейшиму варианту :
        Stetho.initializeWithDefaults(this);

        Resources res = getResources();
        boolean enableStethoConsole = res.getBoolean(R.bool.enableStethoConsole);
        // считывает enableStethoConsole true - false из values/bools.xml

        if (enableStethoConsole) { // Надо выводить отладку в консоль
            Timber.plant(new ConfigurableStethoTree(new ConfigurableStethoTree.Configuration.Builder()
                   .showTags(true)
                   .minimumPriority(Log.DEBUG)
                   .build()));
            Log.i(LOGTAG, "Using Stetho console logging");
        } else  {  // НЕ надо выводить отладку в консоль
            Timber.plant(new Timber.DebugTree());
        }
        Timber.i("Initialised Stetho debugging"+getEnv());
    }

    // Stetho отслеживать сетевые запросы OkHttp 3.x  расписать что делает:
    @Override
    public OkHttpClient getHttpClient(long timeout) {  // вызывается из Launcher getHttpClient
        if(timeout > 0) {
            return new OkHttpClient.Builder()
                    .readTimeout(timeout, TimeUnit.SECONDS)
                    .writeTimeout(timeout, TimeUnit.SECONDS)
                    .addNetworkInterceptor(new StethoInterceptor())
                    .build();
        } else {
            return new OkHttpClient.Builder()
                    .addNetworkInterceptor(new StethoInterceptor())
                    .build();
        }
    }

}

/* Для вызова Stesho Необходимо иметь Application и указать ее в Manifest:
В Jasonette Это сделано в два яруса:
DebugManifest:   android:name=".Launcher.DebugLauncher"   	<activity    android:name=".Core.JasonViewActivity"
Manifest 	     android:name=".Launcher.Launcher"	    	<activity    android:name=".Core.JasonViewActivity"
При этом DebugLauncher наследует Launcher а это наследует Application:
public class DebugLauncher extends Launcher {
public class      Launcher extends Application {
В обоих манифестах:
<activity    android:name=".Core.JasonViewActivity"

Т.о. в рабочем варианте стартует APP:
Launcher в onCreate он зовет On Create из материнского из Application
ну и далее JasonViewActivity

В в отладочном варианте стартует APP:
Сначала стартует DebugLauncher из его nCreate зовется Launcher onCreate: он зовет On Create из материнского из Application
инициализируется отладчик по простейшиму варианту :  Stetho.initializeWithDefaults(this); +  // OkHttp 3.x
ну и далее JasonViewActivity

В DebugLauncher из src/debug/res/values/bools.xml берется:
   <bool name="enableStethoConsole">true</bool>  -- вкл ведение журнала консоли Stetho Timber
<!--bool name="enableStethoConsole">false</bool--> выкл ведение журнала консоли Stetho

И ВКЛЮЧАЕТСЯ отладчик STETHO + отслеживать сетевые запросы OkHttp 3.x :

Далее см .Launcher.Launcher
*/