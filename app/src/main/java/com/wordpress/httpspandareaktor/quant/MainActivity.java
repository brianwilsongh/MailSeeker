package com.wordpress.httpspandareaktor.quant;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
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
import java.util.List;
import java.util.TreeMap;
import java.util.prefs.Preferences;

public class MainActivity extends AppCompatActivity implements FetchCallback {

    //this textview shows emails found, start as view.gone
    TextView emailDisplay;
    byte emailsFound = 0;

    //this EditText is where the user's URL input goes, queried URL is another store (created URL) of the input
    EditText inputURL;
    URL queriedURL;

    //the progress bar that starts invisible but is revealed after search begins, and the search term if it exists
    LinearLayout progressBar;
    EditText searchTermField;
    String searchTerm;

    //the top section contains the search term and URL edit text boxes
    LinearLayout topSection;

    //text below progress bar
    TextView progressText;

    //is the AsyncTask currently running?
    boolean currentlyRunning;

    //create a pointer to control each asynctask if needed
    DownloadAsyncTask currentAsyncTask;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        searchTermField = (EditText) findViewById(R.id.searchTermField);

        inputURL = (EditText) findViewById(R.id.inputURL);

        progressBar = (LinearLayout) findViewById(R.id.progressBar);
        topSection = (LinearLayout) findViewById(R.id.topSection);

        progressText = (TextView) findViewById(R.id.progressText);
        emailDisplay = (TextView) findViewById(R.id.emailDisplay);

        //AsyncTask currently NOT running, thus:
        currentlyRunning = false;

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //create the menu, currently holding just the settings page
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //recursive listener for items on the menu
        switch (item.getItemId()) {
            case R.id.menu_goToSettings:
                Intent intent = new Intent(this, Settings.class);
                startActivity(intent);
                return true;
            default:
                return onOptionsItemSelected(item);
        }
    }

    public void extractButton(View view) {

        if (!networkAvailable()) {
            //Error message if the network is unavailable
            Toast.makeText(this, "Network unavailable!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentlyRunning) {
            //Error message if there is another DLAsyncTask is already running
            Toast.makeText(this, "Cannot do two tasks at once!", Toast.LENGTH_SHORT).show();
        }

        //User just typed in a URL and requested fetch
        if (!inputURL.getText().toString().equals("")) {
            //if not empty, try to build URL, makeURL shoudld catch MalformedURLException
            URL currentURL = NetworkUtils.makeURL(inputURL.getText().toString());

            if (!(currentURL == null)) {
                searchTerm = searchTermField.getText().toString();
                //if the currentlyRunning boolean says there are no current tasks going, make a new one and reference it
                DownloadAsyncTask mDownloadAsyncTask = new DownloadAsyncTask(this, searchTerm);
                currentAsyncTask = mDownloadAsyncTask;

                //new task created so set boolean to true
                currentlyRunning = true;
                topSection.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
                emailDisplay.setVisibility(View.VISIBLE);

                //store the url query as a string so we can do stuff with it later
                queriedURL = NetworkUtils.makeURL(inputURL.getText().toString());

                //execute the asyncTask
                mDownloadAsyncTask.execute(currentURL);
            } else {
                Toast.makeText(this, "Bad URL! Try again", Toast.LENGTH_SHORT).show();
            }

        } else {
            Toast.makeText(this, "Cannot extract from an empty URL!", Toast.LENGTH_SHORT).show();
        }
    }

    public void clearURL(View view) {
        inputURL.setText("");
    }

    @Override
    public void onUpdate(List<Object> list) {
        //update the UI elements (progress text, emailDisplay)

        try {
            //retrieve the first item in the update array, which is progress text, show if not null
            String newLoadingText = list.get(0).toString();
            if (newLoadingText != null) {
                progressText.setText(newLoadingText);
            }

            String[] newEmails = (String[]) list.get(1);
            for (String email : newEmails) {
                if (email != null && !emailDisplay.getText().toString().contains(email)) {
                    String existingEmails = emailDisplay.getText().toString();
                    emailDisplay.setText(email + "\n" + existingEmails);
                    emailsFound += 1;
                }
            }

        } catch (IndexOutOfBoundsException e){
            e.printStackTrace();
        } catch (NullPointerException e){
            e.printStackTrace();
        }

        if (emailsFound > 20) {
            killTask(emailDisplay);
        }

    }

    public void killTask(View view) {
        //user wants to kill the AsyncTask
        if (currentlyRunning) {
            currentAsyncTask.cancel(true);
            progressBar.setVisibility(View.GONE);
            currentlyRunning = false;
        }
    }

    @Override
    public void onFinish(String s) {
        Toast.makeText(this, "Completed", Toast.LENGTH_LONG).show();
    }

    public boolean networkAvailable() {
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


}
