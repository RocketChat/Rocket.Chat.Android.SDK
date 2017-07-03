package com.github.rocketchat.livechat;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.UiThread;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.github.rocketchat.R;
import com.github.rocketchat.livechat.Application.LiveChatApplication;
import com.github.rocketchat.model.Department;
import com.github.rocketchat.utils.AppUtils;

import java.util.ArrayList;

import io.rocketchat.common.data.model.ErrorObject;
import io.rocketchat.livechat.LiveChatAPI;
import io.rocketchat.livechat.callback.AuthListener;
import io.rocketchat.livechat.callback.ConnectListener;
import io.rocketchat.livechat.callback.InitialDataListener;
import io.rocketchat.livechat.model.GuestObject;
import io.rocketchat.livechat.model.LiveChatConfigObject;


public class SignupActivity extends AppCompatActivity implements ConnectListener, AuthListener.RegisterListener, AuthListener.LoginListener, InitialDataListener {

    EditText username,email;
    Button register;

    //Offline form
    TextView default_message,success_message;
    EditText message;


    LiveChatAPI api;
    ProgressDialog dialog;
    private SharedPreferences.Editor editor;
    Spinner departments;

    Boolean isconnected=false;
    Boolean isOfflineForm=false;

    LiveChatConfigObject chatConfigObject;
    private String selectedDeptId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        getSupportActionBar().setTitle("LiveChat Registration");
        api =((LiveChatApplication)getApplicationContext()).getLiveChatAPI();
        api.setReconnectionStrategy(null);
        api.connect(this);

        username= (EditText) findViewById(R.id.userid);
        email= (EditText) findViewById(R.id.email);
        default_message= (TextView) findViewById(R.id.offline_message);
        message= (EditText) findViewById(R.id.message);
        success_message= (TextView) findViewById(R.id.success_message);
        departments= (Spinner) findViewById(R.id.departments);

        register= (Button) findViewById(R.id.register);

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
                        if (isOfflineForm){
                            String message=SignupActivity.this.message.getText().toString();
                            api.sendOfflineMessage(username,email,message);
                            showSuccessMessage(chatConfigObject.getOfflineSuccessMessage());
                        }else {
                            dialog.show();
                            api.registerGuest(username, email, selectedDeptId , SignupActivity.this);
                        }

                    }else{
                        AppUtils.showToast(SignupActivity.this,"Not connected to server",true);
                    }

                }
            }
        });

    }



    @Override
    public void onConnect(String sessionID) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
//                AppUtils.showToast(SignupActivity.this,"Connected to server",true);
                Snackbar
                        .make(findViewById(R.id.activity_signup), R.string.connected, Snackbar.LENGTH_LONG)
                        .show();
            }
        });
        api.getInitialData(this);

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
    public void onInitialData(LiveChatConfigObject object, ErrorObject error) {
        if (error!=null){

        }else{
            isconnected=true;
            chatConfigObject=object;
            if (chatConfigObject.getEnabled()) {
                if (chatConfigObject.getOnline()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setUpRegistrationForm(chatConfigObject.getPopupTitle(), Department.getDepartments(chatConfigObject.getDepartments()));
                        }
                    });
                } else {
                    if (chatConfigObject.getDisplayOfflineFOrm()) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setUpOfflineForm(chatConfigObject.getOfflineTitle(), chatConfigObject.getOfflineMessage());
                            }
                        });
                    }else{
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showSuccessMessage(chatConfigObject.getOfflineUnavailableMessage());
                            }
                        });
                    }
                }
            }else{
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showSuccessMessage(R.string.livechat_enabled);
                    }
                });
            }
        }
    }

    @Override
    public void onRegister(GuestObject object, final ErrorObject error) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (error!=null){
                    AppUtils.showToast(SignupActivity.this, error.getMessage() , false);
                    dialog.dismiss();
                }else {
                    AppUtils.showToast(SignupActivity.this, "Registration successful", false);
                    dialog.dismiss();
                }
            }
        });
        if (error==null) {
            api.login(object.getToken(), this);
        }
    }

    @Override
    public void onLogin(GuestObject object,ErrorObject error) {
        if (error==null) {
            System.out.println("login success");
            editor.putString("username", username.getText().toString());
            editor.putString("email", email.getText().toString());
            editor.commit();

            LiveChatAPI.ChatRoom room = api.createRoom(object.getUserID(), object.getToken());
            Intent intent = new Intent();
            intent.putExtra("roomInfo", room.toString());
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        finish();
    }

    public void setUpOfflineForm(String offlineTitle,String defaultMessage){
        isOfflineForm=true;
        default_message.setVisibility(View.VISIBLE);
        message.setVisibility(View.VISIBLE);
        success_message.setVisibility(View.GONE);
        username.setHint(R.string.type_name);
        email.setHint(R.string.type_email);
        departments.setVisibility(View.GONE);
        register.setText(R.string.button_send);
        getSupportActionBar().setTitle(offlineTitle);

        if (!defaultMessage.equals("")){
            default_message.setText(defaultMessage);
        }
    }


    public void setUpRegistrationForm(String title,final ArrayList <Department> showDepartments){
        isOfflineForm=false;
        default_message.setVisibility(View.GONE);
        message.setVisibility(View.GONE);
        success_message.setVisibility(View.GONE);
        username.setHint(R.string.username);
        email.setHint(R.string.emailid);
        register.setHint(R.string.register);
        getSupportActionBar().setTitle(title);
        if (showDepartments.size()>0) {
            selectedDeptId = showDepartments.get(0).getId();
        }
        if (showDepartments.size()>1) {
            departments.setVisibility(View.VISIBLE);
            ArrayAdapter<Department> adapter = new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,showDepartments);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            departments.setAdapter(adapter);
            departments.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    selectedDeptId=showDepartments.get(i).getId();
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });
        }
    }

    @UiThread
    public void showSuccessMessage(Object msg){
        default_message.setVisibility(View.GONE);
        message.setVisibility(View.GONE);
        username.setVisibility(View.GONE);
        email.setVisibility(View.GONE);
        register.setVisibility(View.GONE);
        departments.setVisibility(View.GONE);

        success_message.setVisibility(View.VISIBLE);
        if (msg instanceof CharSequence) {
            success_message.setText((CharSequence) msg);
        }else{
            success_message.setText((int) msg);
        }
    }
}
