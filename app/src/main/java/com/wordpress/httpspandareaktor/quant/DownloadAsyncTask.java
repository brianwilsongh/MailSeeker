package com.wordpress.httpspandareaktor.quant;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by brian on 3/20/17.
 */

public class DownloadAsyncTask extends AsyncTask<URL, String, String> {

    //create instance of listener interface
    private FetchCallback listener;
    URL currentURL;

    //String array for onProgressUpdate
    private String[] updateArray = new String[3];

    //max links to hit
    private int linksMaximum;

    //bucket string for holding the full html
    String bucket = "";

    //arraylists to store visited and unvisited urls
    private HashSet<URL> visitedLinks = new HashSet<>();
    private HashSet<URL> collectedLinks = new HashSet<>();

    //first link visited
    private String firstLinkAsString = "";

    public DownloadAsyncTask(FetchCallback listener) {
        this.listener = listener;
    }

    @Override
    protected void onPreExecute() {
        //before executing the download, do the following
        super.onPreExecute();
        linksMaximum = 4;

    }

    @Override
    protected String doInBackground(URL... urls) {
        //main task to do
        publishProgress("Initialize request...", " at URL: " + urls[0]);

        //lastResult is the most recent webpage pulled from domain
        String lastResult = "";


        int linksHit = 0;
        firstLinkAsString = urls[0].toString();

        //first fetch is unique and sets up a few things or the other iterations
        try {
            lastResult = fetch(urls[0]);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //add the lastest url to visited URLs arraylist, increment links hit counter, add to bucket
        visitedLinks.add(urls[0]);
        Log.v("DLAsync", " add to visitedLinks set: " + urls[0]);
        linksHit ++;
        if (!lastResult.equals("")) {
            bucket = bucket.concat(lastResult);
        } else {
            return "First query returned nothing!";
        }

        //pull first round of links and sort them based on visited or unvisited
        pullLinks(lastResult);
        Log.v("DLAsync", " completed INITIAL pull of links from first query");
        cleanCollectedUrls();

        while (linksHit < linksMaximum) {
            //while the links hit counter is below the max, and when collectedLinks isn't empty

            //clean links, then make iterator for the collectedLinks arraylist
            Iterator<URL> iterator = collectedLinks.iterator();

            if (iterator.hasNext()) {
                URL nextUrl = iterator.next();
                //for each URL in collectedLinks array
                //try a fetch using latest URL if visitedLinks does NOT contain this object
                try {
                    //sleep to prevent server from kicking off ip
                    sleepMilliseconds(5000);
                    Log.v("DLasync.doinBG", " now attempting fetch of: " + nextUrl.toString());

                    //send update before and after fetch is executed

                    sendUpdate("Attempting next URL... " + nextUrl.toString(), "", "");

                    lastResult = fetch(nextUrl);

                    sendUpdate("Extracting data from " + nextUrl + "...", lastResult, nextUrl.toString() + "\n");

                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (lastResult != null) {

                    //add to the bucket, increment counter
                    linksHit++;
                    bucket = bucket.concat(lastResult);
                    visitedLinks.add(nextUrl);
                    collectedLinks.remove(nextUrl);

                    Log.v("downloadAsyncTask", "visited links is now:" + linksHit);
                    Log.v("the url ", "I just fetched was: " + nextUrl.toString());
                    Log.v("the result", "was: " + lastResult);
                    Log.v("visited url array:", " " + visitedLinks);
                    Log.v("collected url array:", " " + collectedLinks);

                }

            } else {
                pullLinks(bucket);
                Log.v("DLasync", " WE RAN OUT OF URLs, FETCHING MORE");
            }

        }
        Log.v("DLasyncTask", " final list of visited:" + visitedLinks);
        return bucket;
    }

    @Override
    protected void onProgressUpdate(String... values) {
        //send info back to main so user can know data is coming in
        listener.onUpdate(updateArray);

    }

    @Override
    protected void onPostExecute(String s) {
        //this is what is returned
        listener.onFinish(s);
        this.cancel(true);

    }


    private String fetch(URL url) throws IOException {
        //make a request to this URL, returns the response as a string

        //input stream, httpURLConnection, and the resulting string declared here
        InputStream stream = null;
        HttpURLConnection connection = null;
        HttpsURLConnection secureConnection = null;
        String finalResult = "";

        try {
            // open connections for URL object based on http or https
            if (url.getProtocol().equals("http")) {
                connection = (HttpURLConnection) url.openConnection();
                //set the timeout values
                connection.setReadTimeout(3000 * linksMaximum);
                connection.setConnectTimeout(5000);
                //set the request method, because we are gonna GET
                connection.setRequestMethod("GET");
                connection.connect();

                if (connection.getResponseCode() == 200) {
                    stream = connection.getInputStream();
                    if (stream != null) {
                        finalResult = convertResponse(stream);
                    }
                }
            }
            if (url.getProtocol().equals("https")) {
                //do all the stuff for https here separately for now because of casting issues
                secureConnection = (HttpsURLConnection) url.openConnection();
                secureConnection.setReadTimeout(3000 * linksMaximum);
                secureConnection.setConnectTimeout(5000);
                secureConnection.setRequestMethod("GET");
                secureConnection.connect();

                if (secureConnection.getResponseCode() == 200) {
                    stream = secureConnection.getInputStream();
                    if (stream != null) {
                        finalResult = convertResponse(stream);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            if (secureConnection != null) {
                secureConnection.disconnect();
            }
            if (stream != null) {
                stream.close();
            }
        }
        return finalResult;

    }

    private void pullLinks(String htmlPage) {
        //this method pulls links from a page, if they haven't been visited, add into unvisited ArrayList<URL>

        Document doc = Jsoup.parse(htmlPage);
        Elements links = doc.select("a[href]");

        for (Element link : links) {

            String possibleUrl = link.attr("abs:href");

            if (!possibleUrl.equals("")) {
                //if the link attr isn't empty, make a URL
                URL theUrl = NetworkUtils.makeURL(possibleUrl);
                Log.v("DLAsync.pullLinks", " created URL from page: " + theUrl.toString());
                Log.v(".pullLink collect-visit", " HashSet contents = " + collectedLinks + " " + visitedLinks);

                if (RegexUtils.urlDomainNameMatch(firstLinkAsString, theUrl.toString())) {
                    //if the url is within the same domain as original query
                    if (!visitedLinks.contains(theUrl)) {
                        Log.v("DLAsyncTask.pullLinks", " thinks that " + theUrl.toString() + " wasn't visited, add into collected...");
                        collectedLinks.add(theUrl);
                    }
                    Log.v("DlAsyncTask.pull", " the collected links array is now:" + collectedLinks.toString());
                    Log.v("DlAsyncTask.pull", " the visited links array is now:" + visitedLinks.toString());
                }
            }

        }
    }

    private void cleanCollectedUrls() {
        //iterator to go over and clean out collectedLinks HashSet
        for (Iterator itr = visitedLinks.iterator(); itr.hasNext(); ) {
            URL thisURL = (URL) itr.next();
            if (urlInHashSet(thisURL, collectedLinks)) {
                collectedLinks.remove(thisURL);
                Log.v("DLasync.cleanCollected", " from CollectedLinks, just cleaned: " + thisURL);
                Log.v(".cleanCollected", " collected set is now:" + collectedLinks.toString());
            }
        }

    }


    private String convertResponse(InputStream stream) throws IOException {
        //pass in an InputStream and this will return all the chars
        String returnString = null;

        //ISReader converts bytecode into chars assuming it's UTF-8, buffered reads line by line
        InputStreamReader inputReader = new InputStreamReader(stream, "UTF-8");
        BufferedReader reader = new BufferedReader(inputReader);

        //have line be the first line, as long as it's not null go into loop to append it then go to next
        String currentLine = reader.readLine();
        while (currentLine != null) {
            returnString += currentLine;
            currentLine = reader.readLine();
        }
        return returnString;

    }


    private void sleepMilliseconds(int time) {
        //try sleeping randomly up to time milliseconds
        int multipliedParam = (int) (Math.random() * time + 1);

        try {
            TimeUnit.MILLISECONDS.sleep(multipliedParam);
            Log.v("DLASync.sleep", " sleep " + multipliedParam + " milliseconds...");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private boolean urlInHashSet(URL url, HashSet<URL> set){
        boolean returnBoolean = false;

        for (URL setItem : set){
            Log.v("DLAsync.urlInHashSet", " checking new potential set item " + url.toString() + " equivalency to this existing HashSet url: " + setItem.toString());
            if (NetworkUtils.urlAuthPathMatch(setItem, url)) {
                returnBoolean = true;
            }
        }
        Log.v("DLAsync.urlInHashSet", " found no " + url.toString() + " in " + set.toString());
        return returnBoolean;
    }

    private void sendUpdate(String progressText, String dataText, String lastUrl){
        //builds an update array to send to onProgressUpdate by calling publishProgress
        updateArray[0] = progressText;
        updateArray[1] = dataText;
        updateArray[2] = lastUrl;
        publishProgress(updateArray);
    }


}
