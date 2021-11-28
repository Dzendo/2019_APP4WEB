package site.app4web.app4web.Component
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.text.Editable
import android.text.InputType
import android.text.TextUtils
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import site.app4web.app4web.Helper.JasonHelper
import site.app4web.app4web.Core.JasonViewActivity
import org.json.JSONObject


object JasonTextfieldComponent {
    fun build(view: View?, component: JSONObject, parent: JSONObject?, context: Context): View? {
        var view = view
        return if (view == null) {
            EditText(context)
        } else {
            try {
                view = JasonComponent.Companion.build(view, component, parent, context)
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
                if (style.has("align")) {
                    val align = style.getString("align")
                    if (align.equals("center", ignoreCase = true)) {
                        (view as EditText?)!!.gravity = Gravity.CENTER_HORIZONTAL
                    } else if (align.equals("right", ignoreCase = true)) {
                        (view as EditText?)!!.gravity = Gravity.RIGHT
                    } else {
                        (view as EditText?)!!.gravity = Gravity.LEFT
                    }
                }
                if (style.has("size")) {
                    (view as EditText?)!!.textSize = style.getString("size").toFloat()
                }
                (view as EditText?)!!.setSingleLine()
                (view as EditText?)!!.setLines(1)
                (view as EditText?)!!.ellipsize = TextUtils.TruncateAt.END


                // padding
                // заполнение
                var padding_left = JasonHelper.pixels(context, "10", "horizontal").toInt()
                var padding_right = JasonHelper.pixels(context, "10", "horizontal").toInt()
                var padding_top = JasonHelper.pixels(context, "10", "horizontal").toInt()
                var padding_bottom = JasonHelper.pixels(context, "10", "horizontal").toInt()
                if (style.has("padding")) {
                    padding_left =
                        JasonHelper.pixels(context, style.getString("padding"), "horizontal")
                            .toInt()
                    padding_right = padding_left
                    padding_top = padding_left
                    padding_bottom = padding_left
                }

                // overwrite if more specific values exist
                // перезаписать, если существуют более конкретные значения
                if (style.has("padding_left")) {
                    padding_left =
                        JasonHelper.pixels(context, style.getString("padding_left"), "horizontal")
                            .toInt()
                }
                if (style.has("padding_right")) {
                    padding_right =
                        JasonHelper.pixels(context, style.getString("padding_right"), "horizontal")
                            .toInt()
                }
                if (style.has("padding_top")) {
                    padding_top =
                        JasonHelper.pixels(context, style.getString("padding_top"), "vertical")
                            .toInt()
                }
                if (style.has("padding_bottom")) {
                    padding_bottom =
                        JasonHelper.pixels(context, style.getString("padding_bottom"), "vertical")
                            .toInt()
                }
                view.setPadding(padding_left, padding_top, padding_right, padding_bottom)


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

                // The order is important => Must set the secure mode first and then set the font because Android sets the typeface to monospace by default when password mode
                // Порядок важен => Сначала необходимо установить безопасный режим, а затем установить шрифт, потому что Android устанавливает шрифт в моноширинный режим по умолчанию в режиме пароля
                if (style.has("secure")) {
                    (view as EditText?)!!.inputType =
                        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                    (view as EditText?)!!.transformationMethod =
                        PasswordTransformationMethod.getInstance()
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
                view.requestLayout()
                view
            } catch (e: Exception) {
                Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
                View(context)
            }
        }
    }
}