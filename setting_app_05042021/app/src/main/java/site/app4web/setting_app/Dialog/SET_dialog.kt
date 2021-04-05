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
class SET_dialog : AppCompatDialogFragment() {  // , TextView.OnEditorActionListener {
    lateinit var  SetView : TextView
    lateinit var  SetEditText : EditText
    lateinit var  AnswerSetView : TextView

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val builder = AlertDialog.Builder(activity!!)
        val inflater = activity!!.layoutInflater
        val view = inflater.inflate(site.app4web.setting_app.R.layout.set_dialog, null)
        builder.setView(view)

     /*   // Добавляем слушателя к компонентам
       // SetEditText = view.findViewById(site.app4web.setting_app.R.id.SetEditText) as EditText
        SetEditText.setText(setting._VersionSetting)
        SetEditText.setOnEditorActionListener(this)
        //AnswerSetView = view.findViewById(site.app4web.setting_app.R.id.AnswerSetView) as TextView
        //SetView = view.findViewById(site.app4web.setting_app.R.id.SetView) as TextView
        SetEditText.requestFocus()
        SetView.text = setting._VersionSetting
        AnswerSetView.text = setting!!.number_template.toString()
      */
        return builder
            .setTitle("SETTING")
            .create()
    }

 /*   override fun onEditorAction(SeteditText: TextView, actionId: Int, event: KeyEvent?): Boolean {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            AnswerSetView.text = SeteditText.text
            //setting.ReadSetting(HTTPeditText.text.toString())
            setting._VersionSetting = SeteditText.text.toString()
            SetView.text = setting._VersionSetting
           // dialog!!.cancel()
            return true
        }
        return  false
    } */
}

