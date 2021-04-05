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
class GORIZONT : AppCompatDialogFragment() , TextView.OnEditorActionListener {
    lateinit var  GorizontView : TextView
    lateinit var  GorizonteditText : EditText
    lateinit var  AnswerGorizontView : TextView

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val builder = AlertDialog.Builder(activity!!)
        val inflater = activity!!.layoutInflater
        val view = inflater.inflate(site.app4web.setting_app.R.layout.gorizont_dialog, null)
        builder.setView(view)

        // Добавляем слушателя к компонентам
        GorizonteditText = view.findViewById(site.app4web.setting_app.R.id.GorizonteditText) as EditText
        GorizonteditText.setText(setting.http_gorizont)
        GorizonteditText.setOnEditorActionListener(this)
        AnswerGorizontView = view.findViewById(site.app4web.setting_app.R.id.AnswerGorizontView) as TextView
        GorizontView = view.findViewById(site.app4web.setting_app.R.id.GorizontView) as TextView
        GorizonteditText.requestFocus()
        GorizontView.text = setting._VersionSetting
        return builder
            .setTitle("Путь Gorizont")
            .create()
    }

    override fun onEditorAction(GorizonteditText: TextView, actionId: Int, event: KeyEvent?): Boolean {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            AnswerGorizontView.text = GorizonteditText.text
            //setting.ReadSetting(HTTPeditText.text.toString())
            setting.http_gorizont = GorizonteditText.text.toString()
            GorizontView.text = setting._VersionSetting
           // dialog!!.cancel()
            return true
        }
        return  false
    }
}

