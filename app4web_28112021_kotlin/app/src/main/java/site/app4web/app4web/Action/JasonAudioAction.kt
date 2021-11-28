package site.app4web.app4web.Action

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Environment
import android.util.Base64
import android.util.Log
import site.app4web.app4web.Core.JasonViewActivity
import site.app4web.app4web.Helper.JasonHelper
import org.json.JSONObject
import java.io.InputStream
import cafe.adriel.androidaudiorecorder.AndroidAudioRecorder
import cafe.adriel.androidaudiorecorder.model.AudioChannel
import cafe.adriel.androidaudiorecorder.model.AudioSampleRate
import cafe.adriel.androidaudiorecorder.model.AudioSource
import timber.log.Timber


class JasonAudioAction {
    private var player: MediaPlayer? = null
    private var mFileUrl: String? = null
    fun play(action: JSONObject, data: JSONObject?, event: JSONObject?, context: Context) {
        try {
            if (action.has("options")) {
                val options = action.getJSONObject("options")
                if (options.has("url")) {
                    if (player == null) {
                        player = MediaPlayer()
                        player!!.setAudioStreamType(AudioManager.STREAM_MUSIC)
                        val url = options.getString("url")
                        player!!.reset()
                        player!!.setDataSource(url)
                        player!!.prepare()
                    }
                    val audioManager =
                        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    (context as JasonViewActivity).volumeControlStream = AudioManager.STREAM_MUSIC
                    if (player!!.isPlaying) {
                        player!!.pause()
                    } else {
                        player!!.start()
                    }
                }
            }
            JasonHelper.next("success", action, JSONObject(), event, context)
        } catch (e: SecurityException) {
            JasonHelper.permission_exception("\$audio.play", context)
        } catch (e: Exception) {
            Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
        }
    }

    fun pause(action: JSONObject?, data: JSONObject?, event: JSONObject?, context: Context?) {
        if (player != null) {
            player!!.pause()
        }
        JasonHelper.next("success", action, JSONObject(), event, context)
    }

    fun stop(action: JSONObject?, data: JSONObject?, event: JSONObject?, context: Context?) {
        if (player != null) {
            player!!.stop()
            player!!.release()
            player = null
        }
        JasonHelper.next("success", action, JSONObject(), event, context)
    }

    fun duration(action: JSONObject?, data: JSONObject?, event: JSONObject?, context: Context?) {
        if (player != null) {
            try {
                val duration = player!!.duration / 1000
                val ret = JSONObject()
                ret.put("value", duration.toString())
                JasonHelper.next("success", action, ret, event, context)
            } catch (e: Exception) {
                Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
                try {
                    val err = JSONObject()
                    err.put("message", "invalid position")
                    JasonHelper.next("error", action, err, event, context)
                } catch (e2: Exception) {
                    Log.d("Warning", e2.stackTrace[0].methodName + " : " + e2.toString())
                }
            }
        } else {
            try {
                val err = JSONObject()
                err.put("message", "player doesn't exist")
                JasonHelper.next("error", action, err, event, context)
            } catch (e: Exception) {
                Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
            }
        }
    }

    fun position(action: JSONObject?, data: JSONObject?, event: JSONObject?, context: Context?) {
        if (player != null) {
            try {
                val duration = player!!.duration
                val position = player!!.currentPosition
                val ratio = (position / duration).toFloat()
                val ret = JSONObject()
                ret.put("value", ratio.toString())
                JasonHelper.next("success", action, ret, event, context)
            } catch (e: Exception) {
                try {
                    val err = JSONObject()
                    err.put("message", "invalid position or duration")
                    JasonHelper.next("error", action, err, event, context)
                } catch (e2: Exception) {
                    Log.d("Warning", e2.stackTrace[0].methodName + " : " + e2.toString())
                }
            }
        } else {
            try {
                val err = JSONObject()
                err.put("message", "player doesn't exist")
                JasonHelper.next("error", action, err, event, context)
            } catch (e: Exception) {
                Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
            }
        }
    }

    fun seek(action: JSONObject, data: JSONObject?, event: JSONObject?, context: Context?) {
        if (player != null) {
            try {
                if (action.has("options")) {
                    val options = action.getJSONObject("options")
                    if (options.has("position")) {
                        val position = options.getString("position").toFloat()
                        val duration = player!!.duration
                        player!!.seekTo(position.toInt() * duration)
                    }
                }
            } catch (e: Exception) {
                Log.d("Warning", e.stackTrace[0].methodName + " : " + e.toString())
            }
        }
        JasonHelper.next("success", action, JSONObject(), event, context)
    }

    fun record(action: JSONObject, data: JSONObject?, event: JSONObject?, context: Context?) {
        try {
            var color = JasonHelper.parse_color("rgba(0,0,0,0.8)")
            if (action.has("options")) {
                val options = action.getJSONObject("options")
                if (options.has("color")) {
                    color = JasonHelper.parse_color(options.getString("color"))
                }
            }
            val filePath =
                Environment.getExternalStorageDirectory().toString() + "/recorded_audio.m4a"
            val requestCode = (System.currentTimeMillis() % 10000).toInt()
            AndroidAudioRecorder.with(context as JasonViewActivity?)
                .setFilePath(filePath)
                .setColor(color)
                .setRequestCode(requestCode)
                .setSource(AudioSource.MIC)
                .setChannel(AudioChannel.STEREO)
                .setSampleRate(AudioSampleRate.HZ_48000)
                .setAutoStart(true)
                .setKeepDisplayOn(true)
                .record()
            try {
                mFileUrl = "file://$filePath"
                val callback = JSONObject()
                callback.put("class", "JasonAudioAction")
                callback.put("method", "completeRecording")
                JasonHelper.dispatchIntent(
                    requestCode.toString(),
                    action,
                    data,
                    event,
                    context,
                    null,
                    callback
                )
            } catch (e: Exception) {
                Timber.e(e)
            }
        } catch (e: SecurityException) {
            JasonHelper.permission_exception("\$audio.record", context)
        } catch (e: Exception) {
            Timber.w(e)
            try {
                val err = JSONObject()
                err.put("message", e.toString())
                JasonHelper.next("error", action, err, event, context)
            } catch (e2: Exception) {
                Timber.e(e2)
            }
        }
    }

    fun completeRecording(intent: Intent?, options: JSONObject) {
        //Timber.d("recording completed, sending action: %s"); //error
        Timber.d("recording completed, sending action: ")
        try {
            val action = options.getJSONObject("action")
            val data = options.getJSONObject("data")
            val event = options.getJSONObject("event")
            val context = options["context"] as Context
            val stream = context.contentResolver.openInputStream(Uri.parse(mFileUrl))
            val byteArray = JasonHelper.readBytes(stream)
            val encoded = Base64.encodeToString(byteArray, Base64.NO_WRAP)
            val stringBuilder = StringBuilder()
            stringBuilder.append("data:audio/m4a;base64,")
            stringBuilder.append(encoded)
            val data_uri = stringBuilder.toString()
            val ret = JSONObject()
            ret.put("file_url", mFileUrl)
            ret.put("url", mFileUrl)
            ret.put("content_type", "audio/m4a")
            ret.put("data_uri", data_uri)
            JasonHelper.next("success", action, ret, event, context)
        } catch (e: Exception) {
            Timber.e(e)
        }
    }
}