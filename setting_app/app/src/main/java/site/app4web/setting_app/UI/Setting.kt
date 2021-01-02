package site.app4web.setting_app.UI

import android.content.Context
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import org.json.JSONObject
import java.io.IOException
import android.graphics.drawable.Drawable
import com.google.gson.Gson
import site.app4web.setting_app.Launcher.Launcher

// класс описания Setting  для web4app превратить в data class потом
// data class  Setting (var _VersionSetting : String = "setClass",var setting: Setting ) {
class  Setting  {
        var Test1 : String = "test1 Class"
        var Test2 : String = "test2 Class"
        var Test3 : String = "test3 Class"
        var _VersionSetting : String = "setClass"
        var setting: Setting = this
        var url_http : String = ""
        // var Logo = "app4webline.png"
        var Logo = "https://DinaDurykina.github.io/app4webline.png"
        lateinit var LogoImg : Drawable

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
        var http_calc = "https://DinaDurykina.github.io/calc_Platform_NoImage_err.json"
        var http_help = "https://DinaDurykina.github.io/calc_Platform_NoImage.json"
        var http_setting = "https://DinaDurykina.github.io/web4app_setting.json"
        var http_gorizont = "https://1DinaDurykina.github.io/web4app_gorizont.json"
        var http_vertical = "https://1DinaDurykina.github.io/web4app_vertical.json"
        lateinit var settingJson  :JSONObject
        lateinit var gorizontJson :JSONObject
        lateinit var verticalJson :JSONObject
        var template : String = "{ " +
                "  \"\$jason\": {" +
                "    \"head\": {" +
                "      \"title\": \"wrap\"," +
                "      \"actions\": { \"\$load\": { \"type\": \"\$render\" } }" +
                "    }," +
                "    \"body\": {" +
                "      \"background\": {" +
                "        \"type\": \"html\"," +
                "        \"url\": \"http://####.###/\"," +
                "        \"action\": { \"type\": \"\$default\" }" +
                "      }" +
                "    }" +
                "  }" +
                "}"
        data class  template_name_url (val name_template : String, val url_template : String)
        var number_template = 0
        var Name_Template : Array<template_name_url> = arrayOf (
            template_name_url("0_Mobile","https://afalinalv.github.io/jason7.json"),
            template_name_url("1_Native","https://DinaDurykina.github.io/TPL01RN.json"),
            template_name_url("2_Reactive","https://DinaDurykina.github.io/TPL01RNS.json"),
            template_name_url("3_Reactive Native","https://DinaDurykina.github.io/TPL011NNS.json"),
            template_name_url("4_Native tab","https://DinaDurykina.github.io/TPL011NN.json"),
            template_name_url("5_Reactive tab","https://DinaDurykina.github.io/TPL011NNFAB.json"),
            template_name_url("6_Reactive Native tab","https://DinaDurykina.github.io/TPL011NNFABS")
        )
//Depricate Delete, в том числе из DD и переделать на то, что выше
       /* var Template: Array<String> = arrayOf(
            "https://DinaDurykina.github.io/#jason.json",
            "https://DinaDurykina.github.io/TPL01RN.json",
            "https://DinaDurykina.github.io/TPL01RNS.json",
            "https://DinaDurykina.github.io/TPL011NNS.json",
            "https://DinaDurykina.github.io/TPL011NN.json",
            "https://DinaDurykina.github.io/TPL011NNFAB.json",
            "https://DinaDurykina.github.io/TPL011NNFABS.json"
        )*/
        var nametemplate = Name_Template[0].name_template
        var urltemplate  = Name_Template[0].url_template
       //var urltemplate = Template[0]
        var NoIcon = "https://DinaDurykina.github.io/icons8-глобус-24_err.png"
        lateinit var NoIconImg :  Drawable
        var Toast = false
        var Snekbar = true
        var Push = true
        var Pin = "2522"
        var PinTry = 7
        var PinOK =false


    fun ReadSetting(http_setting:String?): Boolean {

        // Асинхронное считывание файла результат content должен быть не Null
        val content: String = ProgressTask().execute(http_setting).get()
        // разбор считанного JSON
        if (content=="Null") return false  // что нибудь сказать надо бы что нет файда
        try {
            // Launcher.setting = GsonBuilder().create().fromJson(content, Setting::class.java)
          Launcher.setting = Gson().fromJson(content, Setting::class.java)
        } catch (e: JsonSyntaxException) {
            e.printStackTrace()
            return false
        }
        this.http_setting =  http_setting!! //getString(R.string.http_setting)
        //android.widget.Toast.makeText(context, "setting загружен", android.widget.Toast.LENGTH_SHORT).show()
        // ReadTemplate(urltemplate)
       // ReadTemplate(Launcher.setting.urltemplate) будет работать неправильно из старого экземляра объекта в новый ???
        return true
    }
    fun ReadTemplate(http_template:String?): Boolean {

        // Асинхронное считывание файла результат content должен быть не Null
        val content = ProgressTask().execute(http_template)//.get()
        if (content == "Null") return false
        setting.template = content
        setting.nametemplate = Name_Template[this.number_template].name_template
        setting.urltemplate  = Name_Template[this.number_template].url_template
        return true
    }
   }
