package com.wordpress.httpspandareaktor.quant;

import android.util.Log;

import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by brian on 3/20/17.
 */

public class RegexUtils {


    public static String cleanText(String input, boolean filterMonth, boolean filterDay, boolean filterCommon){
        //booleans are filters that take month, day, very common words like "a" and "the"
        //split input into array to clean using delimiter of unlimited whitespace to capture everything
        String[] dirtyWordArray = input.split("\\s+");
        StringBuilder cleanString = new StringBuilder();

        //use enhanced for loop to clean out non-alpha
        //include words with comma, period, exclaimation, apostrophe at start/fin, etc...
        for (String word : dirtyWordArray) {
            if (word.matches("[a-z[A-Z]]+") && !word.matches("null")){
                if (passesFilter(word.toLowerCase(), filterMonth, filterDay, filterCommon)) {
                    cleanString.append(word + " ");
                }


            } else if ((word.matches("[a-z[A-Z]]+\\!?") || word.matches("[a-zA-Z]+\\.?") || word.matches("[a-zA-Z]+\\??"))
                    && !word.matches("null")){
                if (passesFilter(word.toLowerCase(), filterMonth, filterDay, filterCommon)) {
                    cleanString.append(word.substring(0, word.length() - 1) + " ");
                }
            } else {
                cleanString.append("");
            }
        }
        Log.v("RegexUtils.cleanText", "cleanString is: " + cleanString);
        return cleanString.toString();

    }


    private static boolean passesFilter(String word, boolean filterMonth, boolean filterDay, boolean filterCommon){
        String[] monthStrings = new String[]{"january", "february", "march", "april", "may", "june", "july",
                "august", "september", "october", "november", "december", "jan", "feb", "mar", "apr", "may", "jun",
        "jul", "aug", "sep", "oct", "nov", "dec"};

        String[] dayStrings = new String[]{"monday", "tuesday", "wednesday", "thursday", "friday"};

        String[] commonStrings = new String[]{"the", "be", "to", "of", "and", "a", "in", "that", "is",
                "have", "i", "it", "its", "for", "not", "on", "with", "he", "she", "his", "her", "as", "you", "do", "at",
                "or", "a", "an", "will", "their", "there", "by", "comment", "comments", "date",
        "how", "from", "et", "more", "are", "your", "am", "pm", "site", "why", "where", "our", "this",
                "about", "us", "if", "about", "find", "but", "out", "we", "all", "after", "before",
        "say", "says", "new", "what", "over", "lol", "just", "being", "was", "has", "still", "who", "into",
         "me", "they", "go", "hi", "can", "my", "welcome", "something", "it", "there", "around", "used", "something",
        "some", "around", "so", "up", "every", "them", "same", "need", "such", "also", "were", "which", "between", "than",
         "when", "through", "could", "other", "made", "been", "very", "would", "since", "thus", "later", "much",
         "another", "although", "while", "usually", "make", "only", "good", "get", "even", "now"};

        if (Arrays.asList(monthStrings).contains(word)){
            return false;
        }

        if (Arrays.asList(dayStrings).contains(word)){
            return false;
        }

        if (Arrays.asList(commonStrings).contains(word)){
            return false;
        }
        return true;

    }


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



        //now extract substring from strings, domain name, to take care of www or not www in URLs
        String siteNameA = "";
        //search for the pattern I want, like "google.com" in http://www.google.com/maps using RegEx
        Pattern patternA = Pattern.compile("([^\\.]+)\\.(co.)?([^\\.]+)$");
        Matcher matcherA = patternA.matcher(hostA);
        if (matcherA.find())
        {
            siteNameA = matcherA.group();
        }

        String siteNameB = "";
        //search for the pattern I want, like "google.com/" in http://www.google.com/maps
        Pattern patternB = Pattern.compile("([^\\.]+)\\.(co.)?([^\\.]+)$");
        Matcher matcherB = patternB.matcher(hostB);
        if (matcherB.find())
        {
            siteNameB = matcherB.group();
        }

        return siteNameA.equals(siteNameB);

    }




}
