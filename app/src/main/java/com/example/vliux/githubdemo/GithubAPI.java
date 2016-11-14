package com.example.vliux.githubdemo;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.util.Base64;
import android.util.Log;

import com.google.gson.Gson;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * Created by vliux on 11/14/16.
 */
public class GithubAPI {

    public GithubAPI(final Context mContext) {
        this.mLocalCache = new LocalCache(mContext);
    }

    @WorkerThread
    @Nullable
    public Bean queryRulesContent(final String user, final String repo) throws IOException {
        return makeQuery(getRulesUrl(user, repo), user, repo);
    }

    public Bean queryReadmeContent(final String user, final String repo) throws IOException {
        return makeQuery(getReadmeUrl(user, repo), user, repo);
    }

    private Bean makeQuery(final String queryUrl, final String user, final String repo) throws IOException {
        final String etag = mLocalCache.getEtag(user, repo);
        final URL url = new URL(queryUrl);
        final HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        if(null != etag){
            urlConnection.setRequestProperty("If-None-Match", etag);
        }
        try {
            final Map<String, List<String>> headers = urlConnection.getHeaderFields();
            if(sDebug) {
                Log.d(TAG, "QUERY_CONTENT HTTP RESPONSE HEADERS:");
                for (final Map.Entry<String, List<String>> entry : headers.entrySet()) {
                    Log.d(TAG, " " + entry.getKey() + ":");
                    for (final String value : entry.getValue()) {
                        Log.d(TAG, "     " + value);
                    }
                }
            }
            final int responseCode = urlConnection.getResponseCode();
            switch (responseCode){
                case KResponseOK:
                    saveEtag(user, repo, headers);
                    return readContentResponse(new BufferedInputStream(urlConnection.getInputStream()));
                case KResponseNotModified:
                    Log.d(TAG, "content hasn't changed since last query");
                    return null;
                default:
                    return null;
            }
        } finally {
            urlConnection.disconnect();
        }
    }

    private void saveEtag(final String user, final String repo, final Map<String, List<String>> reponseHeaders){
        final List<String> etagValues = reponseHeaders.get(KHeaderEtag);
        if (null != etagValues && etagValues.size() > 0) {
            final String newEtag = etagValues.get(0);
            if(null != newEtag) mLocalCache.setEtag(user, repo, newEtag);
        }
    }

    private Bean readContentResponse(final InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder sb = new StringBuilder();
        String line;
        while((line = reader.readLine()) != null) {
            sb.append(line);
        }

        final String result = sb.toString();
        Log.d(TAG, "QUERY_CONTENT HTTP RESPONSE BODY:\n" + sb.toString());
        //Type type = new TypeToken<Map<String, String>>(){}.getType();
        Bean bean = new Gson().fromJson(result, Bean.class);
        final String rawContent = bean.getContent();
        if(null != rawContent && rawContent.length() > 0){
            final String decodedContent = new String(Base64.decode(rawContent, Base64.DEFAULT));
            Log.d(TAG, "DECODED_CONTENT=\n" + decodedContent);
            bean.content = decodedContent;
        }else{
            Log.e(TAG, "content in response is empty or null");
            throw new IOException("content in response is empty or null");
        }
        return bean;
    }

    private static String getRulesUrl(final String user, final String repo){
        return String.format("https://api.github.com/repos/%s/%s/contents/%s.xml", user, repo, repo);
    }

    private static String getReadmeUrl(final String user, final String repo){
        return String.format("https://api.github.com/repos/%s/%s/readme", user, repo);
    }

    public static class Bean {
        private String content;

        public String getContent() {
            return content;
        }

        public void setContent(final String content) {
            this.content = content;
        }
    }

    private static class LocalCache {
        public LocalCache(final Context mContext) {
            this.mSharedPrefs = mContext.getSharedPreferences(KSpName, Context.MODE_PRIVATE);
        }

        String getEtag(final String user, final String repo){
            return mSharedPrefs.getString(getSpKey(user, repo), null);
        }

        void setEtag(final String user, final String repo, final String etag){
            mSharedPrefs.edit().putString(getSpKey(user, repo), etag).apply();
        }

        private static String getSpKey(final String user, final String repo){
            return user + ":" + repo;
        }

        private final SharedPreferences mSharedPrefs;
    }

    private final LocalCache mLocalCache;

    private static final String TAG = "GithubAPI";
    private static final String KSpName = "prescriptions";
    private static final boolean sDebug = true;

    private static final String KHeaderEtag = "Etag";

    private static final int KResponseOK = 200;
    private static final int KResponseNotModified = 304;
}
