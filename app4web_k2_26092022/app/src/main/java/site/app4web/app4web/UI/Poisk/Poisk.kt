package site.app4web.app4web.UI.Poisk

import android.os.AsyncTask
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.customsearch.Customsearch
import com.google.api.services.customsearch.CustomsearchRequestInitializer
import site.app4web.app4web.Launcher.Launcher.Companion.setting


/* Обращается к Google Search для поиска передаваемой строки
    Google отдает 10 результатов в Search
    Перебрасываю их с ArraList и отдаю на высветку
 */

class  Poisk : AsyncTask<String, Void, MutableList<MutableMap<String,Any>>?>() {
    val myCX = setting.myCX                             // "003159383193150926956:c6m9ah2oy8e" //Your search engine
    val myKey = setting.myKey                           //"AIzaSyCL_iY2ALQxC4w6Qfld253adeI_GGl_bmw"
    val myApp = setting.myApp                           //"APP4WEB"

    override fun doInBackground(vararg searchQuery: String):MutableList<MutableMap<String,Any>>? {

        val cs = Customsearch.Builder(NetHttpTransport(), JacksonFactory.getDefaultInstance(), null)
            .setApplicationName(myApp)
            .setGoogleClientRequestInitializer(CustomsearchRequestInitializer(myKey))
            .build()

            val list = cs.cse().list(searchQuery[0]).setCx(myCX)   //Set search parameter
            val result = list.execute()    //Execute search
            if (result == null) return null
            if (result.items == null) return null

        val resultitems : MutableList<MutableMap<String,Any>> = mutableListOf()
        result.items.forEach { ri -> resultitems.add(ri.toMutableMap()) }

        return resultitems
    }
}


