package site.app4web.app4web.Action;

import android.content.Context;

import site.app4web.app4web.Core.JasonViewActivity;
import site.app4web.app4web.Helper.JasonHelper;

import org.json.JSONObject;


public class JasonVisionAction {

    /**
     * {
     *     "type": "$vision.scan"
     * }
     *
     * Scans code specified in
     * Сканирует код, указанный в
     *   https://developer.apple.com/documentation/avfoundation/avmetadataobjecttype?language=objc for iOS
     *   https://developers.google.com/vision/android/barcodes-overview for Android
     */

    public void scan(final JSONObject action, final JSONObject data, final JSONObject event, final Context context) {
        ((JasonViewActivity) context).cameraManager.is_open = true;
        JasonHelper.next("success", action, data, event, context);
    }
}
