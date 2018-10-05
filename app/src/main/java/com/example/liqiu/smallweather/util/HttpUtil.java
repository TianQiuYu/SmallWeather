package com.example.liqiu.smallweather.util;

import okhttp3.OkHttpClient;
import okhttp3.Request;

public class HttpUtil {
    public static void sendOkHttpRequest(String address,okhttp3.Callback callback){
        OkHttpClient client = new OkHttpClient();
        Request build = new Request.Builder().url(address).build();
        client.newCall(build).enqueue(callback);
    }
}
