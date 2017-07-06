package com.rocketchat.sdkdemo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.github.rocketchat.livechat.ChatActivity;

public class MainActivity extends AppCompatActivity {

    Button support;
    EditText domainName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        support= (Button) findViewById(R.id.support);
        domainName=(EditText)findViewById(R.id.url);
        support.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MyApplication myApplication= (MyApplication) getApplicationContext();

                if (domainName.getText().toString().equals("")){
                    myApplication.setServerUrl(MyApplication.serverurl);
                }else{
                    myApplication.setServerUrl(domainName.getText().toString()+"/websocket");
                }
                Intent intent=new Intent(MainActivity.this,ChatActivity.class);
                startActivity(intent);

            }
        });
    }
}
