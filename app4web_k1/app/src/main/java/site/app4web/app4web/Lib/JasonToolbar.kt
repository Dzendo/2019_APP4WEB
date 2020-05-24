package site.app4web.app4web.Lib

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.Glide
import org.json.JSONObject
import site.app4web.app4web.Component.JasonImageComponent
import site.app4web.app4web.Helper.JasonHelper

/**
 * Created by realitix on 27/07/17.
 */
class JasonToolbar : Toolbar {
    private var titleView: TextView? = null
    private var logoView: ImageView? = null
    private var alignment = -1
    private var leftOffset = 0
    private var topOffset = 0
    private var imageWidth = 0
    private var imageHeight = 0

    constructor(context: Context?) : super(context) {}
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {}
    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {
    }

    override fun setTitle(title: CharSequence) {
        // remove image view before inserting title view
        // удаляем вид изображения перед вставкой вида заголовка
        if (logoView != null && logoView!!.parent === this) {
            removeView(logoView)
        }

        // remove title if empty
        // удаляем заголовок, если пуст
        if (title.length <= 0) {
            if (titleView != null && titleView!!.parent === this) {
                removeView(titleView)
            }
            return
        }

        // create title only on the first call
        // создаем заголовок только при первом вызове
        if (titleView == null) {
            titleView = TextView(context)
        }

        // insert into toolbar
        // вставить на панель инструментов
        if (titleView!!.parent !== this) {
            addView(
                titleView,
                LayoutParams(
                    LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT
                )
            )
        }

        // manage positioning
        // управлять позиционированием
        val params =
            LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            )
        params.gravity = if (alignment == -1) Gravity.LEFT else alignment
        params.leftMargin = leftOffset
        params.topMargin = topOffset
        titleView!!.layoutParams = params

        // set text
        // установить текст
        titleView!!.text = title
    }

    fun setImage(url: JSONObject?) {
        // remove title view before inserting image view
        // удалить заголовок перед вставкой изображения
        if (titleView != null && titleView!!.parent === this) {
            removeView(titleView)
        }

        // create the image view only on the first call
        // создаем вид изображения только при первом вызове
        if (logoView == null) {
            logoView = ImageView(context)
        }

        // insert into toolbar
        // вставить на панель инструментов
        if (logoView!!.parent !== this) {
            addView(logoView)
        }

        // manage positioning
        // управлять позиционированием
        val params =
            LayoutParams(imageWidth, imageHeight)
        params.gravity = if (alignment == -1) Gravity.CENTER else alignment
        params.leftMargin = leftOffset
        params.topMargin = topOffset
        logoView!!.layoutParams = params

        // load image with glide
        // загрузить изображение с помощью glide
        Glide.with(context)
            .load(JasonImageComponent.resolve_url(url!!, context))
            .into(logoView)
    }

    fun setTitleFont(style: JSONObject) {
        JasonHelper.setTextViewFont(titleView, style, context)
    }

    override fun setTitleTextColor(color: Int) {
        titleView!!.setTextColor(color)
    }

    fun setTitleSize(size: Float) {
        titleView!!.textSize = size
    }

    fun setTitleTypeface(font: Typeface?) {
        titleView!!.typeface = font
    }

    fun setAlignment(alignment: Int) {
        this.alignment = alignment
    }

    fun setLeftOffset(offset: Int) {
        leftOffset = offset
    }

    fun setTopOffset(offset: Int) {
        topOffset = offset
    }

    fun setImageHeight(height: Int) {
        imageHeight = height
    }

    fun setImageWidth(width: Int) {
        imageWidth = width
    }
}