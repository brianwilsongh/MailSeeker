package com.wordpress.httpspandareaktor.quant;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.examples.HtmlToPlainText;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.TreeMap;

public class MainActivity extends AppCompatActivity implements FetchCallback {

    //this TextView will display the data
    TextView dataFeed;

    //this textview shows topwords, start as view.gone
    TextView topWords;

    //this textiew shows the urls found on the page, we will crawl later
    TextView urlsDiscovered;

    //this EditText is where the user's URL input goes, absolute URL is set to domain name
    EditText inputURL;
    String absoluteURL;

    //the progress bar that starts invisible but is b
    ProgressBar progressBar;

    //is the AsyncTask currently running?
    boolean currentlyRunning;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //get references to the Views
        dataFeed = (TextView) findViewById(R.id.dataFeed);
        inputURL = (EditText) findViewById(R.id.inputURL);
        inputURL.setTextIsSelectable(true);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        topWords = (TextView) findViewById(R.id.topWords);
        urlsDiscovered = (TextView) findViewById(R.id.urlsDiscovered);

        //AsyncTask currently NOT running, thus:
        currentlyRunning = false;


    }


    public void extractButton(View view) {
        //User just typed in a URL and requested fetch
        if (inputURL.getText().toString() != "") {
            //if not empty, try to build URL, makeURL shoudld catch MalformedURLException
            URL currentURL = makeURL(inputURL.getText().toString());

            //if good to go, make a new AsyncTask, if no others are running make a new instance and execute
            DownloadAsyncTask mDownloadAsyncTask = new DownloadAsyncTask(this);

            //if currently not running, execute the DownloadAsyncTask
            if (!currentlyRunning) {
                currentlyRunning = true;
                progressBar.setVisibility(View.VISIBLE);

                //store the url query as a string so we can do stuff with it later
                absoluteURL = inputURL.getText().toString();

                //execute the asyncTask
                mDownloadAsyncTask.execute(currentURL);
            } else {
                Toast.makeText(this, "Wait until the current task is finished!", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Cannot extract from an empty URL!", Toast.LENGTH_SHORT).show();
        }
    }

    public void clearURL(View view) {
        inputURL.setText("");
    }

    @Override
    public void onFinish(String s) {

        if (s != null) {
            //we're testing Jsoup here
            Document doc = Jsoup.parse(s);

            StringBuilder urlsFound = new StringBuilder();

            Elements links = doc.select("a[href]");
            Log.v("MainActivity.onFinish", " Jsoup found urls: " + links.toString());

            for (Element link : links){
                if (link.attr("abs:href") != "") {
                    if (Utils.urlDomainNameMatch(link.attr("abs:href"), absoluteURL)){
                        urlsFound.append("INTERNAL: " + link.attr("abs:href"));
                        urlsFound.append("\n");
                        urlsFound.append("\n");
                    } else {
                        urlsFound.append("EXTERNAL: " + link.attr("abs:href"));
                        urlsFound.append("\n");
                        urlsFound.append("\n");

                    }
                }
            }

            urlsDiscovered.setText(urlsFound.toString());

            s = RegexUtils.cleanText(doc.body().text(), true, true, true);



            //set the desired text in the box
            if (!(s.equals(""))) {
                dataFeed.setText(s);

                //use frequency map generate in Abathur, append values to topWords TextView
                topWords.setText(Abathur.findFrequency(s));
                topWords.setVisibility(View.VISIBLE);

            } else {
                Toast.makeText(this, "Extraction returned with nothing, check URL!", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Extraction returned with nothing, check URL!", Toast.LENGTH_SHORT).show();
        }

        //when not running AsyncTask, hide ProgressBar and set boolean to false
        progressBar.setVisibility(View.GONE);
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
