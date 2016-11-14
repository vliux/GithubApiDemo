package com.example.vliux.githubdemo;

import android.os.AsyncTask;
import android.support.annotation.WorkerThread;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private EditText mEtBase64;
    private GithubAPI mGithubApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mEtBase64 = (EditText)findViewById(R.id.et_decode_mode);
        mGithubApi = new GithubAPI(this);
    }

    public void onClick(final View view){
        final int base64Decode = Integer.valueOf(mEtBase64.getText().toString());
        Log.d(TAG, "base64 decode mode = " + base64Decode);
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    mGithubApi.queryContent("greenify", "rx-mipush");
                } catch (IOException e) {
                    Log.e(TAG, "error!", e);
                }
            }
        });
    }

    @WorkerThread
    private void makeQuery(final int base64DecodeMode) throws IOException {
        URL url = new URL("https://api.github.com/repos/greenify/rx-mipush/contents/rx-mipush.xml");
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        try {
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            readResponse(in,base64DecodeMode);

            if(false) {
                final Map<String, List<String>> headers = urlConnection.getHeaderFields();
                Log.d(TAG, "HTTP RESPONSE HEADERS:");
                for (final Map.Entry<String, List<String>> entry : headers.entrySet()) {
                    Log.d(TAG, " " + entry.getKey() + ":");
                    for (final String value : entry.getValue()) {
                        Log.d(TAG, "     " + value);
                    }
                }
            }
        } finally {
            urlConnection.disconnect();
        }
    }

    private void readResponse(final InputStream in, final int base64DecodeMode) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder sb = new StringBuilder();
        String line;
        while((line = reader.readLine()) != null) {
            sb.append(line);
        }

        final String result = sb.toString();
        Log.d(TAG, "HTTP RESPONSE BODY:\n" + sb.toString());
        //Type type = new TypeToken<Map<String, String>>(){}.getType();
        Bean bean = new Gson().fromJson(result, Bean.class);
        final String content = bean.getContent();
        Log.d(TAG, "CONTENT:");
        Log.d(TAG, content);
        if(null != content && content.length() > 0){
            final String decodedContent = new String(Base64.decode(content, base64DecodeMode));
            Log.d(TAG, "DECODED_CONTENT=\n" + decodedContent);
        }else{
            Log.e(TAG, "content in response is empty or null");
        }
    }

    private static final String TAG = "vliux";

    private static class Bean {
        private String content;

        public String getContent() {
            return content;
        }

        public void setContent(final String content) {
            this.content = content;
        }
    }
}
