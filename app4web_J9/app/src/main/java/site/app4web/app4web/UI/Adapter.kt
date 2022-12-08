package site.app4web.app4web.UI

import android.content.Context

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast

import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso

import site.app4web.app4web.R
import site.app4web.app4web.UI.Adapter.*


// для вывода сложных объектов в RecyclerView необходимо определить свой адаптер:
class Adapter(private val context: Context, private val sites: Array<Site?>, private val orient: Boolean) :
    RecyclerView.Adapter<ViewHolder>() {
    private val inflater: LayoutInflater = LayoutInflater.from(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val item: Int =  if (orient)     R.layout.item_gorizont
                                    else R.layout.item_vertical
        val view = inflater.inflate(item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val site = sites[position]

        // Инфо: http://developer.alexanderklimov.ru/android/library/picasso.php
       //Picasso.with(context).load(site!!.icon).into(holder.icon_View)  // 2.5.2
        Picasso.get().load(site!!.icon).into(holder.icon_View)          // 2.8
        // в поле holder.icon_View загнать изображение из сети по адресу site.getIcon()

        holder.link_View.text = site.link
        //  holder.name_View.text = site.name
        holder.title_View.text = site.title
    }

    override fun getItemCount(): Int = sites.size

    inner class ViewHolder internal constructor(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
        internal val icon_View: ImageView
        internal val link_View: TextView
       // internal val name_View: TextView
        internal val title_View: TextView

        init {
            icon_View = view.findViewById<View>(R.id.icon_View) as ImageView
            link_View = view.findViewById<View>(R.id.link_View) as TextView
       //     name_View = view.findViewById<View>(R.id.name_View) as TextView
            title_View = view.findViewById<View>(R.id.title_View) as TextView

            view.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            Toast.makeText(context,"Выбрали позицию = " + this.adapterPosition , Toast.LENGTH_SHORT).show()
            DoHttp(context,sites[this.adapterPosition]!!.link)
        }

    }
}

/* Адаптер, который используется в RecyclerView, должен наследоваться от абстрактного класса RecyclerView.Adapter.
 Этот класс определяет три метода:
onCreateViewHolder: возвращает объект ViewHolder, который будет хранить данные по одному объекту Site_Json.
onBindViewHolder: выполняет привязку объекта ViewHolder к объекту Site_Json по определенной позиции.
getItemCount: возвращает количество объектов в списке

Для хранения данных в классе адаптера определен статический класс ViewHolder,
 который использует определенные в list_item.xml элементы управления.
*/