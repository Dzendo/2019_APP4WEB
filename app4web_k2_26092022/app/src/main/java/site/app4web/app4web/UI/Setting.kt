package site.app4web.app4web.UI

import android.content.Context
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import site.app4web.app4web.Launcher.Launcher


// класс описания Setting  для web4app
class  Setting  {

        var setting: Setting = this
        var url_http : String = ""
        // var Logo = "app4webline.png"
        var Logo = "https://DinaDurykina.github.io/app4webline.png"

        var Reclama = false
        var AdMob_BANNER = true
    // Для Firebase и AdMob
        var ID_APP = "ca-app-pub-4136371087370112~6171379650"
        var id_BANNER0 = "ca-app-pub-4136371087370112/2298996526"
        var MyBANNER = "HTTP"
    // Данные для поиска Google Search
        var myCX = "003159383193150926956:c6m9ah2oy8e" //Your search engine
        var myKey = "AIzaSyCL_iY2ALQxC4w6Qfld253adeI_GGl_bmw"
        var myApp = "APP4WEB"

        var OtherReclama = true
        var http_setting = "https://DinaDurykina.github.io/web4app_setting.json"
        var http_gorizont = "https://1DinaDurykina.github.io/web4app_gorizont.json"
        var http_vertical = "https://1DinaDurykina.github.io/web4app_vertical.json"
        var Template: Array<String> = arrayOf(
            "https://DinaDurykina.github.io/#jason.json",
            "https://DinaDurykina.github.io/TPL01RN.json",
            "https://DinaDurykina.github.io/TPL01RNS.json",
            "https://DinaDurykina.github.io/TPL011NNS.json",
            "https://DinaDurykina.github.io/TPL011NN.json",
            "https://DinaDurykina.github.io/TPL011NNFAB.json",
            "https://DinaDurykina.github.io/TPL011NNFABS.json"
        )
        var urltemplate = Template[0]
        var NoIcon = "https://DinaDurykina.github.io/icons8-глобус-24.png"
        var Toast = false
        var Snekbar = true
        var Push = true


    fun Read_Setting(context:Context,http_setting:String) {

        // Асинхронное считывание файла результат content должен быть не Null
        val content = ProgressTask().execute(http_setting).get()
        // разбор считанного JSON
        try {
            Launcher.setting = GsonBuilder().create().fromJson(content, Setting::class.java)
        } catch (e: JsonSyntaxException) {
            e.printStackTrace()
        }
        this.http_setting =  http_setting //getString(R.string.http_setting)
        android.widget.Toast.makeText(context, "setting загружен", android.widget.Toast.LENGTH_SHORT).show()
    }
    companion object {
        @JvmStatic
        fun CreateSetting(http_setting_in: String?) : Setting? {
            var http_setting = "https://DinaDurykina.github.io/web4app_setting.json"
            if (http_setting_in != null) http_setting = http_setting_in
            // Асинхронное считывание файла результат content должен быть не Null
            val content = ProgressTask().execute(http_setting).get()
            if (content=="Null") return Setting()
            // разбор считанного JSON
             val set=GsonBuilder().create().fromJson(content, Setting::class.java)
             if (set==null) return Setting()
             return set
        }
    }
}
