package com.wordpress.httpspandareaktor.quant;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by brian on 3/20/17.
 */

public class DownloadAsyncTask extends AsyncTask<URL, Void, String> {

    //create instance of listener interface
    private FetchCallback listener;
    URL currentURL;

    public DownloadAsyncTask(FetchCallback listener){
        this.listener = listener;
    }

    @Override
    protected void onPreExecute() {
        //before executing the download, do the following
        super.onPreExecute();
    }

    @Override
    protected String doInBackground(URL... urls) {
        //main task to do
        String returnValue = null;
        try {returnValue = fetch(urls[0]);}
        catch (IOException e) {
            e.printStackTrace();
        }
        return returnValue;
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
            Log.v("DownloadAsyncTask.fetch", "protocol of url is..." + url.getProtocol());
            // open connections for URL object based on http or https
            if (url.getProtocol().equals("http")) {
                connection = (HttpURLConnection) url.openConnection();
                //set the timeout values
                connection.setReadTimeout(6000);
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
                secureConnection.setReadTimeout(6000);
                secureConnection.setConnectTimeout(3000);
                secureConnection.setRequestMethod("GET");
                secureConnection.connect();

                if (secureConnection.getResponseCode() == 200){
                    stream = secureConnection.getInputStream();
                    if (stream != null) {
                        finalResult = convertResponse(stream);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null){
                connection.disconnect();
            }
            if (secureConnection != null){
                secureConnection.disconnect();
            }
            if (stream != null){
                stream.close();
            }
        }
        return finalResult;

    }


    private String convertResponse(InputStream stream) throws IOException {
        //pass in an InputStream and this will return all the chars
        String returnString = null;

        //ISReader converts bytecode into chars assuming it's UTF-8, buffered reads line by line
        InputStreamReader inputReader = new InputStreamReader(stream, "UTF-8");
        BufferedReader reader = new BufferedReader(inputReader);

        //have line be the first line, as long as it's not null go into loop to append it then go to next
        String currentLine = reader.readLine();
        while (currentLine != null){
            returnString += currentLine;
            currentLine = reader.readLine();
        }
        return returnString;

    }




}
