package com.example.rocketchat;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.github.rocketchat.livechat.ChatActivity;
import com.github.rocketchat.livechat.SignupActivity;

public class MainActivity extends AppCompatActivity {

    Button support;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        support= (Button) findViewById(R.id.support);
        support.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(MainActivity.this,ChatActivity.class);
                startActivity(intent);
            }
        });

    }
}
