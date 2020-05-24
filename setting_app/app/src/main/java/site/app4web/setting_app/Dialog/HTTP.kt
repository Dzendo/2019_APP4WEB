package site.app4web.setting_app.Dialog

import android.app.Dialog
import android.os.Bundle
import android.view.KeyEvent
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.appcompat.app.AppCompatDialogFragment
import site.app4web.setting_app.Launcher.Launcher.setting

// AlertDialog с собственной разметкой
class HTTP : AppCompatDialogFragment() , TextView.OnEditorActionListener {
    lateinit var  HttpView : TextView
    lateinit var  HTTPeditText : EditText
    lateinit var  AnswerHTTPView : TextView

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val builder = AlertDialog.Builder(activity!!)
        val inflater = activity!!.layoutInflater
        val view = inflater.inflate(site.app4web.setting_app.R.layout.http_dialog, null)
        builder.setView(view)

        // Добавляем слушателя к компонентам
        HTTPeditText = view.findViewById(site.app4web.setting_app.R.id.HTTPeditText) as EditText
        HTTPeditText.setText(setting.http_setting)
        HTTPeditText.setOnEditorActionListener(this)
        AnswerHTTPView = view.findViewById(site.app4web.setting_app.R.id.AnswerHttpView) as TextView
        HttpView = view.findViewById(site.app4web.setting_app.R.id.HttpView) as TextView
        HTTPeditText.requestFocus()
        HttpView.text = setting._VersionSetting
        return builder
            .setTitle("Путь Setting")
            .create()
    }

    override fun onEditorAction(HTTPeditText: TextView, actionId: Int, event: KeyEvent?): Boolean {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            AnswerHTTPView.text = HTTPeditText.text
            setting.ReadSetting(HTTPeditText.text.toString())
            HttpView.text = setting._VersionSetting
           // dialog!!.cancel()
            return true
        }
        return  false
    }
}

