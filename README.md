# DEPRECATED
----------------------

**This is no longer supported, please consider using the [Rocket.Chat.Kotlin.SDK](https://github.com/RocketChat/Rocket.Chat.Kotlin.SDK) instead**.

<!--- Here are some features that SDK should provide
- Authentication in integration with native app username and password
- Sending and receiving messages, audio, video , document, images
- Listing out all subscribed channels and their types
- Local history of messages (this one needs to be determined)
- I think it would be more interesting if we provide pluggable UI componentz
- User can enable and disable the feature depending on his choice
- The user interface should have facility to synchronize with app theme, so that it should look as a part of native app
Feel free to add or edit the features :)
--->

This SDK is divided into two parts
1. Core SDK
2. LiveChat SDK

This SDK currently provides support to include <b> livechat client functionality </b> in any android app. </br>
**Core SDK** coming soon.....

<a href='https://play.google.com/store/apps/details?id=com.rocketchat.sdkdemo&pcampaignid=MKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1'><img alt='Get it on Google Play' width="200" src='https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png'/></a>

Demo
--------
<img src="https://github.com/RocketChat/Rocket.Chat.Android.SDK/blob/develop/recording/demo.gif" align="center" alt="LiveChat" width="320px" height="560px"/>

License
-------
MIT License

Gradle
------
Add below snippet in build.gradle file. 

```Gradle
dependencies {
    compile 'com.github.rocketchat:rocketchatsdk:0.2.0'
}
```

[ ![Download](https://api.bintray.com/packages/rocketchat/RocketChat-SDK/RocketChat-Android-SDK/images/download.svg) ](https://bintray.com/rocketchat/RocketChat-SDK/RocketChat-Android-SDK/_latestVersion)

How to use
-------------
There are two major parts required to be followed

#### 1. Set up Application class
- Create Custom Application class that extends LiveChatApplication class in the library. Add name of this custom class in the manifest file. Inside onCreate method call setServerUrl(url) passing url of the server as a parameter.

Example :
1. Creating custom Application class as "MyApplication"

```java
    public class MyApplication extends LiveChatApplication {

    public static String serverurl="wss://livechattest.rocket.chat/websocket";

    @Override
    public void onCreate() {
        setServerUrl(serverurl);
        super.onCreate();
    }
}
     
```
2. Declaring class name in manifest file
```
    <application
            android:name=".MyApplication" \\ can change depending on package location
            android:allowBackup="true"
            android:icon="@mipmap/ic_launcher"
            .........
            ........
```


#### 2. Open ChatActivity
- Clicking on required view will start new activity by passing ChatActivity as a parameter to the startActivity intent.

Example:<br>
support is a button (required view). Clicking on button will fire intent to open ChatActivity.

```
     support.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(getApplicationContext(),ChatActivity.class);
                startActivity(intent);
            }
        });
```

<b>For more information refer sample code under 'example' directory. </b>



