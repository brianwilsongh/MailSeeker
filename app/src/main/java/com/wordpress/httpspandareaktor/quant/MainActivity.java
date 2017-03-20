package com.wordpress.httpspandareaktor.quant;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.examples.HtmlToPlainText;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends AppCompatActivity implements FetchCallback {

    //this TextView will display the data
    TextView dataFeed;
    EditText inputURL;
    ProgressBar progressBar;
    boolean currentlyRunning;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //get references to the editText field for URL
        dataFeed = (TextView) findViewById(R.id.dataFeed);
        inputURL = (EditText) findViewById(R.id.inputURL);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        currentlyRunning = false;


    }


    public void extractButton(View view){
        if (inputURL.getText().toString() != "") {

            URL currentURL = makeURL(inputURL.getText().toString());
            DownloadAsyncTask mDownloadAsyncTask = new DownloadAsyncTask(this);

            //if currently not running, execute the DownloadAsyncTask
            if (currentlyRunning == false) {
                currentlyRunning = true;
                progressBar.setVisibility(View.VISIBLE);
                mDownloadAsyncTask.execute(currentURL);
            } else {
                Toast.makeText(this, "Wait until the current task is finished!", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Cannot extract from an empty URL!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onFinish(String s) {

        //we're testing Jsoup here

        Document doc = Jsoup.parse(s);
        s = RegexUtils.cleanText(doc.body().text());

        //set the desired text in the box, no longer running so now false
        progressBar.setVisibility(View.GONE);
        dataFeed.setText(s);
        currentlyRunning = false;
    }

    public boolean isNetworkAvailable() {
        //returns boolean to determine whether network is available, requires ACCESS_NETWORK_STATE permission
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        // if network is off, networkInfo will be null
        //otherwise check if we are connected
        if (networkInfo != null && networkInfo.isConnected()) {
            return true;
        }
        return false;
    }

    public URL makeURL(String string) {
        URL returnURL = null;
        try {
            returnURL = new URL(string);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return returnURL;
    }



}
