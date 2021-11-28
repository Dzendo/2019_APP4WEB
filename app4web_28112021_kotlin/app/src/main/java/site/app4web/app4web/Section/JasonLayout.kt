package site.app4web.app4web.Section

import android.content.Context
import android.widget.LinearLayout
import site.app4web.app4web.Helper.JasonHelper
import org.json.JSONObject


object JasonLayout {
    fun autolayout(
        isHorizontalScroll: Boolean,
        parent: JSONObject?,
        item: JSONObject,
        root_context: Context?
    ): LinearLayout.LayoutParams {
        var width = 0f
        var height = 0f
        var weight = 0
        return try {
            val style = JasonHelper.style(item, root_context)
            val item_type = item.getString("type")
            if (parent == null) {
                // parent == null means: it's at the root level. Which can be:
                //  1. a layer item
                //  2. the root level of a section item
                // parent == null означает: это на корневом уровне. Который может быть:
                // 1. элемент слоя
                // 2. корневой уровень элемента раздела
                if (style.has("width")) {
                    try {
                        width = (JasonHelper.pixels(
                            root_context,
                            style.getString("width"),
                            "horizontal"
                        ) as Int).toFloat()
                        if (style.has("ratio")) {
                            val ratio = JasonHelper.ratio(style.getString("ratio"))
                            height = width / ratio
                        }
                    } catch (e: Exception) {
                    }
                } else {
                    width = if (isHorizontalScroll) {
                        LinearLayout.LayoutParams.WRAP_CONTENT.toFloat()
                    } else {
                        LinearLayout.LayoutParams.MATCH_PARENT.toFloat()
                    }
                }
                if (style.has("height")) {
                    try {
                        height = (JasonHelper.pixels(
                            root_context,
                            style.getString("height"),
                            "vertical"
                        ) as Int).toFloat()
                        if (style.has("ratio")) {
                            val ratio = JasonHelper.ratio(style.getString("ratio"))
                            width = height * ratio
                        }
                    } catch (e: Exception) {
                    }
                } else {
                    height = LinearLayout.LayoutParams.WRAP_CONTENT.toFloat()
                }
            } else if (parent.getString("type").equals("vertical", ignoreCase = true)) {
                if (style.has("height")) {
                    try {
                        height = (JasonHelper.pixels(
                            root_context,
                            style.getString("height"),
                            "vertical"
                        ) as Int).toFloat()
                        if (style.has("ratio")) {
                            val ratio = JasonHelper.ratio(style.getString("ratio"))
                            width = height * ratio
                        }
                    } catch (e: Exception) {
                    }
                } else {
                    if (item_type.equals(
                            "vertical",
                            ignoreCase = true
                        ) || item_type.equals(
                            "horizontal",
                            ignoreCase = true
                        ) || item_type.equals("space", ignoreCase = true)
                    ) {
                        // layouts should have flexible height inside a vertical layout
                        height = 0f
                        weight = 1
                    } else {
                        // components should stay as their intrinsic size
                        // компоненты должны оставаться как их собственный размер
                        height = LinearLayout.LayoutParams.WRAP_CONTENT.toFloat()
                    }
                }
                if (style.has("width")) {
                    try {
                        width = (JasonHelper.pixels(
                            root_context,
                            style.getString("width"),
                            "horizontal"
                        ) as Int).toFloat()
                        if (style.has("ratio")) {
                            val ratio = JasonHelper.ratio(style.getString("ratio"))
                            height = width / ratio
                        }
                    } catch (e: Exception) {
                    }
                } else {
                    // in case of vertical layout, all its children, regardless of whether they are layout or components,
                    // should have the width match parent
                    // (Except for images, which will be handled inside JasonImageComponent)
                    // в случае вертикального расположения все его дочерние элементы, независимо от того, являются ли они макетом или компонентами,
                    // должен иметь ширину, соответствующую родителю
                    // (За исключением изображений, которые будут обрабатываться внутри JasonImageComponent)
                    width = LinearLayout.LayoutParams.MATCH_PARENT.toFloat()
                }
            } else if (parent.getString("type").equals("horizontal", ignoreCase = true)) {
                if (style.has("width")) {
                    try {
                        width = (JasonHelper.pixels(
                            root_context,
                            style.getString("width"),
                            "horizontal"
                        ) as Int).toFloat()
                        if (style.has("ratio")) {
                            val ratio = JasonHelper.ratio(style.getString("ratio"))
                            height = width / ratio
                        }
                    } catch (e: Exception) {
                    }
                } else {
                    // in a horizontal layout, the child components shouldn't fight with width.
                    // All must be flexible width unless otherwise specified.
                    // в горизонтальном макете дочерние компоненты не должны бороться с шириной.
                    // Все должны иметь гибкую ширину, если не указано иное.
                    width = 0f
                    weight = 1
                }
                if (style.has("height")) {
                    try {
                        height = (JasonHelper.pixels(
                            root_context,
                            style.getString("height"),
                            "vertical"
                        ) as Int).toFloat()
                        if (style.has("ratio")) {
                            val ratio = JasonHelper.ratio(style.getString("ratio"))
                            width = height * ratio
                        }
                    } catch (e: Exception) {
                    }
                } else {
                    height = LinearLayout.LayoutParams.WRAP_CONTENT.toFloat()
                }
            }
            val layoutParams = LinearLayout.LayoutParams(width.toInt(), height.toInt())
            if (weight > 0) {
                layoutParams.weight = weight.toFloat()
            }
            layoutParams
        } catch (e: Exception) {
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
    }
}