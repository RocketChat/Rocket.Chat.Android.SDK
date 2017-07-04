package com.example.rocketchat;

import com.github.rocketchat.livechat.Application.LiveChatApplication;

/**
 * Created by sachin on 24/6/17.
 */

public class MyApplication extends LiveChatApplication {

    public static String serverurl="wss://livechattest.rocket.chat/websocket";

    @Override
    public void onCreate() {
        super.onCreate();
    }

}
