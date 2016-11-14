package com.example.vliux.githubdemo;

import android.os.AsyncTask;
import android.support.annotation.WorkerThread;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.internal.Excluder;
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
    private TextView mTvReadme;
    private TextView mTvRules;
    private GithubAPI mGithubApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTvReadme = (TextView) findViewById(R.id.tv_readme);
        mTvRules = (TextView)findViewById(R.id.tv_rules);
        mGithubApi = new GithubAPI(this);
    }

    public void onClick(final View view){
        new QueryTask().execute();
    }

    private final class QueryTask extends AsyncTask<Object, Integer, GithubAPI.Bean[]> {
        @Override
        protected GithubAPI.Bean[] doInBackground(final Object... params) {
            try {
                final GithubAPI.Bean[] beans = new GithubAPI.Bean[2];
                beans[0] = mGithubApi.queryReadmeContent("greenify", "rx-mipush");
                beans[1] = mGithubApi.queryRulesContent("greenify", "rx-mipush");
                return beans;
            }catch (final Exception e){
                return null;
            }
        }

        @Override
        protected void onPostExecute(final GithubAPI.Bean[] been) {
            if(null == been){
                mTvReadme.setText("ERROR");
            }else{
                mTvReadme.setText(been[0].getContent());
                mTvRules.setText(been[1].getContent());
            }
        }
    };
}
