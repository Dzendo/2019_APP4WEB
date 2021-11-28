package site.app4web.app4web.Helper

import android.content.Context
import site.app4web.app4web.R


object JasonSettings {
    fun isAsync(className: String?, context: Context): Boolean {
        val asyncActions = context.resources.getStringArray(R.array.asyncActions)
        for (s in asyncActions) {
            if (s.equals(className, ignoreCase = true)) return true
        }
        return false
    }
}