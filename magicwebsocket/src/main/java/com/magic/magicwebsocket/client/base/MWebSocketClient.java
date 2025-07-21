package com.magic.magicwebsocket.client.base;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.magic.magicwebsocket.MLog;
import com.magic.magicwebsocket.data.WBIndex;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class MWebSocketClient  {
    public static final MWebSocketClient[] webSocketClient ={
            new MWebSocketClient(),
            new MWebSocketClient(),
            new MWebSocketClient(),
            new MWebSocketClient(),
            new MWebSocketClient()
    };
    public static int webSocketClient_index=0;

    private OkHttpClient client;
    private WebSocket webSocket;
    private String serverUrl = "ws://127.0.0.1:"; // 替换为你自己的服务器地址
    public Boolean is_connect=Boolean.FALSE;
    private Throwable throwable=null;
    public final HashMap<String,TargetCallback> m_accept_arr =new HashMap<>();
    public final HashMap<String,String> m_send_arr =new HashMap<>();

    private static final String TAG = "Magic-MyClient";
    /*
       作用: 作为接收者:添加需要处理的事务
       参数1: 添加的事务的名称
       参数2: 接收到其他进程的请求的进行回调处理
    */
    public void addHandlerForTarget(String target,TargetCallback targetCallback){
        if(targetCallback ==null)return;
        m_accept_arr.put(target,targetCallback);
    }
    //删除事务
    public void removeHandlerForTarget(String target){
        m_accept_arr.remove(target);
    }

    //发送者异步回调添加
    public void addSend(String sessionId){
        m_send_arr.put(sessionId,"");
    }
    public void removeSend(String sessionId){
        m_send_arr.remove(sessionId);
    }


//    public static WebSocketClient getInstance(){
////        if (webSocketClient==null){
////            webSocketClient = new WebSocketClient();
////        }
//        return webSocketClient;
//    }

    public void connect(int port) {
        client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("ws://127.0.0.1:"+port)
                .build();

        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
                super.onClosed(webSocket, code, reason);
            }

            @Override
            public void onClosing(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
                super.onClosing(webSocket, code, reason);
                webSocket.close(1000, null);
                synchronized (this){
                    is_connect=Boolean.FALSE;
                }

            }

            @Override
            public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, @Nullable Response response) {
                super.onFailure(webSocket, t, response);
                synchronized (this){
                    is_connect=Boolean.FALSE;
                    throwable=t;
//        webSocketClient.close();
                    Log.d(TAG, "WebSocket Error", t);
                }

            }

            @Override
            public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
                super.onMessage(webSocket, text);
                MLog.d(TAG, "onMessage: "+text);
                if (TextUtils.isEmpty(text)) {
                    Log.e(TAG, "Received message: 内容为Null" );
                    return;
                }

                try {
                    JSONObject jsonObject = new JSONObject(text);
                    String pkg = jsonObject.getString("pkg");
                    String processName = jsonObject.getString("processName");
                    String target = jsonObject.getString("target");
                    String data = jsonObject.getString("data");
                    int index = jsonObject.getInt("index");
                    String sessionId = jsonObject.getString("sessionId");
                    //作为发送者:需要处理4
                    if (index == WBIndex.IPC_INDEX_SERVER_TO_A){
                        synchronized (m_send_arr){
                            if(!m_send_arr.containsKey(sessionId))return;
                            m_send_arr.put(sessionId,data);
//                    callback.onResponse(data);
                        }

                    }
                    //作为接收者:需要处理2
                    else if(index == WBIndex.IPC_INDEX_SERVER_TO_B){
                        if(!m_accept_arr.containsKey(target))return;
                        TargetCallback targetCallback = m_accept_arr.get(target);
                        if (targetCallback==null)return;
                        String s = targetCallback.onRequest(target, pkg, processName, data);
                        jsonObject.put("data",s);
                        jsonObject.put("index",WBIndex.IPC_INDEX_B_TO_SERVER);
                        sendMessage(jsonObject.toString());
                    }
                    //处理 注册
                    else if(index == WBIndex.IPC_INDEX_Register){
                        synchronized (m_send_arr){
                            if(!m_send_arr.containsKey(sessionId))return;
                            m_send_arr.put(sessionId,"success");
//                    callback.onResponse(data);
                        }
                    }
                    //处理：目标已经断开连接
                    else if(index == WBIndex.IPC_INDEX_Obj_Close){
                        synchronized (m_send_arr){
                            if(!m_send_arr.containsKey(sessionId))return;
                            m_send_arr.put(sessionId,"disconnect");
                        }
                    }

                } catch (JSONException e) {
                    Log.e(TAG, "onMessage Error", e);
                }
            }

            @Override
            public void onMessage(@NonNull WebSocket webSocket, @NonNull ByteString bytes) {
                super.onMessage(webSocket, bytes);
            }

            @Override
            public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
                super.onOpen(webSocket, response);
                MLog.d(TAG, "WebSocket Connected");
                synchronized (this){
                    is_connect=Boolean.TRUE;
                }

//        webSocket.send("Hello from Android!");

            }
        });

        // client.dispatcher().executorService().shutdown(); // 如果你想关闭线程池，注意别太早调用
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public boolean isconnect(){
        synchronized (this){
            return is_connect;
        }
//        return is_connect;
    }


    public void sendMessage(String message) {
        if (webSocket != null) {
            webSocket.send(message);
        }
    }

    public void close() {
        if (webSocket != null) {
            webSocket.close(1000, "Closing normally");
        }
    }
}
