// A mildly-modified version of the MaterialBadgeTextView library from:
// https://github.com/matrixxun/MaterialBadgeTextView
package site.app4web.app4web.Lib

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.ViewCompat
import site.app4web.app4web.Helper.JasonHelper

//import android.support.v7.widget.AppCompatTextView;
/**
 * Created by matrixxun on 2016/8/30.
 */
class MaterialBadgeTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {
    private var backgroundColor = 0
    private var borderColor = 0
    private var borderWidth = 0f
    private var borderAlpha = 0f
    private var ctType = 0
    private var density = 0f
    private var mShadowRadius = 0
    private var shadowYOffset = 0
    private var shadowXOffset = 0
    private var basePadding = 0
    private var diffWH = 0
    private var isHighLightMode = false
    private fun init(context: Context, attrs: AttributeSet?) {
        gravity = Gravity.CENTER
        density = getContext().resources.displayMetrics.density
        mShadowRadius = (density * SHADOW_RADIUS).toInt()
        shadowYOffset = (density * Y_OFFSET).toInt()
        shadowXOffset = (density * X_OFFSET).toInt()
        basePadding = mShadowRadius * 2
        val textHeight = textSize
        val textWidth = textHeight / 4
        diffWH = (Math.abs(textHeight - textWidth) / 2).toInt()
        val horizontalPadding = basePadding + diffWH
        setPadding(horizontalPadding, basePadding, horizontalPadding, basePadding)
        backgroundColor = JasonHelper.parse_color("#ff0000")
        borderColor = Color.TRANSPARENT
        borderWidth = 0f
        borderAlpha = 1f
        ctType = DEFAULT_FILL_TYPE
    }

    override fun onTextChanged(
        text: CharSequence,
        start: Int,
        lengthBefore: Int,
        lengthAfter: Int
    ) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter)
        /** 纯色小红点模式下若有文本需要从无变为有, 要归位view的大小. */
        /** Если в режиме сплошной красной точки есть текст, вам нужно изменить размер представления.  */
        val strText = text.toString().trim { it <= ' ' }
        if (isHighLightMode && "" != strText) {
            val lp = layoutParams
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT
            lp.width = ViewGroup.LayoutParams.WRAP_CONTENT
            layoutParams = lp
            isHighLightMode = false
        }
    }

    override fun onSizeChanged(
        w: Int,
        h: Int,
        oldw: Int,
        oldh: Int
    ) {
        super.onSizeChanged(w, h, oldw, oldh)
        refreshBackgroundDrawable(w, h)
    }

    private fun refreshBackgroundDrawable(targetWidth: Int, targetHeight: Int) {
        if (targetWidth <= 0 || targetHeight <= 0) {
            return
        }
        val text = text ?: return
        if (text.length == 1) {
            /**第一种背景是一个正圆形, 当文本为个位数字时  */
            /** Первый фон представляет собой идеальный круг, когда текст представляет собой однозначное число  */
            val max = Math.max(targetWidth, targetHeight)
            val circle: ShapeDrawable
            val diameter = max - 2 * mShadowRadius
            val oval: OvalShape =
                OvalShadow(mShadowRadius, diameter)
            circle = ShapeDrawable(oval)
            // AA ViewCompat.setLayerType(this, ViewCompat.LAYER_TYPE_SOFTWARE, circle.getPaint());
            ViewCompat.setLayerType(this, View.LAYER_TYPE_SOFTWARE, circle.paint)
            circle.paint.setShadowLayer(
                mShadowRadius.toFloat(),
                shadowXOffset.toFloat(),
                shadowYOffset.toFloat(),
                KEY_SHADOW_COLOR
            )
            circle.paint.color = backgroundColor
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN) {
                setBackgroundDrawable(circle)
            } else {
                background = circle
            }
        } else if (text.length > 1) {
            /**第二种背景是上下两边为直线的椭圆, 当文本长度大于1时  */
            /** Вторым фоном является эллипс с прямыми линиями сверху и снизу. Когда длина текста больше 1,  */
            val sr = SemiCircleRectDrawable()
            //ViewCompat.setLayerType(this, ViewCompat.LAYER_TYPE_SOFTWARE, sr.getPaint());
            ViewCompat.setLayerType(this, View.LAYER_TYPE_SOFTWARE, sr.paint)
            sr.paint.setShadowLayer(
                mShadowRadius.toFloat(),
                shadowXOffset.toFloat(),
                shadowYOffset.toFloat(),
                KEY_SHADOW_COLOR
            )
            sr.paint.color = backgroundColor
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                background = sr
            } else {
                setBackgroundDrawable(sr)
            }
        } else {
            /** 第三种情况就是text="", 即文本长度为0, 因为无任何文本, 则对当前的TextView背景不做任何更新,
             * 但是有时候我们需要一个无字的纯色小圆形,用来表达强调.这种情况因为要重新设置View的大小, 所以不在这里表现, 请使用另外一个方法setHighLightMode()来完成.
             */
            /** Третий случай - это text = "", то есть длина текста равна 0.
             * Поскольку текст отсутствует, обновления для текущего фона TextView не производятся.
             * Но иногда нам нужен сплошной маленький кружок без слов, чтобы выразить акцент.
             * Это потому, что размер представления сбрасывается, поэтому он здесь не представлен.
             * Пожалуйста, используйте другой метод setHighLightMode () для завершения.
             */
        }
    }

    fun setBadgeCount(count: String) {
        setBadgeCount(count, false)
    }

    fun setBadgeCount(count: String, goneWhenZero: Boolean) {
        var temp = -1
        try {
            temp = count.toInt()
        } catch (e: Exception) {
            Log.d(
                "Warning",
                e.stackTrace[0].methodName + " : " + e.toString()
            )
        }
        if (temp != -1) {
            setBadgeCount(temp, goneWhenZero)
        }
    }

    fun setBadgeCount(count: Int) {
        setBadgeCount(count, true)
    }

    fun setBadgeCount(count: Int, goneWhenZero: Boolean) {
        if (count > 0 && count <= 99) {
            text = count.toString()
            visibility = View.VISIBLE
        } else if (count > 99) {
            text = "99+"
            visibility = View.VISIBLE
        } else if (count <= 0) {
            text = "0"
            visibility = if (goneWhenZero) {
                View.GONE
            } else {
                View.VISIBLE
            }
        }
    }

    /**
     * 明确的展现一个无任何文本的红色圆点,
     * 主要是通过设置文本setText("")触发onTextChanged(), 再连锁触发onSizeChanged()最后更新了背景.
     * Четко показать красную точку без текста,
     * Главным образом, вызывая onTextChanged (), устанавливая текст setText (""),
     * затем связывая цепочку onSizeChanged (), чтобы, наконец, обновить фон.
     */
    fun setHighLightMode() {
        setHighLightMode(false)
    }

    fun clearHighLightMode() {
        isHighLightMode = false
        setBadgeCount(0)
    }

    /**
     *
     * @param isDisplayInToolbarMenu
     */
    fun setHighLightMode(isDisplayInToolbarMenu: Boolean) {
        isHighLightMode = true
        val params = layoutParams
        params.width = dp2px(context, 8f)
        params.height = params.width
        if (isDisplayInToolbarMenu && params is FrameLayout.LayoutParams) {
            params.topMargin =
                dp2px(context, 8f)
            params.rightMargin =
                dp2px(context, 8f)
        }
        layoutParams = params
        val drawable = ShapeDrawable(OvalShape())
        // AA ViewCompat.setLayerType(this, ViewCompat.LAYER_TYPE_SOFTWARE, drawable.getPaint());
        ViewCompat.setLayerType(this, View.LAYER_TYPE_SOFTWARE, drawable.paint)
        drawable.paint.color = backgroundColor
        drawable.paint.isAntiAlias = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            background = drawable
        } else {
            setBackgroundDrawable(drawable)
        }
        text = ""
        visibility = View.VISIBLE
    }

    override fun setBackgroundColor(color: Int) {
        backgroundColor = color
        refreshBackgroundDrawable(width, height)
    }

    private inner class OvalShadow(
        shadowRadius: Int,
        circleDiameter: Int
    ) : OvalShape() {
        private val mRadialGradient: RadialGradient
        private val mShadowPaint: Paint
        private val mCircleDiameter: Int
        override fun draw(canvas: Canvas, paint: Paint) {
            val viewWidth = this@MaterialBadgeTextView.width
            val viewHeight = this@MaterialBadgeTextView.height
            canvas.drawCircle(
                viewWidth / 2.toFloat(),
                viewHeight / 2.toFloat(),
                (mCircleDiameter / 2 + mShadowRadius).toFloat(),
                mShadowPaint
            )
            canvas.drawCircle(
                viewWidth / 2.toFloat(),
                viewHeight / 2.toFloat(),
                (mCircleDiameter / 2).toFloat(),
                paint
            )
        }

        init {
            mShadowPaint = Paint()
            mShadowRadius = shadowRadius
            mCircleDiameter = circleDiameter
            mRadialGradient = RadialGradient(
                (mCircleDiameter / 2).toFloat(), (mCircleDiameter / 2).toFloat(),
                mShadowRadius.toFloat(), intArrayOf(
                    FILL_SHADOW_COLOR, Color.TRANSPARENT
                ), null, Shader.TileMode.CLAMP
            )
            mShadowPaint.shader = mRadialGradient
        }
    }

    internal inner class SemiCircleRectDrawable : Drawable() {
        val paint: Paint
        private var rectF: RectF? = null

        override fun setBounds(
            left: Int,
            top: Int,
            right: Int,
            bottom: Int
        ) {
            super.setBounds(left, top, right, bottom)
            if (rectF == null) {
                rectF = RectF(
                    (left + diffWH).toFloat(),
                    (top + mShadowRadius + 4).toFloat(),
                    (right - diffWH).toFloat(),
                    (bottom - mShadowRadius - 4).toFloat()
                )
            } else {
                rectF!![left + diffWH.toFloat(), top + mShadowRadius + 4.toFloat(), right - diffWH.toFloat()] =
                    bottom - mShadowRadius - 4.toFloat()
            }
        }

        override fun draw(canvas: Canvas) {
            var R = (rectF!!.bottom * 0.4).toFloat()
            if (rectF!!.right < rectF!!.bottom) {
                R = (rectF!!.right * 0.4).toFloat()
            }
            canvas.drawRoundRect(rectF!!, R, R, paint)
        }

        override fun setAlpha(alpha: Int) {
            paint.alpha = alpha
        }

        override fun setColorFilter(colorFilter: ColorFilter?) {
            paint.colorFilter = colorFilter
        }

        override fun getOpacity(): Int {
            return PixelFormat.TRANSPARENT
        }

        init {
            paint = Paint()
            paint.isAntiAlias = true
        }
    }

    companion object {
        private const val DEFAULT_FILL_TYPE = 0
        private const val SHADOW_RADIUS = 3.5f
        private const val FILL_SHADOW_COLOR = 0x55000000
        private const val KEY_SHADOW_COLOR = 0x55000000
        private const val X_OFFSET = 0f
        private const val Y_OFFSET = 1.75f
        fun dp2px(context: Context, dpValue: Float): Int {
            return try {
                val scale = context.resources.displayMetrics.density
                (dpValue * scale + 0.5f).toInt()
            } catch (e: Exception) {
                (dpValue + 0.5f).toInt()
            }
        }
    }

    init {
        init(context, attrs)
    }
}