package site.app4web.app4web.Section

import android.content.Context
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.GlideDrawable
import com.bumptech.glide.request.animation.GlideAnimation
import com.bumptech.glide.request.target.SimpleTarget
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import site.app4web.app4web.Component.JasonComponentFactory
import site.app4web.app4web.Component.JasonImageComponent
import site.app4web.app4web.Core.JasonViewActivity
import site.app4web.app4web.Helper.JasonHelper
import java.util.*

/********************************************************
 *
 * Here's the hierarchy:
 * Вот иерархия:
 *
 * - ViewHolder
 * - ContentView
 * - Layout
 * - Component
 *
 */
class ItemAdapter(
    root_context: Context,
    context: Context,
    items: ArrayList<JSONObject?>?
) : RecyclerView.Adapter<ItemAdapter.ViewHolder?>() {
    private var items: ArrayList<JSONObject?>? = items
    var context: Context
    var root_context: Context
    var cloned_items: ArrayList<JSONObject?>
    var signature_to_type: MutableMap<String, Int> =
        HashMap()
    var type_to_signature: MutableMap<Int, String> =
        HashMap()
    var factory = ViewHolderFactory()
    var isHorizontalScroll = false
    var backgroundImageView: ImageView? = null

    /********************************************************
     *
     * Root level RecyclerView/ViewHolder logic
     * Логика RecyclerView / ViewHolder корневого уровня
     *
     */
    inner class ViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView), View.OnClickListener {
        var subviews: ArrayList<View?>?
        var type: String
        val view: View
            get() = itemView

        override fun onClick(view: View) {
            val position = adapterPosition
            val item = view.tag as JSONObject
            try {
                if (item.has("action")) {
                    val action = item.getJSONObject("action")
                    (root_context as JasonViewActivity).call(
                        action.toString(),
                        JSONObject().toString(),
                        "{}",
                        view.context
                    )
                } else if (item.has("href")) {
                    val href = item.getJSONObject("href")
                    val action = JSONObject().put("type", "\$href").put("options", href)
                    (root_context as JasonViewActivity).call(
                        action.toString(),
                        JSONObject().toString(),
                        "{}",
                        view.context
                    )
                }
            } catch (e: Exception) {
            }
        }

        init {
            subviews = ArrayList()
            itemView.setOnClickListener(this)
            type = "item"
        }
    }

    fun updateItems(items: ArrayList<JSONObject?>?) {
        this.items = items
        cloned_items = ArrayList()
        cloned_items.addAll(items!!)
    }
    fun getItems(): ArrayList<JSONObject?>? {
        return items
    }
    fun filter(text: String) {
        var text = text
        items!!.clear()
        if (text.isEmpty()) {
            items!!.addAll(cloned_items)
        } else {
            text = text.toLowerCase()
            for (item in cloned_items) {
                if (item.toString().toLowerCase().contains(text)) {
                    items!!.add(item)
                }
            }
        }
        notifyDataSetChanged()
    }

    // For determining the view type.
    // 1. Generate a signature using the JSON markup and assign it to signature_to_type.
    // 2. If the signature already exists, return the type.
    // Для определения типа представления.
    // 1. Генерируем подпись с использованием разметки JSON и присваиваем ее сигнатуре signature_to_type.
    // 2. Если подпись уже существует, вернуть type.
    override fun getItemViewType(position: Int): Int {
        val item = items!![position]

        // if the key starts with "horizontal_section",
        // we deal with it in a special manner.
        // Assuming that all items for a horizontal section will have the same prototype,
        // we can generate the signature from just one of its items.
        // если ключ начинается с "Horizontal_section",
        // мы имеем дело с этим особым образом.
        // Предполагая, что все элементы для горизонтального сечения будут иметь один и тот же прототип,
        // мы можем сгенерировать подпись только из одного из ее элементов.
        val stringified_item: String
        stringified_item = if (item!!.has("horizontal_section")) {
            try {
                val horizontal_section_items = item.getJSONArray("horizontal_section")
                // assuming that the section would contain at least one item,
                // we will take the first item from the section and generate the signature
                val first_item = horizontal_section_items.getJSONObject(0)
                "[$first_item]"
            } catch (e: Exception) {
                item.toString()
            }
        } else {
            item.toString()
        }


        // Simplistic way of transforming an item JSON into a generic string, by replacing out all non-structural values
        // - replace out text and url
        // Упрощенный способ преобразования элемента JSON в общую строку путем замены всех неструктурных значений
        // - заменить текст и URL
        var regex = "\"(url|text)\"[ ]*:[ ]*\"([^\"]+)\""
        var signature =
            stringified_item.replace(regex.toRegex(), "\"jason\":\"jason\"")
        // - replace out 'title' and 'description'
        // - заменим «заголовок» и «описание»
        regex = "\"(title|description)\"[ ]*:[ ]*\"([^\"]+)\""
        signature = signature.replace(regex.toRegex(), "\"jason\":\"jason\"")
        return if (signature_to_type.containsKey(signature)) {
            // if the signature exists, get the type using the signature
            signature_to_type[signature]!!
        } else {
            // If it's a new signature, set the mapping between jason and type, both ways
            // Если это новая подпись, установите соответствие между jason и type, оба способа

            // Increment the index (new type) first.
            // Сначала увеличиваем индекс (новый тип).
            val index = signature_to_type.size

            // 1. jason => type: assign that index as the type for the signature
            // 1. jason => type: назначить этот индекс в качестве типа для подписи
            signature_to_type[signature] = index

            // 2. type => jason: assign the stringified item so it can be used later
            // 2. type => jason: назначить строковый элемент, чтобы его можно было использовать позже
            //  Need to use the original instance instead of the stubbed out "signature" since some components requre url or text attributes to instantiate (create)
            // Нужно использовать оригинальный экземпляр вместо заглушенной «подписи», так как некоторые компоненты требуют создания атрибутов url или text для создания (создания)
            type_to_signature[index] = stringified_item
            //type_to_signature.put(index, signature);

            // Return the new index;
            // Возвращаем новый индекс;
            index
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val signatureString = type_to_signature[viewType]
        val viewHolder: ViewHolder?
        if (signatureString!!.startsWith("[")) {
            // Horizontal Section => Build a ViewHolder with a horizontally scrolling RecyclerView
            // Горизонтальное сечение => Создание ViewHolder с горизонтальной прокруткой RecyclerView

            // 1. Create RecyclerView
            // 1. Создать RecyclerView
            val horizontalListView = RecyclerView(parent.context)
            horizontalListView.layoutManager = LinearLayoutManager(
                horizontalListView.context,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            horizontalListView.isNestedScrollingEnabled = false

            // 2. Create Adapter
            // 2. Создать адаптер
            val horizontal_adapter = ItemAdapter(
                context,
                horizontalListView.context,
                ArrayList()
            )
            horizontal_adapter.isHorizontalScroll = true

            // 3. Connect RecyclerView with Adapter
            // 3. Соединяем RecyclerView с адаптером
            horizontalListView.adapter = horizontal_adapter

            // 4. Instantiate a new ViewHolder with the RecyclerView
            // 4. Создание нового ViewHolder с помощью RecyclerView
            viewHolder = ViewHolder(horizontalListView)
        } else {
            // Vertcial Section => Regular ViewHolder
            // Вертикальное сечение => Regular ViewHolder
            val json: JSONObject
            json = try {
                JSONObject(signatureString)
            } catch (e: JSONException) {
                JSONObject()
            }
            viewHolder = factory.build(null, json)
        }
        return viewHolder!!
    }

    override fun onBindViewHolder(
        viewHolder: ViewHolder,
        position: Int
    ) {
        val json = items!![position]
        if (json!!.has("horizontal_section")) {
            // Horizontal Section
            // Горизонтальное сечение
            // In this case, the viewHolder is a Recyclerview.
            // В этом случае viewHolder является Recyclerview.

            // We fetch the recyclerview from the viewholder (the viewholder's itemView is the recyclerview)
            // Извлекаем обзор рециркулятора из видоискателя (элемент видоискателя - вид рециркулятора)
            val horizontalListAdapter =
                (viewHolder!!.itemView as RecyclerView).adapter as ItemAdapter?

            // Transform JasonArray into ArrayList
            // Преобразование массива Json в ArrayList
            try {
                horizontalListAdapter!!.items =
                    JasonHelper.toArrayList(json.getJSONArray("horizontal_section")) as ArrayList<JSONObject?>?
            } catch (e: Exception) {
            }

            // Update viewholder
            // Обновить видоискатель
            horizontalListAdapter!!.notifyDataSetChanged()
            viewHolder.itemView.invalidate()
        } else {
            // Vertical section
            // Вертикальный разрез
            // Build ViewHolder via ViewHolderFactory
            // Создаем ViewHolder через ViewHolderFactory
            factory.build(viewHolder, json)
        }
    }

    override fun getItemCount(): Int {
        return items!!.size
    }

    /********************************************************
     *
     * ViewHolderFactory => Creates ViewHolders
     * ViewHolderFactory => Создает ViewHolders
     *
     */
    inner class ViewHolderFactory {
        // "subviews" =>
        //
        //      store the DOM tree under viewHolder, so that it can be accessed easily inside onBindViewHolder, for example:
        //      viewHolder.subviews = [Image1, TextView1, Image2, TextView2, TextView3, TextView4];
        //      for(int i = 0 ; i < viewHolder.subviews.size() ; i++){
        //          View el = viewHolder.subviews.get(i);
        //          if(el instancof Button){
        //              ..
        //          } ..
        //      }
        // "subviews" =>
        //
        //      сохранить дерево DOM под viewHolder, чтобы к нему можно было легко получить доступ, например, внутри onBindViewHolder, например:
        //      viewHolder.subviews = [Image1, TextView1, Image2, TextView2, TextView3, TextView4];
        //      for (int i = 0; i <viewHolder.subviews.size (); i ++) {
        //          Просмотр el = viewHolder.subviews.get (i);
        //          if (el instancof Button) {
        //              ..
        //          } ..
        //      }
        private var subviews: ArrayList<View?>? = null
        private var exists: Boolean? = null
        private var index = 0
        fun build(
            prototype: ViewHolder?,
            json: JSONObject?
        ): ViewHolder? {
            val layout: LinearLayout


            // Fill
            exists = prototype != null
            return if (exists!!) {
                // Fill
                // заполнить

                // Get the subviews
                // Получить подпункты
                subviews = prototype!!.subviews
                index = 0

                // Build content view with the existing prototype layout
                // Создание представления контента с существующим макетом прототипа
                layout = prototype.view as LinearLayout
                buildContentView(layout, json)
                layout.tag = json

                // return the existing prototype layout
                // вернуть существующий макет прототипа
                prototype
            } else {
                // Create
                // Создайте

                // Initialize subviews
                // Инициализация подпредставлений
                subviews = ArrayList()

                // Build content view with a new layout
                // Создание представления содержимого с новым макетом
                layout = buildContentView(LinearLayout(context), json)

                // Create a new viewholder with the new layout
                // Создаем новый видоискатель с новым макетом
                val viewHolder =
                    ViewHolder(layout)

                // Assign subviews
                // Назначаем подпредставления
                viewHolder.subviews = subviews
                viewHolder
            }
        }

        // ContentView is the top level view of a cell.
        // It's always a layout.
        // If the JSON supplies a component, ContentView creates a layout wrapper around it
        // ContentView - это вид верхнего уровня ячейки.
        // Это всегда макет.
        // Если JSON предоставляет компонент, ContentView создает оболочку макета вокруг него
        private fun buildContentView(layout: LinearLayout, json: JSONObject?): LinearLayout {
            var layout = layout
            try {
                if (json!!.has("type")) {
                    val type = json.getString("type")
                    if (type.equals("vertical", ignoreCase = true) || type.equals(
                            "horizontal",
                            ignoreCase = true
                        )
                    ) {
                        layout = buildLayout(layout, json, null, 0)
                        layout.isClickable = true
                    } else {
                        // 1. Create components array
                        // 1. Создать массив компонентов
                        val components = JSONArray()


                        // 2. Create a vertical layout and set its components
                        // 2. Создаем вертикальный макет и устанавливаем его компоненты
                        val wrapper = JSONObject()
                        wrapper.put("type", "vertical")

                        // When wrapping, we set the padding on the wrapper to 0, since it will be taken care of on the component
                        // При обёртывании мы устанавливаем отступ на обёртку равным 0, так как об этом позаботится компонент
                        val style = JSONObject()
                        style.put("padding", if (type.equals("html", ignoreCase = true)) 1 else 0)
                        wrapper.put("style", style)

                        // Instead, we set the component's padding to 10
                        // Вместо этого мы устанавливаем отступ компонента 10
                        val componentStyle: JSONObject
                        if (json.has("style")) {
                            componentStyle = json.getJSONObject("style")
                            if (!componentStyle.has("padding")) {
                                componentStyle.put("padding", "10")
                            }
                        } else {
                            componentStyle = JSONObject()
                            componentStyle.put("padding", "10")
                        }
                        json.put("style", componentStyle)

                        // Setup components array
                        // Настройка массива компонентов
                        components.put(json)
                        wrapper.put("components", components)

                        // Setup href and actions
                        // Настройка href и действий
                        if (json.has("href")) {
                            wrapper.put("href", json.getJSONObject("href"))
                        }
                        if (json.has("action")) {
                            wrapper.put("action", json.getJSONObject("action"))
                        }

                        // 3. Start running the layout logic
                        // 3. Запускаем логику макета
                        buildLayout(layout, wrapper, null, 0)

                        // In case we're at the root level
                        // and the child has a width, we need to set the wrapper's width to wrap its child. (for horizontal scrolling sections)
                        // Если мы находимся на корневом уровне
                        // и у дочернего элемента есть ширина, нам нужно установить ширину оболочки, чтобы обернуть его дочерний элемент. (для горизонтальных прокручиваемых секций)
                        val componentView = layout.getChildAt(0)
                        val componentLayoutParams =
                            componentView.layoutParams
                        if (componentLayoutParams.width > 0) {
                            val layoutParams = layout.layoutParams
                            layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
                        }
                    }
                } else {
                    layout = LinearLayout(context)
                }
            } catch (e: JSONException) {
                layout = LinearLayout(context)
            }
            return layout
        }

        internal inner class BackgroundImage(var layout: LinearLayout) :
            SimpleTarget<GlideDrawable?>() {
            override fun onResourceReady(
                resource: GlideDrawable?,
                glideAnimation: GlideAnimation<in GlideDrawable?>?
            ) {
                layout.background = resource
            }

        }

        fun buildLayout(
            layout: LinearLayout,
            item: JSONObject?,
            parent: JSONObject?,
            level: Int
        ): LinearLayout {
            var level = level
            return if (exists!!) {
                try {
                    val components = item!!.getJSONArray("components")
                    for (i in 0 until components.length()) {
                        val component = components.getJSONObject(i)
                        if (component.getString("type").equals(
                                "vertical",
                                ignoreCase = true
                            ) || component.getString("type").equals("horizontal", ignoreCase = true)
                        ) {
                            val childLayout = layout.getChildAt(i) as LinearLayout
                            buildLayout(childLayout, component, item, ++level)
                            if (i > 0) {
                                add_spacing(childLayout, item, item.getString("type"))
                            }
                        } else {
                            val child_component =
                                buildComponent(component, item)
                            if (i > 0) {
                                add_spacing(child_component, item, item.getString("type"))
                            }
                        }
                    }
                } catch (e: JSONException) {
                }
                LinearLayout(context)
            } else {
                try {
                    // Layout styling
                    val type = item!!.getString("type")
                    val style = JasonHelper.style(item, root_context)
                    layout.setBackgroundColor(JasonHelper.parse_color("rgba(0,0,0,0)"))
                    var components: JSONArray
                    components = if (type.equals("vertical", ignoreCase = true) || type.equals(
                            "horizontal",
                            ignoreCase = true
                        )
                    ) {
                        item.getJSONArray("components")
                    } else {
                        JSONArray()
                    }
                    val layoutParams: LinearLayout.LayoutParams?
                    if (type.equals("vertical", ignoreCase = true)) {
                        // vertical layout
                        // вертикальное расположение
                        layout.orientation = LinearLayout.VERTICAL
                        components = item.getJSONArray("components")
                    } else if (type.equals("horizontal", ignoreCase = true)) {
                        // horizontal layout
                        // горизонтальное расположение
                        layout.orientation = LinearLayout.HORIZONTAL
                        components = item.getJSONArray("components")
                    }

                    // set width and height
                    // установить ширину и высоту
                    layoutParams =
                        JasonLayout.autolayout(isHorizontalScroll, parent, item, root_context)
                    layout.layoutParams = layoutParams

                    // Padding
                    // набивка
                    // If root level, set the default padding to 10
                    // Если корневой уровень, установить отступ по умолчанию на 10
                    val default_padding: String
                    default_padding = if (level == 0) {
                        "10"
                    } else {
                        "0"
                    }
                    var padding_left =
                        JasonHelper.pixels(root_context, default_padding, type).toInt()
                    var padding_right =
                        JasonHelper.pixels(root_context, default_padding, type).toInt()
                    var padding_top =
                        JasonHelper.pixels(root_context, default_padding, type).toInt()
                    var padding_bottom =
                        JasonHelper.pixels(root_context, default_padding, type).toInt()
                    if (style!!.has("padding")) {
                        padding_left = JasonHelper.pixels(
                            root_context,
                            style.getString("padding"),
                            type
                        ).toInt()
                        padding_right = padding_left
                        padding_top = padding_left
                        padding_bottom = padding_left
                    }
                    if (style.has("padding_left")) {
                        padding_left = JasonHelper.pixels(
                            root_context,
                            style.getString("padding_left"),
                            type
                        ).toInt()
                    }
                    if (style.has("padding_right")) {
                        padding_right = JasonHelper.pixels(
                            context,
                            style.getString("padding_right"),
                            type
                        ).toInt()
                    }
                    if (style.has("padding_top")) {
                        padding_top = JasonHelper.pixels(
                            root_context,
                            style.getString("padding_top"),
                            type
                        ).toInt()
                    }
                    if (style.has("padding_bottom")) {
                        padding_bottom = JasonHelper.pixels(
                            root_context,
                            style.getString("padding_bottom"),
                            type
                        ).toInt()
                    }
                    layout.setPadding(padding_left, padding_top, padding_right, padding_bottom)

                    // background
                    // фон
                    if (style.has("background")) {
                        if (level == 0) {
                            // top level. allow image background
                            val background = style.getString("background")
                            if (background.matches("(file|http[s]?):\\/\\/.*".toRegex())) {
                                val c = JSONObject()
                                c.put("url", background)
                                var cacheStrategy = DiskCacheStrategy.RESULT
                                // gif doesn't work with RESULT cache strategy
                                // gif не работает со стратегией кэширования RESULT
                                // TODO: Check with Glide V4
                                if (background.matches(".*\\.gif".toRegex())) {
                                    cacheStrategy = DiskCacheStrategy.SOURCE
                                }
                                Glide.with(root_context)
                                    .load(
                                        JasonImageComponent.resolve_url(
                                            c,
                                            root_context
                                        )
                                    )
                                    .diskCacheStrategy(cacheStrategy)
                                    .into(
                                        BackgroundImage(
                                            layout
                                        )
                                    )
                            } else {
                                // plain background
                                // простой фон
                                layout.setBackgroundColor(JasonHelper.parse_color(style.getString("background")))
                            }
                        } else {
                            layout.setBackgroundColor(JasonHelper.parse_color(style.getString("background")))
                        }
                    }


                    // spacing
                    // интервал
                    for (i in 0 until components.length()) {
                        val component = components.getJSONObject(i)
                        val component_type = component.getString("type")
                        if (component_type.equals(
                                "vertical",
                                ignoreCase = true
                            ) || component_type.equals("horizontal", ignoreCase = true)
                        ) {
                            // the child is also a layout
                            // ребенок тоже макет
                            val child_layout =
                                buildLayout(LinearLayout(context), component, item, ++level)
                            layout.addView(child_layout)
                            if (i > 0) {
                                add_spacing(child_layout, item, type)
                            }
                            // From item1, start adding margin-top (item0 shouldn't have margin-top)
                            // Начиная с item1, начинаем добавлять margin-top (item0 не должен иметь margin-top)
                        } else {
                            val child_component =
                                buildComponent(component, item)
                            // the child is a leaf node
                            // ребенок - это листовой узел
                            layout.addView(child_component)
                            if (i > 0) {
                                add_spacing(child_component, item, type)
                            }
                        }
                    }

                    // align
                    // выровнять
                    if (style.has("align")) {
                        if (style.getString("align").equals("center", ignoreCase = true)) {
                            layout.gravity = Gravity.CENTER
                        } else if (style.getString("align").equals("right", ignoreCase = true)) {
                            layout.gravity = Gravity.RIGHT
                        } else {
                            layout.gravity = Gravity.LEFT
                        }
                    }
                    layout.requestLayout()
                } catch (e: JSONException) {
                }
                layout
            }
        }

        fun buildComponent(component: JSONObject, parent: JSONObject?): View? {
            val view: View?
            val style = JasonHelper.style(component, root_context)
            return if (exists!!) {
                view = subviews!![index++]
                JasonComponentFactory.Companion.build(view, component, parent, root_context)
                view
            } else {
                view = JasonComponentFactory.Companion.build(null, component, parent, root_context)
                view!!.id = subviews!!.size
                subviews!!.add(view)
                view
            }
        }

        private fun add_spacing(
            view: View?,
            item: JSONObject?,
            type: String
        ) {
            try {
                var spacing = "0"
                val style = JasonHelper.style(item, root_context)
                spacing = if (style!!.has("spacing")) {
                    style.getString("spacing")
                } else {
                    "0"
                }
                if (type.equals("vertical", ignoreCase = true)) {
                    val m = JasonHelper.pixels(context, spacing, item!!.getString("type")).toInt()
                    val layoutParams: LinearLayout.LayoutParams
                    layoutParams = if (view!!.layoutParams == null) {
                        LinearLayout.LayoutParams(0, 0)
                    } else {
                        view.layoutParams as LinearLayout.LayoutParams
                    }
                    layoutParams.topMargin = m
                    layoutParams.bottomMargin = 0
                    view.layoutParams = layoutParams
                } else if (type.equals("horizontal", ignoreCase = true)) {
                    val m =
                        JasonHelper.pixels(root_context, spacing, item!!.getString("type")).toInt()
                    val layoutParams: LinearLayout.LayoutParams
                    layoutParams = if (view!!.layoutParams == null) {
                        LinearLayout.LayoutParams(0, 0)
                    } else {
                        view.layoutParams as LinearLayout.LayoutParams
                    }
                    layoutParams.leftMargin = m
                    layoutParams.rightMargin = 0
                    view.layoutParams = layoutParams
                }
                view!!.requestLayout()
            } catch (e: Exception) {
                Log.d(
                    "Warning",
                    e.stackTrace[0].methodName + " : " + e.toString()
                )
            }
        }
    }

    companion object {
        const val DATA = 0
    }

    init {
        cloned_items = ArrayList()
        cloned_items.addAll(items!!)
        this.context = context
        this.root_context = root_context
    }
}