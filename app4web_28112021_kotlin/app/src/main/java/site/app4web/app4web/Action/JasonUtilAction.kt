package site.app4web.app4web.Action

import android.app.Dialog
import android.app.TimePickerDialog
import android.content.ContentResolver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import com.google.android.material.snackbar.Snackbar
import androidx.fragment.app.DialogFragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.appcompat.app.AlertDialog
import android.text.InputType
import android.text.format.DateFormat
import android.util.Base64
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TimePicker
import android.widget.Toast
import com.github.florent37.singledateandtimepicker.dialog.SingleDateAndTimePickerDialog
import site.app4web.app4web.Core.JasonViewActivity
import site.app4web.app4web.Helper.JasonHelper
import site.app4web.app4web.Helper.JasonImageHelper
import org.json.JSONArray
import org.json.JSONObject
import java.util.ArrayList
import java.util.Calendar
import java.util.Date


class JasonUtilAction {
    private var counter // general purpose counter; счетчик общего назначения;
            = 0
    private var callback_intent // general purpose intent; намерение общего назначения;
            : Intent? = null

    fun banner(action: JSONObject, data: JSONObject?, event: JSONObject?, context: Context) {
        Handler(Looper.getMainLooper()).post {
            try {
                val options = action.getJSONObject("options")
                var title = "Notice"
                var result = ""
                if (options.has("title")) {
                    title = options["title"].toString()
                }
                result = if (options.has("description")) {
                    """
     $title
     ${options["description"]}
     """.trimIndent()
                } else {
                    title
                }
                val snackbar = Snackbar.make(
                    (context as JasonViewActivity).rootLayout!!,
                    result,
                    Snackbar.LENGTH_LONG
                )
                snackbar.show()
            } catch (e: Exception) {
                Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
            }
        }
        try {
            JasonHelper.next("success", action, JSONObject(), event, context)
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    }

    fun toast(action: JSONObject, data: JSONObject?, event: JSONObject?, context: Context?) {
        Handler(Looper.getMainLooper()).post {
            try {
                val options = action.getJSONObject("options")
                val duration = Toast.LENGTH_SHORT
                val toast = Toast.makeText(context, options["text"].toString(), duration)
                toast.show()
            } catch (e: Exception) {
                Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
            }
        }
        try {
            JasonHelper.next("success", action, JSONObject(), event, context)
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    }

    fun alert(action: JSONObject, data: JSONObject?, event: JSONObject?, context: Context) {
        Handler(Looper.getMainLooper()).post {
            val builder = AlertDialog.Builder(context)
            try {
                var options = JSONObject()
                val textFields = ArrayList<EditText>()
                if (action.has("options")) {
                    options = action.getJSONObject("options")
                    val title = options["title"].toString()
                    val description = options["description"].toString()
                    builder.setTitle(title)
                    builder.setMessage(description)
                    if (options.has("form")) {
                        val lay = LinearLayout(context)
                        lay.orientation = LinearLayout.VERTICAL
                        lay.setPadding(20, 5, 20, 5)
                        val formArr = options.getJSONArray("form")
                        for (i in 0 until formArr.length()) {
                            val obj = formArr.getJSONObject(i)
                            val textBox = EditText(context)
                            if (obj.has("placeholder")) {
                                textBox.hint = obj.getString("placeholder")
                            }
                            if (obj.has("type") && obj.getString("type") == "secure") {
                                textBox.inputType =
                                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                            }
                            if (obj.has("value")) {
                                textBox.setText(obj.getString("value"))
                            }
                            textBox.tag = obj["name"]
                            lay.addView(textBox)
                            textFields.add(textBox)
                            builder.setView(lay)
                        }
                        // Set focous on first text field
                        // Установить фокус на первое текстовое поле
                        val focousedTextField = textFields[0]
                        focousedTextField.post {
                            focousedTextField.requestFocus()
                            val lManager =
                                context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                            lManager.showSoftInput(focousedTextField, 0)
                        }
                    }
                }
                builder.setPositiveButton(
                    "OK"
                ) { dialog, which ->
                    try {
                        if (action.has("success")) {
                            val postObject = JSONObject()
                            if (action.getJSONObject("options").has("form")) {
                                for (i in textFields.indices) {
                                    val textField = textFields[i]
                                    postObject.put(
                                        textField.tag.toString(),
                                        textField.text.toString()
                                    )
                                }
                            }
                            JasonHelper.next("success", action, postObject, event, context)
                        }
                    } catch (err: Exception) {
                    }
                }
                builder.setNeutralButton(
                    "CANCEL"
                ) { dialog, which ->
                    JasonHelper.next(
                        "error",
                        action,
                        JSONObject(),
                        event,
                        context
                    )
                }
                builder.show()
            } catch (e: Exception) {
                Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
            }
        }
    }

    fun picker(action: JSONObject, data: JSONObject?, event: JSONObject?, context: Context?) {
        Handler(Looper.getMainLooper()).post {
            try {
                val options = action.getJSONObject("options")
                if (options.has("items")) {
                    val items = options.getJSONArray("items")
                    val builder = AlertDialog.Builder(
                        context!!
                    )
                    if (options.has("title")) {
                        val title = options["title"].toString()
                        builder.setTitle(title)
                    }
                    val listItems = ArrayList<String>()
                    for (i in 0 until items.length()) {
                        val item = items[i] as JSONObject
                        if (item.has("text")) {
                            listItems.add(item.getString("text"))
                        } else {
                            listItems.add("")
                        }
                    }
                    val charSequenceItems = listItems.toTypedArray<CharSequence>()
                    builder.setItems(charSequenceItems) { dialog, `val` ->
                        val item: JSONObject
                        try {
                            item = items.getJSONObject(`val`)
                            if (item.has("action")) {
                                val intent = Intent("call")
                                intent.putExtra("action", item["action"].toString())
                                LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
                            } else if (item.has("href")) {
                                val intent = Intent("call")
                                val href = JSONObject()
                                href.put("type", "\$href")
                                href.put("options", item["href"])
                                intent.putExtra("action", href.toString())
                                LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
                            }
                        } catch (e: Exception) {
                            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
                        }
                    }
                    builder.setNeutralButton(
                        "CANCEL"
                    ) { dialog, `val` -> }
                    builder.create().show()
                }
            } catch (e: Exception) {
                Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
            }
        }
        try {
            JasonHelper.next("success", action, JSONObject(), event, context)
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    }

    class TimePickerFragment : DialogFragment(), OnTimeSetListener {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            // Use the current time as the default values for the picker
            // Использовать текущее время в качестве значений по умолчанию для выбора
            val c = Calendar.getInstance()
            val hour = c[Calendar.HOUR_OF_DAY]
            val minute = c[Calendar.MINUTE]

            // Create a new instance of TimePickerDialog and return it
            // Создать новый экземпляр TimePickerDialog и вернуть его
            return TimePickerDialog(
                activity, this, hour, minute,
                DateFormat.is24HourFormat(activity)
            )
        }

        override fun onTimeSet(view: TimePicker, hourOfDay: Int, minute: Int) {
            // Do something with the time chosen by the user
            // Делать что-то со временем, выбранным пользователем
        }
    }

    fun datepicker(action: JSONObject?, data: JSONObject?, event: JSONObject?, context: Context?) {
        SingleDateAndTimePickerDialog.Builder(context)
            .bottomSheet()
            .curved() //.minutesStep(15)
            //.title("Simple")
            .listener { date ->
                try {
                    val `val` = (date.time / 1000).toString()
                    val value = JSONObject()
                    value.put("value", `val`)
                    JasonHelper.next("success", action, value, event, context)
                } catch (e: Exception) {
                    Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
                }
            }.display()
    }

    fun addressbook(action: JSONObject, data: JSONObject, event: JSONObject, context: Context) {
        Thread { getContacts(action, data, event, context) }.start()
    }

    private fun getContacts(
        action: JSONObject,
        data: JSONObject,
        event: JSONObject,
        context: Context
    ) {
        val contactList = JSONArray()
        var phoneNumber: String? = null
        var email: String? = null
        val contentResolver = context.contentResolver
        try {
            val cursor =
                contentResolver.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null)
            if (cursor!!.count > 0) {
                counter = 0
                while (cursor.moveToNext()) {
                    val contact = JSONObject()
                    val contact_id =
                        cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID))
                    val name =
                        cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))
                    try {
                        // name
                        // название
                        contact.put("name", name)

                        // phone
                        // Телефон
                        val phoneCursor = contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            arrayOf(contact_id),
                            null
                        )
                        while (phoneCursor!!.moveToNext()) {
                            phoneNumber =
                                phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                            if (phoneNumber != null) {
                                contact.put("phone", phoneNumber)
                            }
                        }
                        if (!contact.has("phone")) {
                            contact.put("phone", "")
                        }
                        phoneCursor.close()

                        // email
                        // Эл. адрес
                        val emailCursor = contentResolver.query(
                            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
                            arrayOf(contact_id),
                            null
                        )
                        while (emailCursor!!.moveToNext()) {
                            email =
                                emailCursor.getString(emailCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA))
                            if (email != null) {
                                contact.put("email", email)
                            }
                        }
                        if (!contact.has("email")) {
                            contact.put("email", "")
                        }
                        emailCursor.close()

                        // Add to array
                        // Добавить в массив
                        contactList.put(contact)
                    } catch (e: Exception) {
                        Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
                    }
                }
                try {
                    JasonHelper.next("success", action, contactList, event, context)
                } catch (e: Exception) {
                    Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
                }
            }
        } catch (e: SecurityException) {
            JasonHelper.permission_exception("\$util.addressbook", context)
        }
    }

    fun share(action: JSONObject, data: JSONObject?, event: JSONObject?, context: Context) {
        Thread {
            try {
                val options = action.getJSONObject("options")
                if (options.has("items")) {
                    callback_intent = Intent()
                    callback_intent!!.action = Intent.ACTION_SEND
                    val items = options.getJSONArray("items")
                    counter = 0
                    val l = items.length()
                    for (i in 0 until l) {
                        val item = items[i] as JSONObject
                        if (item.has("type")) {
                            val type = item.getString("type")
                            if (type.equals("text", ignoreCase = true)) {
                                callback_intent!!.putExtra(
                                    Intent.EXTRA_TEXT,
                                    item["text"].toString()
                                )
                                if (callback_intent!!.type == null) {
                                    callback_intent!!.type = "text/plain"
                                }
                                counter++
                                if (counter == l) {
                                    JasonHelper.next(
                                        "success",
                                        action,
                                        JSONObject(),
                                        event,
                                        context
                                    )
                                    context.startActivity(
                                        Intent.createChooser(
                                            callback_intent,
                                            "Share"
                                        )
                                    )
                                }
                            } else if (type.equals("image", ignoreCase = true)) {
                                // Fetch remote url
                                // Turn it into Bitmap
                                // Create a file
                                // Share the url
                                // Получить удаленный URL
                                // Превращаем его в растровое изображение
                                // Создать файл
                                // Поделиться URL
                                if (item.has("url")) {
                                    val helper = JasonImageHelper(item.getString("url"), context)
                                    helper.setListener { data1: ByteArray?, uri: Uri? ->
                                        callback_intent!!.putExtra(Intent.EXTRA_STREAM, uri)
                                        // override with image type if one of the items is an image
                                        callback_intent!!.type = "image/*"
                                        callback_intent!!.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        counter++
                                        if (counter == l) {
                                            JasonHelper.next(
                                                "success",
                                                action,
                                                JSONObject(),
                                                event,
                                                context
                                            )
                                            context.startActivity(
                                                Intent.createChooser(
                                                    callback_intent,
                                                    "Share"
                                                )
                                            )
                                        }
                                    }
                                    helper.fetch()
                                } else if (item.has("data")) {
                                    // "data" is a byte[] stored as string
                                    // so we need to restore it back to byte[] before working with it.
                                    // "data" - это byte [], хранящийся в виде строки
                                    // поэтому мы должны восстановить его обратно в byte [] перед началом работы с ним.
                                    val d = Base64.decode(item.getString("data"), Base64.DEFAULT)
                                    val helper = JasonImageHelper(d, context)
                                    helper.setListener { data12: ByteArray?, uri: Uri? ->
                                        callback_intent!!.putExtra(Intent.EXTRA_STREAM, uri)
                                        // override with image type if one of the items is an image
                                        callback_intent!!.type = "image/*"
                                        callback_intent!!.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        counter++
                                        if (counter == l) {
                                            JasonHelper.next(
                                                "success",
                                                action,
                                                JSONObject(),
                                                event,
                                                context
                                            )
                                            context.startActivity(
                                                Intent.createChooser(
                                                    callback_intent,
                                                    "Share"
                                                )
                                            )
                                        }
                                    }
                                    helper.load()
                                }
                            } else if (type.equals("video", ignoreCase = true)) {
                                if (item.has("file_url")) {
                                    val uri = Uri.parse(item.getString("file_url"))
                                    callback_intent!!.putExtra(Intent.EXTRA_STREAM, uri)
                                    // override with image type if one of the items is an image
                                    callback_intent!!.type = "video/*"
                                    callback_intent!!.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    counter++
                                    if (counter == l) {
                                        JasonHelper.next(
                                            "success",
                                            action,
                                            JSONObject(),
                                            event,
                                            context
                                        )
                                        context.startActivity(
                                            Intent.createChooser(
                                                callback_intent,
                                                "Share"
                                            )
                                        )
                                    }
                                }
                            } else if (type.equals("audio", ignoreCase = true)) {
                                if (item.has("file_url")) {
                                    val uri = Uri.parse(item.getString("file_url"))
                                    callback_intent!!.putExtra(Intent.EXTRA_STREAM, uri)
                                    callback_intent!!.type = "audio/*"
                                    callback_intent!!.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    counter++
                                    if (counter == l) {
                                        JasonHelper.next(
                                            "success",
                                            action,
                                            JSONObject(),
                                            event,
                                            context
                                        )
                                        context.startActivity(
                                            Intent.createChooser(
                                                callback_intent,
                                                "Share"
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
            }
        }.start()
    }
}