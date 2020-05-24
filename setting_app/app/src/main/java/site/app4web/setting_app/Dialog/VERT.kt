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
class VERT : AppCompatDialogFragment() , TextView.OnEditorActionListener {
    lateinit var  VertView : TextView
    lateinit var  VerteditText : EditText
    lateinit var  AnswerVertView : TextView

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val builder = AlertDialog.Builder(activity!!)
        val inflater = activity!!.layoutInflater
        val view = inflater.inflate(site.app4web.setting_app.R.layout.vertical_dialog, null)
        builder.setView(view)

        // Добавляем слушателя к компонентам
        VerteditText = view.findViewById(site.app4web.setting_app.R.id.VerteditText) as EditText
        VerteditText.setText(setting.http_vertical)
        VerteditText.setOnEditorActionListener(this)
        AnswerVertView = view.findViewById(site.app4web.setting_app.R.id.AnswerVertView) as TextView
        VertView = view.findViewById(site.app4web.setting_app.R.id.VertView) as TextView
        VerteditText.requestFocus()
        VertView.text = setting._VersionSetting
        return builder
            .setTitle("Путь Vertical")
            .create()
    }

    override fun onEditorAction(VerteditText: TextView, actionId: Int, event: KeyEvent?): Boolean {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            AnswerVertView.text = VerteditText.text
            //setting.ReadSetting(VerteditText.text.toString())
            setting.http_gorizont = VerteditText.text.toString()
            VertView.text = setting._VersionSetting
           // dialog!!.cancel()
            return true
        }
        return  false
    }
}

