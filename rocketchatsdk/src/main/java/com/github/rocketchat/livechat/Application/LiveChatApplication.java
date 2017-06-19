package com.github.rocketchat.livechat.Application;

import android.app.Application;

import io.rocketchat.livechat.LiveChatAPI;

/**
 * Created by sachin on 19/6/17.
 */

public class LiveChatApplication extends Application {

    LiveChatAPI liveChatAPI;
    private static String localurl="ws://192.168.43.149:3000/websocket";
    private static String serverurl="wss://livechattest.rocket.chat/websocket";


    @Override
    public void onCreate() {
        super.onCreate();
        liveChatAPI=new LiveChatAPI(serverurl);
    }

    public LiveChatAPI getLiveChatAPI(){
        return liveChatAPI;
    }
}
