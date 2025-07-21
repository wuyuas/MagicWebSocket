package com.magic.magicwebsocket.temp;



import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;

public class WebSocketClientManager {
    private static final String TAG = "Magic-WebSocketClientManager";
    private WebSocketClient client;
    public WebSocketClientManager() {
        try {
            // 连接到本地WebSocket服务器
            client = new WebSocketClient(new URI("http://127.0.0.1:8080")) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    // 连接成功时调用
                    Log.d(TAG,"客户端连接成功==========");
                }

                @Override
                public void onMessage(String message) {
                    // 接收到消息时调用
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    // 连接关闭时调用
                    Log.d(TAG,"onClose -> "+reason);
                }

                @Override
                public void onError(Exception ex) {
                    // 发生错误时调用
                    Log.d(TAG,"onError -> "+ex.toString());
                }
            };
            client.connect();
        } catch (URISyntaxException e) {
            Log.d(TAG,e.toString());
            e.printStackTrace();
        }
    }

    public void sendMessage(String message) {
        // 发送消息到服务器
        if (client != null && client.isOpen()) {
            client.send(message);
        }
    }

    public void closeConnection() {
        // 关闭连接
        if (client != null) {
            client.close();
        }
    }
}
