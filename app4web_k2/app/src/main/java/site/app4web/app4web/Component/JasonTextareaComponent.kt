package site.app4web.app4web.Component

import android.content.*
import android.graphics.Typeface
import android.text.Editable
import android.text.InputType
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import org.json.JSONObject
import site.app4web.app4web.Core.JasonViewActivity
import site.app4web.app4web.Helper.JasonHelper


object JasonTextareaComponent {
    fun build(view: View?, component: JSONObject, parent: JSONObject?, context: Context): View? {
        var view = view
        return if (view == null) {
            EditText(context)
        } else {
            try {
                view = JasonComponent.build(view, component, parent, context)
                val style = JasonHelper.style(component, context)
                val type = component.getString("type")
                if (style.has("color")) {
                    val color = JasonHelper.parse_color(style.getString("color"))
                    (view as TextView?)!!.setTextColor(color)
                }
                if (style.has("color:placeholder")) {
                    val color = JasonHelper.parse_color(style.getString("color:placeholder"))
                    (view as TextView?)!!.setHintTextColor(color)
                }
                if (style.has("font:android")) {
                    val f = style.getString("font:android")
                    if (f.equals("bold", ignoreCase = true)) {
                        (view as TextView?)!!.setTypeface(Typeface.DEFAULT_BOLD)
                    } else if (f.equals("sans", ignoreCase = true)) {
                        (view as TextView?)!!.setTypeface(Typeface.SANS_SERIF)
                    } else if (f.equals("serif", ignoreCase = true)) {
                        (view as TextView?)!!.setTypeface(Typeface.SERIF)
                    } else if (f.equals("monospace", ignoreCase = true)) {
                        (view as TextView?)!!.setTypeface(Typeface.MONOSPACE)
                    } else if (f.equals("default", ignoreCase = true)) {
                        (view as TextView?)!!.setTypeface(Typeface.DEFAULT)
                    } else {
                        try {
                            val font_type = Typeface.createFromAsset(
                                context.assets,
                                "fonts/" + style.getString("font:android") + ".ttf"
                            )
                            (view as TextView?)!!.setTypeface(font_type)
                        } catch (e: Exception) {
                            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
                        }
                    }
                } else if (style.has("font")) {
                    if (style.getString("font").toLowerCase().contains("bold")) {
                        if (style.getString("font").toLowerCase().contains("italic")) {
                            (view as TextView?)!!.setTypeface(
                                Typeface.DEFAULT_BOLD,
                                Typeface.ITALIC
                            )
                        } else {
                            (view as TextView?)!!.setTypeface(Typeface.DEFAULT_BOLD)
                        }
                    } else {
                        if (style.getString("font").toLowerCase().contains("italic")) {
                            (view as TextView?)!!.setTypeface(Typeface.DEFAULT, Typeface.ITALIC)
                        } else {
                            (view as TextView?)!!.setTypeface(Typeface.DEFAULT)
                        }
                    }
                }
                if (!style.has("height")) {
                    val layoutParams = view.layoutParams
                    layoutParams.height = 300
                    view.layoutParams = layoutParams
                }
                var g = 0
                if (style.has("align")) {
                    val align = style.getString("align")
                    if (align.equals("center", ignoreCase = true)) {
                        g = g or Gravity.CENTER_HORIZONTAL
                        (view as TextView?)!!.gravity = Gravity.CENTER_HORIZONTAL
                    } else if (align.equals("right", ignoreCase = true)) {
                        g = g or Gravity.RIGHT
                        (view as TextView?)!!.gravity = Gravity.RIGHT
                    } else {
                        g = g or Gravity.LEFT
                    }
                    g = if (align.equals("bottom", ignoreCase = true)) {
                        g or Gravity.BOTTOM
                    } else {
                        g or Gravity.TOP
                    }
                } else {
                    g = Gravity.TOP or Gravity.LEFT
                }
                (view as EditText?)!!.gravity = g
                if (style.has("size")) {
                    (view as EditText?)!!.textSize = style.getString("size").toFloat()
                }
                (view as EditText?)!!.ellipsize = TextUtils.TruncateAt.END


                // placeholder
                // заполнитель
                if (component.has("placeholder")) {
                    (view as EditText?)!!.hint = component.getString("placeholder")
                }

                // default value
                // значение по умолчанию
                if (component.has("value")) {
                    (view as EditText?)!!.setText(component.getString("value"))
                }

                // keyboard
                // клавиатура
                if (component.has("keyboard")) {
                    val keyboard = component.getString("keyboard")
                    if (keyboard.equals("text", ignoreCase = true)) {
                        (view as EditText?)!!.inputType = InputType.TYPE_CLASS_TEXT
                    } else if (keyboard.equals("number", ignoreCase = true)) {
                        (view as EditText?)!!.inputType = InputType.TYPE_CLASS_NUMBER
                    } else if (keyboard.equals("decimal", ignoreCase = true)) {
                        (view as EditText?)!!.inputType =
                            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                    } else if (keyboard.equals("phone", ignoreCase = true)) {
                        (view as EditText?)!!.inputType = InputType.TYPE_CLASS_PHONE
                    } else if (keyboard.equals("url", ignoreCase = true)) {
                        (view as EditText?)!!.inputType =
                            InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                    } else if (keyboard.equals("email", ignoreCase = true)) {
                        (view as EditText?)!!.inputType =
                            InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                    }
                }

                // Data binding
                // Привязка данных
                if (component.has("name")) {
                    (view as EditText?)!!.addTextChangedListener(object : TextWatcher {
                        override fun beforeTextChanged(
                            s: CharSequence?,
                            start: Int,
                            count: Int,
                            after: Int
                        ) {
                        }

                        override fun onTextChanged(
                            s: CharSequence?,
                            start: Int,
                            before: Int,
                            count: Int
                        ) {
                        }

                        override fun afterTextChanged(s: Editable) {
                            try {
                                (context as JasonViewActivity).model!!.`var`!!.put(
                                    component.getString(
                                        "name"
                                    ), s.toString()
                                )
                                if (component.has("on")) {
                                    val events = component.getJSONObject("on")
                                    if (events.has("change")) {
                                        val intent = Intent("call")
                                        intent.putExtra(
                                            "action",
                                            events.getJSONObject("change").toString()
                                        )
                                        LocalBroadcastManager.getInstance(context)
                                            .sendBroadcast(intent)
                                    }
                                }
                            } catch (e: Exception) {
                                Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
                            }
                        }
                    })
                }
                (view as EditText?)!!.setOnEditorActionListener { v, actionId, event ->
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        try {
                            if (component.has("name")) {
                                (context as JasonViewActivity).model!!.`var`!!.put(
                                    component.getString(
                                        "name"
                                    ), v.text.toString()
                                )
                            }
                        } catch (e: Exception) {
                            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
                        }
                    }
                    false
                }
                view.requestLayout()
                view
            } catch (e: Exception) {
                Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
                View(context)
            }
        }
    }
}