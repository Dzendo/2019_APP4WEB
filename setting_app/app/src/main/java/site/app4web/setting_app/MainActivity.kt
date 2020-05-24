package site.app4web.setting_app

import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.Toast

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import site.app4web.setting_app.Dialog.*
import site.app4web.setting_app.Launcher.Launcher.setting
import site.app4web.setting_app.UI.DoHttp

class MainActivity : AppCompatActivity() {
    lateinit var menu: Menu

    override fun onCreate(savedInstanceState: Bundle?) {
        Toast.makeText(applicationContext, "########### START default ####### ", Toast.LENGTH_LONG ).show()
        //Вызов чтения параметров программы из указанного файла - setting.http_setting
        var setbool = setting.ReadSetting(setting.http_setting)
        Toast.makeText(applicationContext, ">>>>> $setbool <<<<<  read SETTING \n" + setting.http_setting.toString(), Toast.LENGTH_LONG ).show()
        setbool = setting.ReadTemplate(setting.urltemplate)
        Toast.makeText(applicationContext, "<<<<< $setbool >>>>>  read TEMPLATE \n" + setting.urltemplate.toString(), Toast.LENGTH_LONG ).show()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val LogoView: ImageView = findViewById(R.id.LogoView)
//        LogoView.setImageDrawable(setting.LogoImg)
        val NoIconView: ImageView = findViewById(R.id.NoIconView)
//        NoIconView.setImageDrawable(setting.NoIconImg)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
             Toast.makeText(applicationContext, "PinOk = " + setting.PinOK , Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        this.menu=menu
        return true
    }
    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.setGroupVisible(R.id.group_http,setting.PinOK)
        return super.onPrepareOptionsMenu(menu)
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        return when (item.itemId) {
            R.id.action_calc ->{
                DoHttp(this, setting.http_calc)
                true
            }
            R.id.action_help ->{
                DoHttp(this, setting.http_help)
                true
            }
            R.id.action_template ->{
                val manager = supportFragmentManager
                val myDialogFragment = TEMPLATE_dialog()
                myDialogFragment.show(manager, "template")
                Hello.text = setting.urltemplate
                true
            }
            R.id.action_service ->{
                if (--setting.PinTry<0) return true
                val manager = supportFragmentManager
                val myDialogFragment = PIN_dialog()
                myDialogFragment.show(manager, "PIN")
                true
            }
            R.id.action_http_settings ->{
                val manager = supportFragmentManager
                val myDialogFragment = HTTP_dialog()
                myDialogFragment.show(manager, "http_settings")
                true
            }
            R.id.action_gor_settings ->{
                val manager = supportFragmentManager
                val myDialogFragment = GORIZONT()
                myDialogFragment.show(manager, "GORIZONT_settings")
                true
            }
            R.id.action_vert_settings ->{
                val manager = supportFragmentManager
                val myDialogFragment = VERT()
                myDialogFragment.show(manager, "VERT_settings")
                true
            }
            R.id.action_settings ->{
                //if (--setting.PinTry<0) return true
                val manager = supportFragmentManager
                val myDialogFragment = SET_dialog()
                myDialogFragment.show(manager, "SET")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
