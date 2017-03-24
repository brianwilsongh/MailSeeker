package com.wordpress.httpspandareaktor.quant;

import android.util.Log;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by brian on 3/23/17.
 */

public class NetworkUtils {

    public static URL makeURL(String string) {
        URL returnURL = null;
        try {
            returnURL = new URL(string);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            Log.v("NetworkUtils", " I failed to make url from: " + string);
        }
        return returnURL;
    }

    public static boolean urlPathMatch(URL urlA, URL urlB) {
        //check if the paths match of built URL objects

        //build a url to make string for A, then B
        String pathA = urlA.getPath();
        String pathB = urlB.getPath();
        return pathA.equals(pathB);

    }

}
