Rocket.Chat.Android.SDK
----------------------

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
This SDK currently provides support to include <b> livechat client functionality </b> in any android app.


License
-------
MIT License

Gradle
------
Add below snippet in build.gradle file. 

```Gradle
dependencies {
    compile 'com.github.rocketchat:rocketchatsdk:0.1.1'
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

```java
     support.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(getApplicationContext(),ChatActivity.class);
                startActivity(intent);
            }
        });
```
<b> Note : </b> This library currently do not support configuration having any departments at server side. Make sure no department is created or available at server side LiveChat configuartion.

<b>For more information refer sample code under 'app' directory. </b>



