package site.app4web.setting_app.Dialog

import android.app.Dialog
import android.os.Bundle
import android.view.KeyEvent
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatDialogFragment
import site.app4web.setting_app.Launcher.Launcher.setting
import site.app4web.setting_app.R

// AlertDialog с собственной разметкой
class HTTP_dialog : AppCompatDialogFragment() { //, TextView.OnEditorActionListener {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val title = "ВВЕДИТЕ ПУТЬ"
        val message = "Это и есть ПУТЬ"
        val buttonPositiveString = "OK"
        val buttonNegativeString = "Cancel"
        val HTTPeditText : EditText =  EditText(context)
        //HTTPeditText.setOnEditorActionListener(this)
        //HTTPeditText.inputType = android.text.InputType.TYPE_TEXT_VARIATION_URI
        // TYPE_TEXT_FLAG_MULTI_LINE
       // HTTPeditText.gravity = 1
        HTTPeditText.setText (setting.http_setting)
        HTTPeditText.requestFocus()

        val builder = AlertDialog.Builder(activity!!)
        builder.setTitle(title)  // заголовок
        //builder.setMessage(message) // сообщение
        //builder.setIcon(R.drawable.app4webline)
        builder.setView(HTTPeditText)
        builder.setPositiveButton(buttonPositiveString) { dialog, id ->
            // builder.setMessage(message + "setting")
           // Toast.makeText(activity, "Вы выбрали OK " + HTTPeditText.text.toString(), Toast.LENGTH_LONG ).show()
            var setbool = setting.ReadSetting(HTTPeditText.text.toString())
            Toast.makeText(activity, ">>>>> $setbool <<<<<  read SETTING \n" + HTTPeditText.text.toString(), Toast.LENGTH_LONG ).show()
            //Toast.makeText(activity, "Read ${HTTPeditText.text.toString()}", Toast.LENGTH_LONG).show()
            // проверить setbool и нафиг
            setbool = setting.ReadTemplate(setting.urltemplate)      // считываю нулевую обертку из Setting только считанного
            Toast.makeText(activity, "<<<<< $setbool >>>>>  read TEMPLATE \n" + setting.urltemplate.toString(), Toast.LENGTH_LONG ).show()
           // dialog!!.cancel()
        }
        builder.setNegativeButton(buttonNegativeString) { dialog, id ->
            Toast.makeText(activity, "Вы выбрали Cancel", Toast.LENGTH_LONG).show()
        }
        builder.setCancelable(true)

        return builder.create()

    }
/*
    override fun onEditorAction(HTTPeditText: TextView, actionId: Int, event: KeyEvent?): Boolean {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            setting.ReadSetting(HTTPeditText.text.toString())
            Toast.makeText(activity, "Read ${HTTPeditText.text.toString()}", Toast.LENGTH_LONG).show()
           // dialog!!.cancel()
            return true
        }
        return  false
    }
    */
}

