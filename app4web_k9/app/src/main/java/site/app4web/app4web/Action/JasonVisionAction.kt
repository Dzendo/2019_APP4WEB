package site.app4web.app4web.Action

import android.content.Context
import org.json.JSONObject
import site.app4web.app4web.Core.JasonViewActivity
import site.app4web.app4web.Helper.JasonHelper

class JasonVisionAction {
    /**
     * {
     * "type": "$vision.scan"
     * }
     *
     * Scans code specified in
     * Сканирует код, указанный в
     * https://developer.apple.com/documentation/avfoundation/avmetadataobjecttype?language=objc for iOS
     * https://developers.google.com/vision/android/barcodes-overview for Android
     */
    fun scan(
        action: JSONObject?,
        data: JSONObject?,
        event: JSONObject?,
        context: Context
    ) {
        (context as JasonViewActivity).cameraManager?.is_open  = true
        JasonHelper.next("success", action, data, event, context)
    }
}