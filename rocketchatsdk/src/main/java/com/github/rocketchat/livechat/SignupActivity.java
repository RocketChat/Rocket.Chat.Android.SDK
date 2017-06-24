package com.github.rocketchat.livechat;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.github.rocketchat.R;
import com.github.rocketchat.livechat.Application.LiveChatApplication;
import com.github.rocketchat.utils.AppUtils;

import io.rocketchat.livechat.LiveChatAPI;
import io.rocketchat.livechat.callback.AuthListener;
import io.rocketchat.livechat.callback.ConnectListener;
import io.rocketchat.livechat.model.GuestObject;


// TODO: 24/6/17 Reconnection to server manually via snackbar

public class SignupActivity extends AppCompatActivity implements ConnectListener, AuthListener.RegisterListener, AuthListener.LoginListener {

    EditText username,email;
    LiveChatAPI api;
    Boolean isconnected=false;
    ProgressDialog dialog;
    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        getSupportActionBar().setTitle("LiveChat Registration");
        api=((LiveChatApplication)getApplicationContext()).getLiveChatAPI();
        api.setReconnectionStrategy(null);
        api.connect(this);

        username= (EditText) findViewById(R.id.userid);
        email= (EditText) findViewById(R.id.email);
        Button register= (Button) findViewById(R.id.register);

        dialog=new ProgressDialog(this);
        dialog.setIndeterminate(true);
        dialog.setMessage("Registering ...");

        SharedPreferences sharedPref=getPreferences(MODE_PRIVATE);
        editor = sharedPref.edit();

        String Username=sharedPref.getString("username",null);
        String Email=sharedPref.getString("email",null);

        if (Username!=null){
            username.setText(Username);
            email.setText(Email);
        }

        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String username=SignupActivity.this.username.getText().toString();
                String email=SignupActivity.this.email.getText().toString();

                if (username.isEmpty() || email.isEmpty()){
                    AppUtils.showToast(SignupActivity.this,"username and email shouldn't be null",true);
                }else{
                    if (isconnected){
                        dialog.show();
                        api.registerGuest(username,email,null,SignupActivity.this);

                    }else{
                        AppUtils.showToast(SignupActivity.this,"Not connected to server",true);
                    }

                }
            }
        });

    }



    @Override
    public void onConnect(String sessionID) {
        isconnected=true;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
//                AppUtils.showToast(SignupActivity.this,"Connected to server",true);
                Snackbar
                        .make(findViewById(R.id.activity_signup), R.string.connected, Snackbar.LENGTH_LONG)
                        .show();
            }
        });
    }

    @Override
    public void onDisconnect(boolean closedByServer) {
        isconnected=false;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
//                AppUtils.showToast(SignupActivity.this,"Disconnected from server",true);
                AppUtils.getSnackbar(findViewById(R.id.activity_signup),R.string.disconnected_from_server)
                        .setAction("RETRY", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                api.reconnect();
                            }
                        })
                        .show();
            }
        });
    }

    @Override
    public void onConnectError(Exception websocketException) {
        isconnected=false;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
//                AppUtils.showToast(SignupActivity.this,"Connection error",true);
                AppUtils.getSnackbar(findViewById(R.id.activity_signup),R.string.connection_error)
                        .setAction("RETRY", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                api.reconnect();

                            }
                        })
                        .show();
            }
        });
    }

    @Override
    public void onRegister(GuestObject object) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AppUtils.showToast(SignupActivity.this,"Registration successful",false);
                dialog.dismiss();
            }
        });
        System.out.println("Registration success");
        api.login(object.getToken(),this);
    }

    @Override
    public void onLogin(GuestObject object) {
        System.out.println("login success");
        editor.putString("username",username.getText().toString());
        editor.putString("email",email.getText().toString());
        editor.commit();

        LiveChatAPI.ChatRoom room=api.createRoom(object.getUserID(),object.getToken());
        Intent intent=new Intent();
        intent.putExtra("roomInfo",room.toString());
        setResult(RESULT_OK,intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        finish();
    }
}
