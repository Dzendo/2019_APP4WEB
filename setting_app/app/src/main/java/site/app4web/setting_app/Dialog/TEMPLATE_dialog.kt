package site.app4web.setting_app.Dialog

import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDialogFragment
import site.app4web.setting_app.Launcher.Launcher
import site.app4web.setting_app.Launcher.Launcher.setting

// AlertDialog Без собственной разметкой
class TEMPLATE_dialog : AppCompatDialogFragment()  {    //, AdapterView.OnItemClickListener {
    var item =  Launcher.setting.number_template
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val NT:  Array <String> = Array(setting.Name_Template.size){i ->setting.Name_Template[i].name_template }

        val builder = AlertDialog.Builder(activity!!)
        builder.setTitle("Выберите шаблон")
            // добавляем переключатели
            .setSingleChoiceItems(
                NT  , Launcher.setting.number_template
            ) { dialog, item ->
                Toast.makeText(
                    activity,
                    "Шаблон: " + setting.Name_Template[item].name_template +
                    " путь : " + setting.Name_Template[item].url_template,
                    Toast.LENGTH_SHORT
                ).show()
                this.item = item
               // рано
                // Launcher.setting.number_template = item
            }
            .setPositiveButton("OK") { dialog, id ->
               // Toast.makeText(activity, "Вы выбрали OK " + id.toString() +" " + dialog.toString() , Toast.LENGTH_LONG ).show()
               // setting.ReadTemplate(setting.Name_Template[this.item].url_template)
                // User clicked OK, so save the mSelectedItems results somewhere
                // or return them to the component that opened the dialog
                // Заранее присваиваю выбранный номер шаблона - он уже используется в ReadTemplate
                Launcher.setting.number_template = this.item

                val setbool = setting.ReadTemplate(setting.Name_Template[this.item].url_template)
                Toast.makeText(activity, "<<<<< $setbool >>>>>  read TEMPLATE \n" + setting.urltemplate.toString(), Toast.LENGTH_LONG ).show()

            }
           // .setNegativeButton("Отмена") { dialog, id -> }

        return builder.create()



    }
}

