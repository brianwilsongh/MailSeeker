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
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by brian on 3/20/17.
 */

public class DownloadAsyncTask extends AsyncTask<URL, Void, String> {

    //create instance of listener interface
    private FetchCallback listener;
    URL currentURL;

    //max links to hit
    int linksMaximum;

    //bucket string for holding the full html
    String bucket = "";

    //arraylists to store visited and unvisited urls
    public ArrayList<URL> visitedLinks = new ArrayList<>();
    public ArrayList<URL> collectedLinks = new ArrayList<>();

    //first link visited
    String firstLinkAsString = "";

    public DownloadAsyncTask(FetchCallback listener) {
        this.listener = listener;
    }

    @Override
    protected void onPreExecute() {
        //before executing the download, do the following
        super.onPreExecute();
        linksMaximum = 3;

    }

    @Override
    protected String doInBackground(URL... urls) {
        //main task to do

        String lastResult = "";
        int linksHit = 0;
        firstLinkAsString = urls[0].toString();

        //first fetch is unique and sets up a few things
        try {
            lastResult = fetch(urls[0]);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //add the lastest url to visited URLs arraylist, increment links hit counter, add to bucket
        visitedLinks.add(urls[0]);
        linksHit++;
        if (lastResult != "") {
            bucket = bucket.concat(lastResult);
        }
        Log.v("iterationOne", " added to visitedLinks --: " + visitedLinks.get(0).toString());

        //pull links and sort them based on visited or unvisited
        pullLinks(lastResult);
        cleanLinks();

        while (linksHit < linksMaximum && collectedLinks.size() > 0) {
            //while the links hit counter is below the max, and when collectedLinks isn't empty

            //clean links, then make iterator for the collectedLinks arraylist
            cleanLinks();
            Iterator<URL> iterator = collectedLinks.iterator();

            if (iterator.hasNext()) {
                URL nextUrl = iterator.next();
                //for each URL in collectedLinks array
                //try a fetch using latest URL if visitedLinks does NOT contain this object
                try {
                    Log.v("DLasync.doinBG", " will attempt recursive fetch of: " + nextUrl.toString());
                    lastResult = fetch(nextUrl);
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
    protected void onProgressUpdate(Void... values) {
        //not necessary for now as it is void
        super.onProgressUpdate(values);
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
        String finalResult = null;

        try {
            // open connections for URL object based on http or https
            if (url.getProtocol().equals("http")) {
                connection = (HttpURLConnection) url.openConnection();
                //set the timeout values
                connection.setReadTimeout(3000 * linksMaximum);
                connection.setConnectTimeout(3000);
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
                secureConnection.setConnectTimeout(3000);
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

            if (!possibleUrl.equals("") && (collectedLinks.size() + visitedLinks.size() < linksMaximum)) {
                //if the link attr isn't empty, and links size + collected is less than max
                URL theUrl = NetworkUtils.makeURL(possibleUrl);
                Log.v("pullLinks", "made URL: " + theUrl.toString());

                if (RegexUtils.urlDomainNameMatch(firstLinkAsString, theUrl.toString())) {
                    //if the url is within the same domain
                    if (!urlInArrayList(theUrl, visitedLinks)) {
                        Log.v("DLAsyncTask", " pull thinks that " + theUrl.toString() + " wasn't visited, so adding...");
                        collectedLinks.add(theUrl);
                    }

                    Log.v("DlAsyncTask.pull", " the collected links array is now:" + collectedLinks.toString());
                    Log.v("DlAsyncTask.pull", " the visited links array is now:" + visitedLinks.toString());
                    cleanLinks();
                }
            }

        }

        cleanLinks();
    }

    private void cleanLinks() {
        //iterator to go over and clean out collectedLinks
        //TODO: find a more efficient way to do this
        for (Iterator itr = visitedLinks.iterator(); itr.hasNext(); ) {
            URL nowUrl = (URL) itr.next();
            if (urlInArrayList(nowUrl, collectedLinks)) {
                itr.remove();
                Log.v("DLasyncTask", "Just cleaned: " + nowUrl);
                Log.v("collectedLinks", " is now:" + collectedLinks.toString());
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


    private void sleepSeconds(int time) {
        //try sleeping for 2 seconds...
        try {
            TimeUnit.SECONDS.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private boolean urlInArrayList(URL url, ArrayList<URL> list){
        boolean returnBoolean = false;

        for (URL listedUrl : list){
            Log.v("urlInArrayList", " checking list item " + listedUrl.toString() + " to test url " + url.toString());
            if (NetworkUtils.urlPathMatch(listedUrl, url)) {
                returnBoolean = true;
            }
        }
        Log.v("urlInArrayList says:", " no " + url.toString() + " in " + list.toString());
        return returnBoolean;
    }


}
