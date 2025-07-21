package com.magic.magicwebsocket.temp;



import android.util.Log;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class MyClient {
    private static final String TAG = "Magic-MyClient";
    public void connect() {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("ws://127.0.0.1:9999") // 与目标 App 中注入的服务端端口一致
                .build();

        client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, okhttp3.Response response) {

//                webSocket.send("ping");
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                Log.d(TAG,"Received: " + text);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, okhttp3.Response response) {
                Log.d(TAG,"Response = ",t);
                t.printStackTrace();
            }
        });
    }
}
