// A mildly-modified version of the MaterialBadgeTextView library from:
// https://github.com/matrixxun/MaterialBadgeTextView

package site.app4web.app4web.Lib;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Build;
//import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import site.app4web.app4web.Helper.JasonHelper;

import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.view.ViewCompat;

/**
 * Created by matrixxun on 2016/8/30.
 */
public class MaterialBadgeTextView extends AppCompatTextView {

    private static final int DEFAULT_FILL_TYPE = 0;

    private int backgroundColor;

    private static final float SHADOW_RADIUS = 3.5f;
    private static final int FILL_SHADOW_COLOR = 0x55000000;
    private static final int KEY_SHADOW_COLOR = 0x55000000;

    private static final float X_OFFSET = 0f;
    private static final float Y_OFFSET = 1.75f;


    private int mShadowRadius;
    private int shadowYOffset;
    private int shadowXOffset;

    private int diffWH;

    private boolean isHighLightMode;

    public MaterialBadgeTextView(final Context context) {
        this(context, null);
    }

    public MaterialBadgeTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MaterialBadgeTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        setGravity(Gravity.CENTER);
        float density = getContext().getResources().getDisplayMetrics().density;
        mShadowRadius = (int) (density * SHADOW_RADIUS);
        shadowYOffset = (int) (density * Y_OFFSET);
        shadowXOffset = (int) (density * X_OFFSET);
        int basePadding = (mShadowRadius * 2);
        float textHeight = getTextSize();
        float textWidth = textHeight / 4;
        diffWH = (int) (Math.abs(textHeight - textWidth) / 2);
        int horizontalPadding = basePadding + diffWH;
        setPadding(horizontalPadding, basePadding, horizontalPadding, basePadding);
        backgroundColor = JasonHelper.parse_color("#ff0000");
        int borderColor = Color.TRANSPARENT;
        float borderWidth = 0;
        float borderAlpha = 1;
        int ctType = DEFAULT_FILL_TYPE;

    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter);
        /** 纯色小红点模式下若有文本需要从无变为有, 要归位view的大小.*/
        /** Если в режиме сплошной красной точки есть текст, вам нужно изменить размер представления. */
        String strText = text==null?"":text.toString().trim();
        if(isHighLightMode && !"".equals(strText)){
            ViewGroup.LayoutParams lp = getLayoutParams();
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            setLayoutParams(lp);
            isHighLightMode = false;
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        refreshBackgroundDrawable(w, h);
    }

    private void refreshBackgroundDrawable(int targetWidth, int targetHeight) {
        if (targetWidth <= 0 || targetHeight <= 0) {
            return;
        }
        CharSequence text = getText();
        if (text == null) {
            return;
        }
        if (text.length() == 1) {
            /**第一种背景是一个正圆形, 当文本为个位数字时 */
            /** Первый фон представляет собой идеальный круг, когда текст представляет собой однозначное число */
            int max = Math.max(targetWidth, targetHeight);
            ShapeDrawable circle;
            final int diameter = max - (2 * mShadowRadius);
            OvalShape oval = new OvalShadow(mShadowRadius, diameter);
            circle = new ShapeDrawable(oval);
            // AA ViewCompat.setLayerType(this, ViewCompat.LAYER_TYPE_SOFTWARE, circle.getPaint());
            ViewCompat.setLayerType(this, View.LAYER_TYPE_SOFTWARE, circle.getPaint());
            circle.getPaint().setShadowLayer(mShadowRadius, shadowXOffset, shadowYOffset, KEY_SHADOW_COLOR);
            circle.getPaint().setColor(backgroundColor);
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN) {
                setBackgroundDrawable(circle);
            } else {
                setBackground(circle);
            }
        } else if (text.length() > 1) {
            /**第二种背景是上下两边为直线的椭圆, 当文本长度大于1时 */
            /** Вторым фоном является эллипс с прямыми линиями сверху и снизу. Когда длина текста больше 1, */
            SemiCircleRectDrawable sr = new SemiCircleRectDrawable();
            //ViewCompat.setLayerType(this, ViewCompat.LAYER_TYPE_SOFTWARE, sr.getPaint());
            ViewCompat.setLayerType(this, View.LAYER_TYPE_SOFTWARE, sr.getPaint());
            sr.getPaint().setShadowLayer(mShadowRadius, shadowXOffset, shadowYOffset, KEY_SHADOW_COLOR);
            sr.getPaint().setColor(backgroundColor);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                setBackground(sr);
            } else {
                setBackgroundDrawable(sr);
            }
        } else {
            /** 第三种情况就是text="", 即文本长度为0, 因为无任何文本, 则对当前的TextView背景不做任何更新,
             * 但是有时候我们需要一个无字的纯色小圆形,用来表达强调.这种情况因为要重新设置View的大小, 所以不在这里表现, 请使用另外一个方法setHighLightMode()来完成.
             */
            /** Третий случай - это text = "", то есть длина текста равна 0.
             *   Поскольку текст отсутствует, обновления для текущего фона TextView не производятся.
             *   Но иногда нам нужен сплошной маленький кружок без слов, чтобы выразить акцент.
             *   Это потому, что размер представления сбрасывается, поэтому он здесь не представлен.
             *   Пожалуйста, используйте другой метод setHighLightMode () для завершения.
             */

        }

    }

    public void setBadgeCount(String count){
        setBadgeCount(count, false);
    }
    public void setBadgeCount(String count, boolean goneWhenZero) {
        int temp = -1;
        try {
            temp = Integer.parseInt(count);
        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
        if (temp != -1) {
            setBadgeCount(temp, goneWhenZero);
        }
    }

    public void setBadgeCount(int count){
        setBadgeCount(count, true);
    }
    public void setBadgeCount(int count, boolean goneWhenZero){
        if(count >0 && count <= 99){
            setText(String.valueOf(count));
            setVisibility(View.VISIBLE);
        }else if(count >99){
            setText("99+");
            setVisibility(View.VISIBLE);
        }else if(count <= 0){
            setText("0");
            if(goneWhenZero){
                setVisibility(View.GONE);
            }else{
                setVisibility(View.VISIBLE);
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
    public void setHighLightMode(){
        setHighLightMode(false);
    }

    public void clearHighLightMode(){
        isHighLightMode = false;
        setBadgeCount(0);
    }

    /**
     *
     * @param isDisplayInToolbarMenu
     */
    public void setHighLightMode(boolean isDisplayInToolbarMenu){
        isHighLightMode = true;
        ViewGroup.LayoutParams params = getLayoutParams();
        params.width = dp2px(getContext(), 8);
        params.height = params.width;
        if(isDisplayInToolbarMenu && params instanceof FrameLayout.LayoutParams){
            ((FrameLayout.LayoutParams)params).topMargin=dp2px(getContext(), 8);
            ((FrameLayout.LayoutParams)params).rightMargin=dp2px(getContext(), 8);
        }
        setLayoutParams(params);
        ShapeDrawable drawable = new ShapeDrawable(new OvalShape());
        // AA ViewCompat.setLayerType(this, ViewCompat.LAYER_TYPE_SOFTWARE, drawable.getPaint());
        ViewCompat.setLayerType(this, View.LAYER_TYPE_SOFTWARE, drawable.getPaint());
        drawable.getPaint().setColor(backgroundColor);
        drawable.getPaint().setAntiAlias(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            setBackground(drawable);
        } else {
            setBackgroundDrawable(drawable);
        }
        setText("");
        setVisibility(View.VISIBLE);
    }

    public void setBackgroundColor(int color){
        backgroundColor = color;
        refreshBackgroundDrawable(getWidth(), getHeight());
    }

    private class OvalShadow extends OvalShape {
        private final Paint mShadowPaint;
        private final int mCircleDiameter;

        public OvalShadow(int shadowRadius, int circleDiameter) {
            super();
            mShadowPaint = new Paint();
            mShadowRadius = shadowRadius;
            mCircleDiameter = circleDiameter;
            RadialGradient mRadialGradient = new RadialGradient(mCircleDiameter / 2, mCircleDiameter / 2,
                    mShadowRadius, new int[]{
                    FILL_SHADOW_COLOR, Color.TRANSPARENT
            }, null, Shader.TileMode.CLAMP);
            mShadowPaint.setShader(mRadialGradient);
        }

        @Override
        public void draw(Canvas canvas, Paint paint) {
            final int viewWidth = MaterialBadgeTextView.this.getWidth();
            final int viewHeight = MaterialBadgeTextView.this.getHeight();
            canvas.drawCircle(viewWidth / 2, viewHeight / 2, (mCircleDiameter / 2 + mShadowRadius), mShadowPaint);
            canvas.drawCircle(viewWidth / 2, viewHeight / 2, (mCircleDiameter / 2), paint);
        }
    }

    class SemiCircleRectDrawable extends Drawable {
        private final Paint mPaint;
        private RectF rectF;

        public Paint getPaint() {
            return mPaint;
        }

        public SemiCircleRectDrawable() {
            mPaint = new Paint();
            mPaint.setAntiAlias(true);
        }

        @Override
        public void setBounds(int left, int top, int right, int bottom) {
            super.setBounds(left, top, right, bottom);
            if (rectF == null) {
                rectF = new RectF(left + diffWH, top + mShadowRadius+4, right - diffWH, bottom - mShadowRadius-4);
            } else {
                rectF.set(left + diffWH, top + mShadowRadius+4, right - diffWH, bottom - mShadowRadius-4);
            }
        }

        @Override
        public void draw(Canvas canvas) {
            float R = (float)(rectF.bottom * 0.4);
            if (rectF.right < rectF.bottom) {
                R = (float)(rectF.right * 0.4);
            }
            canvas.drawRoundRect(rectF, R, R, mPaint);
        }

        @Override
        public void setAlpha(int alpha) {
            mPaint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
            mPaint.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSPARENT;
        }
    }

    public static int dp2px(Context context, float dpValue) {
        try {
            final float scale = context.getResources().getDisplayMetrics().density;
            return (int) (dpValue * scale + 0.5f);
        } catch (Exception e) {
            return (int) (dpValue + 0.5f);
        }
    }

}


