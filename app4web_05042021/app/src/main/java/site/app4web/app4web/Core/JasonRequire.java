package site.app4web.app4web.Core;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import site.app4web.app4web.Helper.JasonHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;

import okhttp3.Request;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Response;

public class JasonRequire implements Runnable{
    final String URL;
    final CountDownLatch latch;
    final Context context;
    final OkHttpClient client;

    JSONObject private_refs;

    public JasonRequire(String url, CountDownLatch latch, JSONObject refs, OkHttpClient client, Context context) {
        this.URL = url.replace("\\", "");
        this.latch = latch;
        this.private_refs = refs;
        this.context = context;
        this.client = client;
    }
    public void run() {
        if(this.URL.contains("file://")) {
            local();
        } else {
            remote();
        }
    }
    private void local(){
        try {
            Runnable r = new Runnable()
            {
                @Override
                public void run()
                {
                    Object json = JasonHelper.read_json(URL, context);
                    try {
                        private_refs.put(URL, json);
                    } catch (Exception e) {
                        Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
                    }
                    latch.countDown();
                }
            };
            Thread t = new Thread(r);
            t.start();
        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
            latch.countDown();
        }
    }
    private void remote(){
        Request request;
        Request.Builder builder = new Request.Builder();

        // Session Handling
        // Обработка сессии
        try {
            SharedPreferences pref = context.getSharedPreferences("session", 0);
            JSONObject session = null;
            URI uri_for_session = new URI(this.URL);
            String session_domain = uri_for_session.getHost();
            if(pref.contains(session_domain)){
                String str = pref.getString(session_domain, null);
                session = new JSONObject(str);
            }

            // session.header
            // заголовок сессии
            if(session != null && session.has("header")) {
                Iterator<?> keys = session.getJSONObject("header").keys();
                while (keys.hasNext()) {
                    String key = (String) keys.next();
                    String val = session.getJSONObject("header").getString(key);
                    builder.addHeader(key, val);
                }
            }

            // session.body
            // тело сеанса
            Uri.Builder b = Uri.parse(this.URL).buildUpon();
            // Attach Params from Session
            // Прикрепить параметры из сессии
            if(session != null && session.has("body")) {
                Iterator<?> keys = session.getJSONObject("body").keys();
                while (keys.hasNext()) {
                    String key = (String) keys.next();
                    String val = session.getJSONObject("body").getString(key);
                    b.appendQueryParameter(key, val);
                }
            }

            Uri uri = b.build();
            String url_with_session = uri.toString();
            request = builder
                    .url(url_with_session)
                    .build();


            // Actual call
            // Фактический звонок
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    latch.countDown();
                    e.printStackTrace();
                }

                @Override
                public void onResponse(Call call, final Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        latch.countDown();
                        throw new IOException("Unexpected code " + response);
                    }
                    try {
                        String res = response.body().string();
                        // store the res under
                        // сохранить результат под
                        if(res.trim().startsWith("[")) {
                            // array
                            // массив
                            private_refs.put(URL, new JSONArray(res));
                        } else if(res.trim().startsWith("{")){
                            // object
                            // объект
                            private_refs.put(URL, new JSONObject(res));
                        } else {
                            // string
                            // строка
                            private_refs.put(URL, res);
                        }
                        latch.countDown();
                    } catch (JSONException e) {
                        Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
                    }
                }
            });

        } catch (Exception e){
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
    }
}
