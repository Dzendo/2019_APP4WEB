package site.app4web.app4web.Component

import android.content.*
import android.util.Log
import android.view.*
import android.widget.LinearLayout
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SimpleOnItemTouchListener
import com.google.android.gms.maps.*
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import org.json.JSONException
import org.json.JSONObject
import site.app4web.app4web.Action.JasonGeoAction
import site.app4web.app4web.Core.JasonViewActivity
import site.app4web.app4web.Helper.JasonHelper
import timber.log.Timber


object JasonMapComponent : JasonComponent() {
    var EQUATOR_LENGTH = 40075004
    private const val DEFAULT_PIN_ACTION_PROP = "default_action"
    private const val MAP_PIN_TITLE_PROP = "title"
    private const val MAP_PIN_COORD_PROP = "coord"
    private const val MAP_PIN_DESCRIPTION_PROP = "description"
    const val JS_FALSE = "false"
    fun build(view: View?, component: JSONObject, parent: JSONObject?, context: Context): View {
        if (view == null) {
            try {
                MapsInitializer.initialize(context)
                val options = GoogleMapOptions()
                var latlng: LatLng? = LatLng(0, 0)
                if (component.has("region")) {
                    latlng = getCoordinates(component.getJSONObject("region"))
                }
                options.camera(CameraPosition(latlng, 16, 0, 0))
                val style = component.getJSONObject("style")
                if (style.has("type")) {
                    when (style["type"] as String) {
                        "satellite" -> options.mapType(GoogleMap.MAP_TYPE_SATELLITE)
                        "hybrid" -> options.mapType(GoogleMap.MAP_TYPE_HYBRID)
                        "terrain" -> options.mapType(GoogleMap.MAP_TYPE_TERRAIN)
                        else -> options.mapType(GoogleMap.MAP_TYPE_NORMAL)
                    }
                } else {
                    options.mapType(GoogleMap.MAP_TYPE_NORMAL)
                }
                val mapview = MapView(context, options)
                mapview.onCreate(null) // Trigger onCreate
                (context as JasonViewActivity).addListViewOnItemTouchListener(touchListener)
                // Add pins when the map is ready
                // Добавляем булавки, когда карта готова
                mapview.getMapAsync(MapReadyHandler(component, mapview, context))
                return mapview
            } catch (e: Exception) {
                Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
            }
        } else {
            try {
                Companion.build(view, component, parent, context)
                JasonHelper.style(component, context)
                addListener(view, context)
                view.requestLayout()
                (view as MapView).onResume() // Trigger onResume
                return view
            } catch (e: Exception) {
                Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
            }
        }
        return View(context)
    }

    private fun getCoordinates(position: JSONObject): LatLng {
        // Calculate latitude and longitude
        // Рассчитать широту и долготу
        var latitude = 0.0
        var longitude = 0.0
        try {
            val r = position.getString("coord").split(",").toTypedArray()
            if (r.size == 2) {
                latitude = r[0].toDouble()
                longitude = r[1].toDouble()
            }
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
        return LatLng(latitude, longitude)
    }

    // Intercept touch events on the RecyclerView to make sure they don't interfere with map moves
    // Перехватывать сенсорные события в RecyclerView, чтобы убедиться, что они не мешают перемещению карты
    var touchListener: SimpleOnItemTouchListener = object : SimpleOnItemTouchListener() {
        // Intercept touch events on the recycler view, and if they are over a mapview, make sure
        // to let the mapview handle them
        override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
            if (rv.findChildViewUnder(e.x, e.y) is LinearLayout) {
                val layout = rv.findChildViewUnder(e.x, e.y) as LinearLayout?
                if (layout != null) {
                    for (i in 0 until layout.childCount) {
                        val child = layout.getChildAt(i)
                        // Weed out non-map views ASAP
                        // Отсеиваем взгляды не на карте как можно скорее
                        if (child.javaClass == MapView::class.java) {
                            val left = layout.left + child.left
                            val right = layout.left + child.right
                            val top = layout.top + child.top
                            val bottom = layout.top + child.bottom
                            if (e.x > left && e.x < right && e.y > top && e.y < bottom) {
                                when (e.actionMasked) {
                                    MotionEvent.ACTION_DOWN -> rv.requestDisallowInterceptTouchEvent(
                                        true
                                    )
                                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> rv.requestDisallowInterceptTouchEvent(
                                        false
                                    )
                                }
                            }
                        }
                    }
                }
            }
            return false
        }
    }

    internal class MapReadyHandler(
        private val component: JSONObject,
        private val view: MapView,
        private val context: Context
    ) : OnMapReadyCallback {
        private fun getZoomForMetersWide(meters: Double, width: Double, lat: Double): Double {
            // Converts metes wide to a zoom level for a google map, got this nice formula from
            // Конвертирует метры в масштаб для карты Google, получил эту красивую формулу из
            // http://stackoverflow.com/a/21034310/1034194
            val latAdjust = Math.cos(Math.PI * lat / 180.0)
            val arg = EQUATOR_LENGTH * width * latAdjust / (meters * 256.0)
            return Math.log(arg) / Math.log(2.0)
        }

        override fun onMapReady(map: GoogleMap) {
            try {
                // Add pins to the map
                // Добавляем булавки на карту
                if (component.has("pins")) {
                    val pins = component.getJSONArray("pins")
                    for (i in 0 until pins.length()) {
                        val pin = pins.getJSONObject(i)
                        val options = MarkerOptions()
                        options.position(getCoordinates(pin))
                        if (pin.has("title")) {
                            options.title(pin.getString("title"))
                        }
                        if (pin.has("description")) {
                            options.snippet(pin.getString("description"))
                        }
                        val marker = map.addMarker(options)
                        if (pin.has("style")) {
                            val style = pin.getJSONObject("style")
                            if (style.has("selected") && style.getBoolean("selected")) {
                                marker.showInfoWindow()
                            }
                        }
                        if (pin.has(ACTION_PROP)) {
                            marker.tag = pin
                        }
                    }
                }

                // Move the camera to the zoom level that shows at least the desired region
                // Переместить камеру на уровень масштабирования, который показывает хотя бы желаемую область
                if (component.has("region")) {
                    val region = component.getJSONObject("region")
                    if (region.has("width") && region.has("height")) {
                        val width = region.getDouble("width")
                        val height = region.getDouble("height")
                        val activity = context as JasonViewActivity
                        val metrics = activity.resources.displayMetrics
                        val viewWidth = (view.layoutParams.width / metrics.density).toDouble()
                        val viewHeight = (view.layoutParams.height / metrics.density).toDouble()
                        var meters = width
                        if (height > width && viewHeight > viewWidth) {
                            // Widen the zoom in order to see the requested height
                            // Расширяем масштаб, чтобы увидеть запрошенную высоту
                            meters = height
                        }
                        val lat = map.cameraPosition.target.latitude
                        val zoom = getZoomForMetersWide(meters, viewWidth, lat).toFloat()
                        map.moveCamera(CameraUpdateFactory.zoomTo(zoom))
                    }
                }

                // Attach listener for pin 'clicks'
                // Прикрепить слушатель к пин-кликам
                map.setOnInfoWindowClickListener(OnInfoWindowClickListener { marker ->
                    try {
                        if (marker.tag == null) {
                            return@OnInfoWindowClickListener
                        }
                        val pinJSONObject = marker.tag as JSONObject?
                        if (pinJSONObject!!.has(ACTION_PROP)) {
                            val pinData =
                                JSONObject().put(MAP_PIN_TITLE_PROP, marker.title)
                            val pos = marker.position
                            pinData.put(
                                MAP_PIN_COORD_PROP, String.format(
                                    JasonGeoAction.Companion.COORDS_STRING_FORMAT,
                                    pos.latitude,
                                    pos.longitude
                                )
                            )
                            pinData.put(MAP_PIN_DESCRIPTION_PROP, marker.snippet)
                            val intent = Intent(INTENT_ACTION_CALL)
                            intent.putExtra(
                                ACTION_PROP,
                                pinJSONObject[ACTION_PROP].toString()
                            )
                            intent.putExtra(DATA_PROP, pinData.toString())
                            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
                        } else if (pinJSONObject.has(HREF_PROP)) {
                            val intent = Intent(INTENT_ACTION_CALL)
                            val href = JSONObject()
                            href.put(TYPE_PROP, "\$href")
                            href.put(
                                OPTIONS_PROP,
                                pinJSONObject[HREF_PROP].toString()
                            )
                            intent.putExtra(
                                ACTION_PROP,
                                pinJSONObject[ACTION_PROP].toString()
                            )
                            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
                        }
                    } catch (e: JSONException) {
                        Timber.e(e)
                    }
                })
            } catch (e: Exception) {
                Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
            }
        }
    }
}