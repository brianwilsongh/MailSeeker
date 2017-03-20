package com.wordpress.httpspandareaktor.quant;

import android.util.Log;

/**
 * Created by brian on 3/20/17.
 */

public class RegexUtils {


    public static String cleanText(String input){
        //split input into array to clean using delimiter of unlimited whitespace to capture everything
        String[] dirtyWordArray = input.split("\\s+");
        Log.v("RegexUtils.cleanText", "dirtywordarray pos 1 is " + dirtyWordArray[0]);
        StringBuilder cleanString = new StringBuilder();

        //use enhanced for loop to clean out non-alpha
        //include words with comma, period, exclaimation, apostrophe at start/fin, etc...
        for (String word : dirtyWordArray) {
            if (word.matches("[a-z[A-Z]]+")){
                cleanString.append(word + " ");
            } else if (word.matches("[a-z[A-Z]]+\\!?") || word.matches("[a-zA-Z]+\\.?") || word.matches("[a-zA-Z]+\\??")){
                cleanString.append(word.substring(0, word.length() - 1) + " ");
            }
        }
        Log.v("RegexUtils.cleanText", "cleanString is: " + cleanString);
        return cleanString.toString();


    }
}
