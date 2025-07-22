package com.magic.magicwebsocket.server.base;

import android.text.TextUtils;
import android.util.Log;

import com.magic.magicwebsocket.MLog;
import com.magic.magicwebsocket.data.WBIndex;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.HashMap;

public class BaseWebSocketServer extends WebSocketServer {
    private static final String TAG = "Magic-Server";
    private static final int m_mainthread_sleep=60;
    private  boolean m_is_open_websocket=false;
    private HashMap<String,WebSocket> m_hashMap=new HashMap<>();

    public BaseWebSocketServer(int port) {
        super(new InetSocketAddress("127.0.0.1", port));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
//        conn.send("Hello from target app!");
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        // handle close
        MLog.d(TAG,"onClose = "+conn);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            MLog.d(TAG,"onMessage = "+message);
            JSONObject jsonObject =new JSONObject(message);
            String processName = jsonObject.optString("processName","");
            String objProcessName = jsonObject.optString("objProcessName","");
            int index = jsonObject.getInt("index");

            //注册
            if((index==WBIndex.IPC_INDEX_Register)){
                m_hashMap.put(processName,conn);
                jsonObject.put("index", WBIndex.IPC_INDEX_Register);
                jsonObject.put("data","success");
                send(processName,jsonObject.toString());
                return;
            }
            index = index+1;
            jsonObject.put("index",index);
            //没有目标名字就全局发送
            if (TextUtils.isEmpty(objProcessName)){
                send(jsonObject.toString());
            }else{
                if(m_hashMap.containsKey(objProcessName)){
                    send(objProcessName,jsonObject.toString());
                }else{
                    jsonObject.put("index",WBIndex.IPC_INDEX_Obj_Close);
                    conn.send(jsonObject.toString());
                }
            }



        } catch (JSONException e) {
            Log.d(TAG, "onMessage: "+message,e);
        }



    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        MLog.d(TAG, "onError: ",ex);
//        mybroadcast(ex.getMessage());
    }

    @Override
    public void onStart() {
//
        synchronized (this){
            m_is_open_websocket=true;
            MLog.d(TAG, "onStart: "+m_is_open_websocket);
        }

    }

    public void send(String message) {
        // 向所有连接的客户端广播消息
        for (WebSocket client : getConnections()) {
            client.send(message);
        }
    }

    public void send(String registerName,String message) {
        // 向所有连接的客户端广播消息
        WebSocket webSocket = m_hashMap.get(registerName);
        if(webSocket==null||webSocket.isClosed()){
            m_hashMap.remove(registerName);
            return;
        }
        if (webSocket.isOpen()){
            webSocket.send(message);
        }
    }


    public boolean startServer(){
        boolean result = false;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    boolean flag =true;
                    for (int i = 0; i < m_mainthread_sleep; i++) {
                        flag=isPortInUse(9999);
                        MLog.d(TAG,"主线程 -> 端口被占用-等待 = "+i);
                        if(!flag)break;
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    if (flag){
                        Log.d(TAG,"主线程 -> 确认端口被占用");
                        return;
                    }
                    start();
//                    Log.d(TAG, "启动成功");
                }catch (Exception e){
                    Log.d(TAG,"WebSocket -> mystart异常 ：",e);
                    if(e.toString().contains("can only be started once")){
                        try {
                            stop();
                        } catch (InterruptedException ex) {
                            Log.d(TAG,"WebSocket -> mystart异常2 ：",e);
                        }
                    }
                }

            }
        }).start();
        for (int i = 0; i < m_mainthread_sleep+1; i++) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            synchronized (this){
                if (m_is_open_websocket){
                    result=true;
                    break;
                }
            }
        }


        return result;
    }
    public static boolean isPortInUse(int port) {
        try (ServerSocket ignored = new ServerSocket()) {
            ignored.bind(new InetSocketAddress("127.0.0.1", port));
            return false;
        } catch (IOException e) {
            return true;
        }
    }

    public boolean stopServer(){
        final boolean[] result = {false};
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    stop();
                    result[0] = true;
                } catch (InterruptedException e) {
                    Log.d(TAG, "WebSocket -> mystop异常 : ",e);
                }
            }
        }).start();
        return result[0];
    }
}
