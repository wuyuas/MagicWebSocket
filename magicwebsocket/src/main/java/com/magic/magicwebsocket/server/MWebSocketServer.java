package com.magic.magicwebsocket.server;

import android.util.Log;


//
//import com.magic.magicwebsocket.java_websocket.WebSocket;
//import com.magic.magicwebsocket.java_websocket.handshake.ClientHandshake;
//import com.magic.magicwebsocket.java_websocket.server.WebSocketServer;
import com.magic.magicwebsocket.server.base.BaseWebSocketServer;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

public class MWebSocketServer {
    private static BaseWebSocketServer baseWebSocketServer =null;
    public MWebSocketServer(int port){
        if(baseWebSocketServer==null){
            baseWebSocketServer=new BaseWebSocketServer(port);
        }
    }

    public boolean startServer(){
        return baseWebSocketServer.startServer();
    }

    public void stopServer(){
        baseWebSocketServer.stopServer();
    }



}
