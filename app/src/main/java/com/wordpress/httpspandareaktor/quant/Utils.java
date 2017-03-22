package com.wordpress.httpspandareaktor.quant;

import android.util.Log;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by brian on 3/22/17.
 */

public class Utils {

    public static boolean urlDomainNameMatch(String urlA, String urlB){
        //check if the host names of two urls match, regardless of www in front of them or not
        //this currently checks if the url pulled by Jsoup matches the base URL input by the user


        //build a url to make string for A, then B
        String hostA = "";
        try {
            URL builtUrlA = new URL(urlA);
            hostA = builtUrlA.getHost();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        String hostB = "";
        try {
            URL builtUrlB = new URL(urlB);
            hostB = builtUrlB.getHost();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        Log.v("Utils.urlDomainName", "host urls A, B = " + hostA + " :: " + hostB);


        //now extract substring from strings, domain name, to take care of www or not www in URLs
        String siteNameA = "";
        //search for the pattern I want, like "google.com" in http://www.google.com/maps using RegEx
        Pattern patternA = Pattern.compile("([^\\.]+)\\.([^\\.]+)$");
        Matcher matcherA = patternA.matcher(hostA);
        if (matcherA.find())
        {
            siteNameA = matcherA.group();
        }

        String siteNameB = "";
        //search for the pattern I want, like "google.com/" in http://www.google.com/maps
        Pattern patternB = Pattern.compile("([^\\.]+)\\.([^\\.]+)$");
        Matcher matcherB = patternB.matcher(hostB);
        if (matcherB.find())
        {
            siteNameB = matcherB.group();
        }

        Log.v("Utils.urlDomainNAme", " cleaned urls A, B = " + siteNameA + " :: " + siteNameB);

        return siteNameA.equals(siteNameB);

    }

}
