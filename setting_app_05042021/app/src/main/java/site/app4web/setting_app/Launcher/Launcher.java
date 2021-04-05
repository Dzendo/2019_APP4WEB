package site.app4web.setting_app.Launcher;

import android.app.Application;
import android.content.Context;
import site.app4web.setting_app.R;
import site.app4web.setting_app.UI.Setting;

public class Launcher extends Application {
    private static Context currentContext;
    public static Setting setting = new Setting();  // NO Синглетон от ДО

    // get current context from anywhere
    // получить текущий контекст из любого места
    public static Context getCurrentContext() {
        return currentContext;
    }

    public static void setCurrentContext(Context context) {
        currentContext = context;
    }

    public Launcher() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        setCurrentContext(getApplicationContext());
        //setting = Setting.CreateSetting(getApplicationContext(), getString(R.string.setting));
        //new Setting();     // Вставил ДО для Setting
        setting.setTest2(" присвоил в new launch");

    }  // end onCreate()
}



