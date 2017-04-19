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
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by brian on 3/20/17.
 */

public class DownloadAsyncTask extends AsyncTask<URL, String, String> {

    //create instance of listener interface
    private FetchCallback listener;

    //the search term if it exists
    private String mSearchTerm;

    //String array for onProgressUpdate
    private List<Object> updateArray = new ArrayList<Object>();

    //max links to hit, set by preferences menu
    private int mlinksMaximum;

    //bucket string for holding the full html
    String bucket = "";

    //arraylists to store visited and unvisited urls
    private HashSet<URL> visitedLinks = new HashSet<>();
    private HashSet<URL> collectedLinks = new HashSet<>();

    //first link visited
    private String firstLinkAsString = "";

    public DownloadAsyncTask(FetchCallback listener, String searchTerm) {
        //set the declared FetchCallback named "listener" to the listener provided in constructor
        //this links this DLAsyncTask to MainActivity, which is instance of FetchCallback interface
        this.listener = listener;
        mSearchTerm = searchTerm;
    }

    @Override
    protected void onPreExecute() {
        //before executing the download, do the following
        super.onPreExecute();

    }

    @Override
    protected String doInBackground(URL... urls) {
        //main task to do
        publishProgress("Initialize request...", " at URL: " + urls[0]);

        //lastResult is the most recent webpage pulled from domain
        String lastResult = "";

        //failedPullLinkCount is a counter to keep track of failure to pull enough URLs in while loop
        int failedPullLinkCount = 0;


        int pagesHit = 0;
        firstLinkAsString = urls[0].toString();

        //first fetch is unique and sets up a few things or the other iterations
        try {
            lastResult = fetch(urls[0]);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //add the lastest url to visited URLs arraylist, increment links hit counter, add to bucket
        visitedLinks.add(urls[0]);
        if (!lastResult.equals("")) {
            //if the last result wasn't empty, it was a page hit. add the results into bucket
            pagesHit ++;
            bucket = bucket.concat(lastResult);
        } else {
            return "First query returned nothing!";
        }

        //pull first round of links and sort them based on visited or unvisited
        pullLinks(lastResult);
        Log.v("DLAsync", " completed INITIAL pull of links from first query");
        cleanCollectedUrls();

        while (!isCancelled()) {
            //while the links hit counter is below the max, and when collectedLinks isn't empty

            //clean links, then make iterator for the collectedLinks arraylist
            Iterator<URL> iterator = collectedLinks.iterator();

            if (iterator.hasNext()) {
                URL thisUrl = iterator.next();
                //for each URL in collectedLinks array
                //try a fetch using latest URL if visitedLinks does NOT contain this object
                try {
                    //sleep to prevent server from kicking off ip
                    sleepMilliseconds(500);
                    Log.v("DLasync.doinBG", " now attempting fetch of: " + thisUrl.toString());
                    sleepMilliseconds(500);

                    //send update before and after fetch is executed

                    sendUpdate("Seeker moving into iteration " + pagesHit + "... ", null);

                    //this is the supposed HTML page we got from the server
                    lastResult = fetch(thisUrl);
                    sleepMilliseconds(500);
                    //extract emails and send
//                    Document document = Jsoup.parse(lastResult);
                    sendUpdate("Purifying HTML contents...", RegexUtils.purify(lastResult, mSearchTerm));



                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (lastResult != null && !lastResult.equals("")) {
                    //if last result wasn't blank or null

                    cleanCollectedUrls();
                    //clean the collected list as we will check to see if there are more unvisited URLs
                    if (collectedLinks.size() < 2){
                        //if there are less than 2 links on the collected list, try pulling more
                        pullLinks(lastResult);
                    }

                    //add to the bucket, increment counter
                    pagesHit++;
                    bucket = bucket.concat(lastResult);
                    visitedLinks.add(thisUrl);
                    collectedLinks.remove(thisUrl);

                    Log.v("downloadAsyncTask", "visited links is now:" + pagesHit);
                    Log.v("the url ", "I just fetched: " + thisUrl.toString());
                    Log.v("the result", "was: " + lastResult);

                } else {
                    //the fetch was not fruitful so don't do pagesHit++, but we've visited this link. remove from collected
                    visitedLinks.add(thisUrl);
                    collectedLinks.remove(thisUrl);
                }

            } else {
                //If there are no collected URLs left, clean and try pull and add to failedPull incrementor
                publishProgress("Ran out of links! Attempting to pull more", "", "");
                cleanCollectedUrls();
                pullLinks(bucket);
                failedPullLinkCount++;
                Log.v("DLasync", " Ran out of URLs, tried to pull more... Try number " + failedPullLinkCount);
                if (failedPullLinkCount == 3){
                    //on third strike, you're out of the loop
                    Log.v("DLAsync", " Totally out of URLs, crawling is finished!");
                    break;
                }
            }

        } //END OF WHILE LOOP


        Log.v("DLasyncTask", " final list of visited:" + visitedLinks);
        return bucket;
    }

    @Override
    protected void onProgressUpdate(String... values) {
        //send info back to main so user can know data is coming in
        listener.onUpdate(updateArray);
        updateArray.clear();
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
                connection.setReadTimeout(3000 * mlinksMaximum);
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
                secureConnection.setReadTimeout(3000 * mlinksMaximum);
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
        //prevent repeated suspicious activity from server
        int multipliedParam = (int) (Math.random() * time + 1);

        try {
            TimeUnit.MILLISECONDS.sleep(multipliedParam);
            Log.v("DLASync.sleep", " sleep " + multipliedParam + " milliseconds...");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private boolean urlInHashSet(URL url, HashSet<URL> set){
        //checks if the URL is in a provided HashSet with an improved for loop
        boolean returnBoolean = false;

        for (URL setItem : set){
            if (NetworkUtils.urlHostPathMatch(setItem, url)) {
                Log.v("DLAsync.urlInHashSet", " just found " + url.toString() + " in " + set.toString());
                returnBoolean = true;
            }
        }
        return returnBoolean;
    }

    private void sendUpdate(String progressText, String[] emailsArray){
        //builds an update array to send to onProgressUpdate by calling publishProgress
        updateArray.add(progressText);
        updateArray.add(emailsArray);
        publishProgress();
    }


}
