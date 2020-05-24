package site.app4web.app4web.Service.vision

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.os.Build
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.Detector.Detections
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import org.json.JSONObject
import site.app4web.app4web.Core.JasonViewActivity

//import android.view.SurfaceHolder;
/**
 * Created by realitix on 06/07/17.
 */
class JasonVisionService(context: AppCompatActivity) {
    private var temp_context: AppCompatActivity? = null
    private var temp_holder: SurfaceHolder? = null
    private var temp_side = 0
    private var detector: BarcodeDetector? = null
    private var cameraSource: CameraSource? = null
    private var view: SurfaceView? = null
    private var side = 0
    var is_open = false
    fun setSide(side: Int) {
        this.side = side
    }
    fun getView(): SurfaceView? {
        return view
    }
    private fun initView(context: AppCompatActivity) {
        if (view != null) {
            return
        }
        view = SurfaceView(context)
        val holder = view!!.holder

        /*
         * Barcode detection
         * Обнаружение штрих-кода
         *
         * When a code is recognized, this service:
         * Когда код распознается, эта служба:
         *
         * [1] triggers the event "$vision.onscan" with the following payload:
         * [1] запускает событие «$ vision.onscan» со следующей полезной нагрузкой:
         *
         * {
         *   "$jason": {
         *     "type": "org.iso.QRCode",
         *     "content": "hello world"
         *   }
         * }
         *
         * the "type" attribute is different for iOS and Android. In case of Android it returns a number code specified at:
         * атрибут «тип» отличается для iOS и Android. В случае Android он возвращает числовой код, указанный в:
         *  https://developers.google.com/android/reference/com/google/android/gms/vision/barcode/Barcode.html#constants
         *
         * [2] Then immediately stops scanning.
         * [3] To start scanning again, you need to call $vision.scan again
         * [2] Затем немедленно прекращает сканирование.
         * [3] Чтобы начать сканирование снова, вам нужно снова вызвать $ vision.scan
         *
         */detector = BarcodeDetector.Builder(context)
            .setBarcodeFormats(Barcode.QR_CODE)
            .build()
        detector!!.setProcessor(object : Detector.Processor<Barcode> {
            override fun release() {}
            override fun receiveDetections(detections: Detections<Barcode>) {
                if (is_open) {
                    val detected_items = detections.detectedItems
                    if (detected_items.size() != 0) {
                        for (i in 0 until detected_items.size()) {
                            val key = detected_items.keyAt(i)
                            val obj = detected_items[key]
                            is_open = false
                            try {
                                val payload = JSONObject()
                                /*
                                JSONArray corners = new JSONArray();
                                for (int j = 0; j < obj.cornerPoints.length; j++) {
                                    Point p = obj.cornerPoints[j];
                                    JSONObject point = new JSONObject();
                                    point.put("top", p.y);
                                    point.put("left", p.x);
                                    corners.put(point);
                                }
                                payload.put("corners", corners);
                                */payload.put("content", obj.rawValue)
                                payload.put("type", obj.format)
                                val response = JSONObject()
                                response.put("\$jason", payload)
                                (context as JasonViewActivity).simple_trigger(
                                    "\$vision.onscan",
                                    response,
                                    context
                                )
                            } catch (e: Exception) {
                                Log.d(
                                    "Warning",
                                    e.stackTrace[0].methodName + " : " + e.toString()
                                )
                            }
                            return
                        }
                    }
                }
            }
        })
        holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(surfaceHolder: SurfaceHolder) {
                startCamera(context, surfaceHolder, side)
            }

            override fun surfaceChanged(
                surfaceHolder: SurfaceHolder,
                i: Int,
                i1: Int,
                i2: Int
            ) {
            }

            override fun surfaceDestroyed(surfaceHolder: SurfaceHolder) {
                stopCamera()
            }
        })
    }

    private fun startCamera(
        context: AppCompatActivity,
        holder: SurfaceHolder,
        side: Int
    ) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CAMERA
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    temp_context = context
                    temp_holder = holder
                    temp_side = side
                    ActivityCompat.requestPermissions(
                        context,
                        arrayOf(Manifest.permission.CAMERA),
                        50
                    )
                } else {
                    openCamera(context, holder, side)
                }
            } else {
                openCamera(context, holder, side)
            }
        } catch (e: Exception) {
            Log.d(
                "Warning",
                e.stackTrace[0].methodName + " : " + e.toString()
            )
        }
    }

    fun startVision(context: AppCompatActivity) {
        openCamera(context, temp_holder, temp_side)
        temp_context = null
        temp_holder = null
        temp_side = -1
    }

    @SuppressLint("MissingPermission")
    fun openCamera(context: AppCompatActivity, holder: SurfaceHolder?, side: Int) {
        try {
            if (cameraSource != null) {
                cameraSource!!.stop()
            }
            cameraSource = CameraSource.Builder(context, detector)
                .setFacing(side)
                .setAutoFocusEnabled(true)
                .build()
            cameraSource?.start(holder!!)

            (context as JasonViewActivity).simple_trigger("\$vision.ready", JSONObject(), context)
        } catch (e: Exception) {
            Log.d(
                "Warning",
                e.stackTrace[0].methodName + " : " + e.toString()
            )
        }
    }

    fun stopCamera() {
        cameraSource!!.stop()
    }

    private fun getVerticalCameraDisplayOrientation(
        context: AppCompatActivity,
        cameraId: Int
    ): Int {
        val info = CameraInfo()
        Camera.getCameraInfo(cameraId, info)
        val rotation = context.windowManager.defaultDisplay.rotation
        var degrees = 0
        when (rotation) {
            Surface.ROTATION_0 -> degrees = 0
            Surface.ROTATION_90 -> degrees = 90
            Surface.ROTATION_180 -> degrees = 180
            Surface.ROTATION_270 -> degrees = 270
        }
        var result: Int
        if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360
            result = (360 - result) % 360 // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360
        }
        return result
    }

    companion object {
        var FRONT = CameraInfo.CAMERA_FACING_FRONT
        var BACK = CameraInfo.CAMERA_FACING_BACK
    }

    init {
        initView(context)
    }
}