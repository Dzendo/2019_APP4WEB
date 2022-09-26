package site.app4web.app4web.Lib;

import android.content.Context;
import android.graphics.Typeface;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import site.app4web.app4web.Component.JasonImageComponent;
import site.app4web.app4web.Helper.JasonHelper;

import org.json.JSONObject;

/**
 * Created by realitix on 27/07/17.
 */

public class JasonToolbar extends Toolbar {
    private TextView titleView;
    private ImageView logoView;
    private int alignment = -1;
    private int leftOffset;
    private int topOffset;
    private int imageWidth;
    private int imageHeight;

    public JasonToolbar(Context context) {
        super(context);
    }

    public JasonToolbar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public JasonToolbar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setTitle(CharSequence title) {
        // remove image view before inserting title view
        // удаляем вид изображения перед вставкой вида заголовка
        if (logoView != null && logoView.getParent() == this) {
            removeView(logoView);
        }

        // remove title if empty
        // удаляем заголовок, если пуст
        if (title.length() <= 0) {
            if (titleView != null && titleView.getParent() == this) {
                removeView(titleView);
            }
            return;
        }

        // create title only on the first call
        // создаем заголовок только при первом вызове
        if (titleView == null) {
            titleView = new TextView(getContext());
        }

        // insert into toolbar
        // вставить на панель инструментов
        if (titleView.getParent() != this) {
            addView(titleView, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        }

        // manage positioning
        // управлять позиционированием
        LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        params.gravity = (alignment == -1) ? Gravity.LEFT : alignment;
        params.leftMargin = leftOffset;
        params.topMargin = topOffset;
        titleView.setLayoutParams(params);

        // set text
        // установить текст
        titleView.setText(title);
    }

    public void setImage(JSONObject url) {
        // remove title view before inserting image view
        // удалить заголовок перед вставкой изображения
        if (titleView != null && titleView.getParent() == this) {
            removeView(titleView);
        }

        // create the image view only on the first call
        // создаем вид изображения только при первом вызове
        if (logoView == null) {
            logoView = new ImageView(getContext());
        }

        // insert into toolbar
        // вставить на панель инструментов
        if (logoView.getParent() != this) {
            addView(logoView);
        }

        // manage positioning
        // управлять позиционированием
        LayoutParams params = new LayoutParams(imageWidth, imageHeight);
        params.gravity = (alignment == -1) ? Gravity.CENTER : alignment;
        params.leftMargin = leftOffset;
        params.topMargin = topOffset;
        logoView.setLayoutParams(params);

        // load image with glide
        // загрузить изображение с помощью glide
        Glide.with(getContext())
                .load(JasonImageComponent.resolve_url(url, getContext()))
                .into(logoView);
    }

    public void setTitleFont(JSONObject style) {
        JasonHelper.setTextViewFont(titleView, style, getContext());
    }

    @Override
    public void setTitleTextColor(int color) {
        titleView.setTextColor(color);
    }

    public void setTitleSize(float size) {
        titleView.setTextSize(size);
    }

    public void setTitleTypeface(Typeface font) {
        titleView.setTypeface(font);
    }

    public void setAlignment(int alignment) {
        this.alignment = alignment;
    }

    public void setLeftOffset(int offset) {
        leftOffset = offset;
    }

    public void setTopOffset(int offset) {
        topOffset = offset;
    }

    public void setImageHeight(int height) {
        imageHeight = height;
    }

    public void setImageWidth(int width) {
        imageWidth = width;
    }
}
