package com.magic.magicwebsocket;

import android.util.Log;

public class MLog {
    public static boolean is_log=true;
    public static void d(String Tag,String obj){
        if(!is_log)return;
        Log.d(Tag,obj);
    }
    public static void d(String Tag,String obj,Throwable throwable){
        if(!is_log)return;
        Log.d(Tag,obj,throwable);
    }
}
