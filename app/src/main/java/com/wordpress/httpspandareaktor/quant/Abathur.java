package com.wordpress.httpspandareaktor.quant;

import android.util.Log;
import android.view.View;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Created by brian on 3/20/17.
 */

public final class Abathur {

private static int wordCount = 0;

    public static String findFrequency(String str){
        HashMap<String, Integer> map = new HashMap<>();
        String[] wordsArray = str.split("\\s+");

        for (String word : wordsArray){
            //it's a word, thus increment the total word counter
            wordCount ++;
            String stringLowerCase = word.toLowerCase();
            if (map.containsKey(stringLowerCase)){
                int count = map.get(stringLowerCase) + 1;
                map.remove(stringLowerCase);
                map.put(stringLowerCase, count);
            } else {
                map.put(stringLowerCase, 1);
            }
        }

        Log.v("Abathur", ": map before sort is" + map.toString());

        return sortMapToString(map);
    }


    private static String sortMapToString(HashMap<String, Integer> unsortedMap){

        //start with List
        List<HashMap.Entry<String, Integer>> entryList = new ArrayList<>(unsortedMap.entrySet());
        Log.v("Abathur.sortMap", " made list " + entryList.toString());

        //sort the list with Collections and a comaprator
        // compares keys reverse the normal order since we want descending
        Collections.sort(entryList, new Comparator<HashMap.Entry<String, Integer>>() {
            @Override
            public int compare(HashMap.Entry<String, Integer> o1, HashMap.Entry<String, Integer> o2) {
                return o2.getValue().compareTo(o1.getValue());
            }
        });

        StringBuilder returner = new StringBuilder();

        for (HashMap.Entry<String, Integer> entry : entryList) {
            //get the word
            returner.append(entry.getKey());
            returner.append(" ");
            //get the number of times the word appears
            returner.append(String.format("(count: %s)", entry.getValue()));
            double statFreq  = (double) (entry.getValue() * 100) / wordCount;
            //truncate the decimal by formatting it, rounding to ceiling for last digit
            DecimalFormat decimalFormat = new DecimalFormat("##.###");
            decimalFormat.setRoundingMode(RoundingMode.CEILING);
            returner.append(String.format(" (frequency: %s", decimalFormat.format(statFreq)));
            returner.append("%)");
            returner.append("\n");
        }
        Log.v("Abathur.sortMap", " made string " + returner.toString());
        return returner.toString();
        }

}
