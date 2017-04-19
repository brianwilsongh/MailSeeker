package com.wordpress.httpspandareaktor.quant;

import java.util.List;

/**
 * Created by brian on 3/20/17.
 */

public interface FetchCallback {

    void onFinish(String s);

    void onUpdate(List<Object> list);
    //string array coding:
    //[0] is text for the loading bar, say what is happening
    //[1] is an array of strings containing emails pulled out



}
