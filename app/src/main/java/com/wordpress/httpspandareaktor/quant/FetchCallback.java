package com.wordpress.httpspandareaktor.quant;

/**
 * Created by brian on 3/20/17.
 */

public interface FetchCallback {

    void onFinish(String s);

    void onUpdate(String[] s);
    //string array coding:
    //[0] is text for the loading bar, say what is happening
    //[1] is the newest raw HTML
    //[2] is most recent link

}
