package site.app4web.setting_app.Dialog

import android.app.Dialog
import android.os.Bundle
import android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
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
class PIN_dialog : AppCompatDialogFragment() { //, TextView.OnEditorActionListener {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val title = "ВВЕДИТЕ ПАРОЛЬ"
        val message = "Это и есть HELP"
        val buttonPositiveString = "OK"
        val buttonNegativeString = "Cancel"
        val PINeditText : EditText =  EditText(context)
       // PINeditText.setOnEditorActionListener(this)
        PINeditText.inputType = android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        PINeditText.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        //PINeditText.Pass
        //PINeditText.setInputType(12)
        PINeditText.gravity = 1
        PINeditText.setText ("")
        PINeditText.requestFocus()

        val builder = AlertDialog.Builder(activity!!)
        builder.setTitle(title)  // заголовок
        builder.setMessage(message) // сообщение
        builder.setIcon(R.drawable.app4webline)
        builder.setView(PINeditText)
        builder.setPositiveButton(buttonPositiveString) { dialog, id ->
           // builder.setMessage(message + "setting")
            Toast.makeText(activity, "Вы выбрали OK " + PINeditText.text.toString(), Toast.LENGTH_LONG ).show()
            setting.PinOK = PINeditText.text.toString() == setting.Pin
            Toast.makeText(activity, "PinOK ${setting.PinOK}", Toast.LENGTH_LONG).show()
           // dialog!!.cancel()
        }
        builder.setNegativeButton(buttonNegativeString) { dialog, id ->
            Toast.makeText(activity, "Вы выбрали Cancel", Toast.LENGTH_LONG).show()
        }
        builder.setCancelable(true)

        return builder.create()

    }
/*
    override fun onEditorAction(PINeditText: TextView, actionId: Int, event: KeyEvent?): Boolean {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            // обрабатываем нажатие кнопки поиска
            setting.PinOK = PINeditText.text.toString() == setting.Pin
            Toast.makeText(activity, "PinOK ${setting.PinOK}", Toast.LENGTH_LONG).show()

            return true
        }
        return  false

    }
    */
}

