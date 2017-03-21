package com.wordpress.httpspandareaktor.quant;

import java.util.HashMap;

/**
 * Created by brian on 3/20/17.
 */

public class Abathur {

    public static HashMap<String, Integer> findFrequency(String str){
        HashMap<String, Integer> map = new HashMap<>();
        String[] wordsArray = str.split("\\s+");
        for (String word : wordsArray){
            String currentWord = word.toString();
            if (map.containsKey(currentWord)){
                map.put(word, map.get(word) + 1);
            } else {
                map.put(word, 1);
            }
        }

        return map;
    }
}
