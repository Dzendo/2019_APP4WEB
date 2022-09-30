package site.app4web.app4web.Action

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import org.json.JSONObject
import site.app4web.app4web.Helper.JasonHelper

class JasonGeoAction {
    operator fun get(
        action: JSONObject?,
        data: JSONObject?,
        event: JSONObject?,
        context: Context
    ) {
        try {
            val locationManager =
                context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val locationListener: LocationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    try {
                        locationManager.removeUpdates(this)
                        val ret = JSONObject()
                        val `val` = String.format(
                            COORDS_STRING_FORMAT,
                            location.latitude,
                            location.longitude
                        )
                        ret.put("coord", `val`)
                        ret.put("value", `val`)
                        JasonHelper.next("success", action, ret, event, context)
                    } catch (e: Exception) {
                        Log.d(
                            "Warning",
                            e.stackTrace[0].methodName + " : " + e.toString()
                        )
                    }
                }

                override fun onStatusChanged(
                    provider: String,
                    status: Int,
                    extras: Bundle
                ) {
                }

                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                0,
                0f,
                locationListener,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            JasonHelper.permission_exception("\$geo.get", context)
        } catch (e: Exception) {
            Log.d(
                "Warning",
                e.stackTrace[0].methodName + " : " + e.toString()
            )
        }
    }

    companion object {
        const val COORDS_STRING_FORMAT = "%f,%f"
    }
}