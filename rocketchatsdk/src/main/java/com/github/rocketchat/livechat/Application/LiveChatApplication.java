package com.github.rocketchat.livechat.Application;

import android.app.Application;
import io.rocketchat.livechat.LiveChatAPI;

/**
 * Created by sachin on 19/6/17.
 */

public class LiveChatApplication extends Application {

    LiveChatAPI liveChatAPI;
//    private String localurl="ws://192.168.43.149:3000/websocket";
//    private String serverurl="wss://livechattest.rocket.chat/websocket";

    private String url;

    public LiveChatAPI getLiveChatAPI(){
        return liveChatAPI;
    }

    public String getServerUrl() {
        return url;
    }

    public void setServerUrl(String url) {
        this.url = url;
        liveChatAPI=new LiveChatAPI(url);
    }
}
