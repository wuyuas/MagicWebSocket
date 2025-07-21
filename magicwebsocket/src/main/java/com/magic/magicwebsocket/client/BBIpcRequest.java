package com.magic.magicwebsocket.client;

import static com.magic.magicwebsocket.client.base.MWebSocketClient.webSocketClient_index;

import android.text.TextUtils;
import android.util.Log;

import com.magic.magicwebsocket.MLog;
import com.magic.magicwebsocket.client.base.Callback;
import com.magic.magicwebsocket.client.base.TargetCallback;
import com.magic.magicwebsocket.client.base.MWebSocketClient;
import com.magic.magicwebsocket.data.WBIndex;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.UUID;

public class BBIpcRequest {
    private static final String TAG = "Magic-Client";
    public String m_pkg;
    public String m_processName;
    //表示某个事务
    public String m_target;
    //某个事务下->某个任务的唯一表示
    private  String m_sessionId= UUID.randomUUID().toString();
    public String m_data;
    public int m_index = WBIndex.IPC_INDEX_A_TO_SERVER;
//    private WebSocketClient m_webSocketClient =WebSocketClient.getInstance();
    public int m_port = 0;
    private String m_registerName="";

    public BBIpcRequest(String pkg,String processName,int port){
        m_pkg=pkg;
        m_processName=processName;
        m_port = port;
        webSocketClient_index=0;
        synchronized (MWebSocketClient.webSocketClient){
            if(!MWebSocketClient.webSocketClient[webSocketClient_index].isconnect()){
                MWebSocketClient.webSocketClient[webSocketClient_index].connect(m_port);
                for (int i = 0; i < 300; i++) {
                    try {
                        Thread.sleep(10);
                        if(MWebSocketClient.webSocketClient[webSocketClient_index].isconnect()){
                            break;
                        }
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }

                }
            }
        }
    }

    public BBIpcRequest(String pkg,String processName,int port,int index){
        if(index>=MWebSocketClient.webSocketClient.length){
            throw new RuntimeException("不支持这么多客户端");
        }
        if(index<0){
            index=0;
        }
        m_pkg=pkg;
        m_processName=processName;
        m_port = port;
        webSocketClient_index=index;
        synchronized (MWebSocketClient.webSocketClient){
            if(!MWebSocketClient.webSocketClient[webSocketClient_index].isconnect()){
                MWebSocketClient.webSocketClient[webSocketClient_index].connect(m_port);
                for (int i = 0; i < 300; i++) {
                    try {
                        Thread.sleep(10);
                        if(MWebSocketClient.webSocketClient[webSocketClient_index].isconnect()){
                            break;
                        }
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }

                }
            }
        }
    }
    public boolean register(String registerName){
        final Boolean[] result = {null};
//        m_registerName=registerName;
        sendTarget(registerName,"", registerName, 30,WBIndex.IPC_INDEX_Register, new Callback() {
            @Override
            public void onFailure(IOException ioException) {
                super.onFailure(ioException);
                result[0] =false;
            }

            @Override
            public void onResponse(String Body) {
                super.onResponse(Body);
                synchronized (result){
                    result[0] =true;
                }

            }
        });
        MLog.d(TAG, "register状态: "+result[0]);
        return result[0];
    }
    public String execute(String target,
                          String data,
                          int timeout) throws IOException {
        return execute("",target,data,timeout);
    }

    public String execute(String registerName,
                         String target,
                          String data,
                          int timeout) throws IOException {
        final String[] result = {null};
        final IOException[] myioException = {null};
        sendTarget(registerName,target, data, timeout,WBIndex.IPC_INDEX_A_TO_SERVER, new Callback() {
            @Override
            public void onFailure(IOException ioException) {
                super.onFailure(ioException);
                myioException[0] =ioException;
            }

            @Override
            public void onResponse(String Body) {
                super.onResponse(Body);
                synchronized (result){
                    result[0] =Body;
                }

            }
        });
        if(myioException[0]!=null){
            throw myioException[0];
        }
        return result[0];
    }


    //作为发送者:发送数据的方法
    public void enqueue(String target,
                        String data,
                        int timeout,
                        Callback callback) throws IOException {
        enqueue("",target,data,timeout,callback);
    }
    public void enqueue(String registerName,
                        String target,
                           String data,
                           int timeout,
                           Callback callback) throws IOException {
        synchronized (MWebSocketClient.webSocketClient){
            if(!MWebSocketClient.webSocketClient[webSocketClient_index].isconnect()){
                throw new IOException(new Throwable());
            }
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                sendTarget(registerName,target,data,timeout,WBIndex.IPC_INDEX_A_TO_SERVER,callback);
            }
        }).start();

    }

    private void sendTarget(String registerName,
                            String target,
                            String data,
                            int timeout,
                            int index,
                            Callback callback){
        JSONObject jsonObject =new JSONObject();
        try {
            m_sessionId= UUID.randomUUID().toString();
            //组装参数
            jsonObject.put("pkg",m_pkg);
            jsonObject.put("processName",m_processName);
            jsonObject.put("data",data);
            jsonObject.put("target",target);
            jsonObject.put("index",index);
            jsonObject.put("sessionId",m_sessionId);
            jsonObject.put("registerName",registerName);
            MWebSocketClient.webSocketClient[webSocketClient_index].addSend(m_sessionId);
//            MWebSocketClient.webSocketClient.connect(m_port);
            //判断连接
            synchronized (MWebSocketClient.webSocketClient){

                if(!MWebSocketClient.webSocketClient[webSocketClient_index].isconnect()){
                  callback.onFailure(new IOException("sendTarget->连接服务端失败"));
                    MWebSocketClient.webSocketClient[webSocketClient_index].removeSend(m_sessionId);
                  return;
                }
                MWebSocketClient.webSocketClient[webSocketClient_index].sendMessage(jsonObject.toString());
            }
            //等待结果
            if(timeout>0){
                int count = timeout*100;
                int i = 0;
                for ( i = 0; i < count; i++) {
                    Thread.sleep(10);
                    synchronized (MWebSocketClient.webSocketClient[webSocketClient_index].m_send_arr){
                        String s = MWebSocketClient.webSocketClient[webSocketClient_index].m_send_arr.get(m_sessionId);
                        if (!TextUtils.isEmpty(s)){
                            callback.onResponse(s);
                            MWebSocketClient.webSocketClient[webSocketClient_index].removeSend(m_sessionId);
                            return;
                        }
                    }
                }
                //发送超时异常
                callback.onFailure(new IOException("超时了："+i));
                MWebSocketClient.webSocketClient[webSocketClient_index].removeSend(m_sessionId);
            }

        } catch (JSONException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }


    /*
        作用: 作为接收者:添加需要处理的事务
        参数1: 添加的事务的名称
        参数2: 接收到其他进程的请求的进行回调处理
     */
    public void addHandlerForTarget(String target, TargetCallback targetCallback){
        synchronized (MWebSocketClient.webSocketClient){
            MWebSocketClient.webSocketClient[webSocketClient_index].addHandlerForTarget(target,targetCallback);
        }

    }
    //删除事务
    public void removeHandlerForTarget(String target){
        synchronized (MWebSocketClient.webSocketClient){
            MWebSocketClient.webSocketClient[webSocketClient_index].removeHandlerForTarget(target);
        }
    }











}
