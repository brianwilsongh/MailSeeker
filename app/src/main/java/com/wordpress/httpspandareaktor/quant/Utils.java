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


        //now extract substring from strings, domain name, to take care of www or not www in URLs
        String siteNameA = "";
        //search for the pattern I want, like "google.com" in http://www.google.com/maps using RegEx
        Pattern patternA = Pattern.compile("([^(w{3})(/{2})\\.]+)\\.([^/]+)");
        Matcher matcherA = patternA.matcher(urlA);
        if (matcherA.find())
        {
            siteNameA = matcherA.group();
        }


        String siteNameB = "";
        //search for the pattern I want, like "google.com/" in http://www.google.com/maps
        Pattern patternB = Pattern.compile("([^(w{3})(/{2})\\.]+)\\.([^/]+)");
        Matcher matcherB = patternB.matcher(urlB);
        if (matcherB.find())
        {
            siteNameB = matcherB.group();
        }


        return siteNameA.equals(siteNameB);

    }

}
