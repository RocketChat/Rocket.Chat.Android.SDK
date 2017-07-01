package com.github.rocketchat.livechat;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.UiThread;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.github.rocketchat.R;
import com.github.rocketchat.livechat.Application.LiveChatApplication;
import com.github.rocketchat.model.Message;
import com.github.rocketchat.model.User;
import com.github.rocketchat.utils.AppUtils;
import com.stfalcon.chatkit.messages.MessageInput;
import com.stfalcon.chatkit.messages.MessagesList;
import com.stfalcon.chatkit.messages.MessagesListAdapter;
import com.stfalcon.chatkit.utils.DateFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import io.rocketchat.common.data.model.ErrorObject;
import io.rocketchat.livechat.LiveChatAPI;
import io.rocketchat.livechat.callback.AgentListener;
import io.rocketchat.livechat.callback.AuthListener;
import io.rocketchat.livechat.callback.ConnectListener;
import io.rocketchat.livechat.callback.LoadHistoryListener;
import io.rocketchat.livechat.callback.MessageListener;
import io.rocketchat.livechat.callback.TypingListener;
import io.rocketchat.livechat.model.AgentObject;
import io.rocketchat.livechat.model.GuestObject;
import io.rocketchat.livechat.model.MessageObject;


public class ChatActivity extends AppCompatActivity implements
        MessagesListAdapter.SelectionListener,
        MessagesListAdapter.OnLoadMoreListener,
        MessageInput.InputListener,
        MessageInput.AttachmentsListener,
        MessageListener.MessageAckListener,
        DateFormatter.Formatter, LoadHistoryListener, AuthListener.LoginListener, ConnectListener, AgentListener.AgentConnectListener, MessageListener.SubscriptionListener, TypingListener, AgentListener.AgentDataListener {


    public static int REQUEST_REGISTER=0;

    /**
     * This will restrict total messages to 1000
     */
    private static final int TOTAL_MESSAGES_COUNT = 1000;
    /**
     * Variables for storing temporary references
     */
    private Menu menu;
    private int selectionCount;

    /**
     * MessageAdapter for loading messages, has 2 callbacks (on selection and onloadmore)
     */
    protected MessagesListAdapter<Message> messagesAdapter;

    private LiveChatAPI liveChatAPI;
    private LiveChatAPI.ChatRoom chatRoom;



    Handler Typinghandler=new Handler();
    Boolean typing=false;
    Boolean isAgentConnected=false;
    private Ringtone r;
    private Date lastTimestamp;
    private ProgressDialog dialog;
    private AlertDialog.Builder builder;

    private String agentEmail;


    private SharedPreferences.Editor editor;

    /**
     * This function will be called whenever messages are being selected and deselected
     * @param count
     */
    @Override
    public void onSelectionChanged(int count) {
        this.selectionCount = count;
//        menu.findItem(R.id.action_delete).setVisible(count > 0);
        menu.findItem(R.id.action_copy).setVisible(count > 0);

    }

    /**
     * On load more for pagination
     * @param page
     * @param totalItemsCount
     */
    @Override
    public void onLoadMore(int page, int totalItemsCount) {
        if (totalItemsCount < TOTAL_MESSAGES_COUNT) {
            chatRoom.getChatHistory(20,lastTimestamp,null,this);
        }
    }

    /**
     * On message submit
     * @param input
     * @return
     */

    @Override
    public boolean onSubmit(final CharSequence input) {
        chatRoom.sendMessage(input.toString(),this);
        if (!isAgentConnected){
            dialog.show();
        }
        return true;
    }

    /**
     * When clicked on attachment
     */
    @Override
    public void onAddAttachments() {

    }

    /**
     * For headers of messages
     * @param date
     * @return
     */

    @Override
    public String format(Date date) {
        if (DateFormatter.isToday(date)) {
            return getString(R.string.date_header_today);
        } else if (DateFormatter.isYesterday(date)) {
            return getString(R.string.date_header_yesterday);
        } else {
            return DateFormatter.format(date, DateFormatter.Template.STRING_DAY_MONTH_YEAR);
        }
    }

    private MessagesList messagesList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        getSupportActionBar().setTitle("LiveChat Room");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);



//        getSupportActionBar().setSubtitle("Communicating with party....");
        messagesList = (MessagesList) findViewById(R.id.messagesList);

        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        r = RingtoneManager.getRingtone(getApplicationContext(), notification);

        MessageInput input = (MessageInput) findViewById(R.id.input);
        input.setInputListener(this);
        input.setAttachmentsListener(this);
        input.getInputEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (!typing){
                    typing=true;
                    chatRoom.sendIsTyping(true);
                }
                Typinghandler.removeCallbacks(onTypingTimeout);
                Typinghandler.postDelayed(onTypingTimeout,600);
            }

            Runnable onTypingTimeout=new Runnable() {
                @Override
                public void run() {
                    typing=false;
                    chatRoom.sendIsTyping(false);
                }
            };

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        /**
         * setting dialogs for info
         */

        dialog=new ProgressDialog(this);
        dialog.setIndeterminate(true);
        dialog.setCancelable(false);
        dialog.setMessage("Contacting agent...");

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
//        } else {
            builder = new AlertDialog.Builder(this);
//        }
//        builder.setTitle("Close conversation")
          builder.setMessage("Are you sure to close this conversation?")
                  .setCancelable(false)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                // continue with delete

                        ChatActivity.this.dialog.setMessage("Closing conversation ...");
                        ChatActivity.this.dialog.show();
                        chatRoom.closeConversation();
                    }
                })
                .setNegativeButton(android.R.string.no, null);



        SharedPreferences sharedPref=getPreferences(MODE_PRIVATE);
        String roomInfo=sharedPref.getString("roomInfo",null);
        editor = sharedPref.edit();

        if (roomInfo==null){
            Intent intent=new Intent(this,SignupActivity.class);
            startActivityForResult(intent,REQUEST_REGISTER);
        }else {
            liveChatAPI=((LiveChatApplication)getApplicationContext()).getLiveChatAPI();
            liveChatAPI.setReconnectionStrategy(null);
            chatRoom=liveChatAPI.new ChatRoom(roomInfo);
            dialog.setMessage("Connecting ...");
            dialog.show();
            liveChatAPI.connect(this);
            initAdapter();
        }
    }


    private void initAdapter() {
        messagesAdapter = new MessagesListAdapter<>(chatRoom.getUserId(), null);
        messagesAdapter.enableSelectionMode(this);
        messagesAdapter.setLoadMoreListener(this);
        messagesAdapter.setDateHeadersFormatter(this);
        messagesList.setAdapter(messagesAdapter);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (RESULT_OK!=resultCode){
            finish();
        }else {
            String roonInfo = data.getStringExtra("roomInfo");
            editor.putString("roomInfo", roonInfo);
            editor.commit();

            liveChatAPI = ((LiveChatApplication) getApplicationContext()).getLiveChatAPI();
            liveChatAPI.setConnectListener(this);
            chatRoom = liveChatAPI.new ChatRoom(roonInfo);
            chatRoom.subscribeLiveChatRoom(null, this);
            initAdapter();
        }
    }

    /**
     * On create load options Menu
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        getMenuInflater().inflate(R.menu.chat_actions_menu, menu);
        menu.findItem(R.id.contact_via_mail).setVisible(false);
        menu.findItem(R.id.action_close_conversation).setVisible(false);
        onSelectionChanged(0);
        return true;
    }

    /**
     * on Options selected, if delete then delete selected messages or copy selected messages
     * @param item
     * @return
     */

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int i = item.getItemId();
//        if (i == R.id.action_delete) {
//            messagesAdapter.deleteSelectedMessages();
//        }
        if (i == R.id.action_copy) {
            messagesAdapter.copySelectedMessagesText(this, getMessageStringFormatter(), true);
            AppUtils.showToast(this, R.string.copied_message, true);
        }else if (i == android.R.id.home) {
            onBackPressed();
        }else if (i==R.id.action_close_conversation){
            builder.show();
        }else if (i==R.id.contact_via_mail){
            if (agentEmail!=null) {
                composeEmail(new String[]{agentEmail}, "Need support");
            }else {
                AppUtils.showToast(this,"Agent not connected",false);
            }
        }
        return true;
    }

    /**
     * On back pressed, if messages are selected, unselect them or go back
     */

    @Override
    public void onBackPressed() {
        if (selectionCount == 0) {
            liveChatAPI.disconnect();
            super.onBackPressed();
        } else {
            messagesAdapter.unselectAllItems();
        }
    }

    /**
     * This function is for formatting copied messages
     * @return
     */

    private MessagesListAdapter.Formatter<Message> getMessageStringFormatter() {
        return new MessagesListAdapter.Formatter<Message>() {
            @Override
            public String format(Message message) {
                String createdAt = new SimpleDateFormat("MMM d, EEE 'at' h:mm a", Locale.getDefault())
                        .format(message.getCreatedAt());

                String text = message.getText();
                if (text == null) text = "[attachment]";

                return String.format(Locale.getDefault(), "%s: %s (%s)",
                        message.getUser().getName(), text, createdAt);
            }
        };
    }



    @Override
    public void onConnect(String sessionID) {
        Log.i ("success","connection is successful");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dialog.setMessage("Logging in ...");
//                AppUtils.showToast(ChatActivity.this,"Connected to server",false);
                Snackbar
                        .make(findViewById(R.id.chat_activity), R.string.connected, Snackbar.LENGTH_LONG)
                        .show();
            }
        });
        chatRoom.login(this);
    }

    @Override
    public void onDisconnect(boolean closedByServer) {
        Log.i ("disconnect","disconnected from server");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (dialog.isShowing()){
                    dialog.dismiss();
                }
//                AppUtils.showToast(ChatActivity.this,"Disconnected from server",false);
                AppUtils.getSnackbar(findViewById(R.id.chat_activity),R.string.disconnected_from_server)
                        .setAction("RETRY", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                dialog.setMessage("Connecting ...");
                                dialog.show();
                                liveChatAPI.reconnect();
                            }
                        })
                        .show();
            }
        });
    }

    @Override
    public void onConnectError(Exception websocketException) {
        Log.i ("connect error","Connect error with server");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
//                AppUtils.showToast(ChatActivity.this,"Connection error",false);
                if (dialog.isShowing()){
                    dialog.dismiss();
                }
                AppUtils.getSnackbar(findViewById(R.id.chat_activity),R.string.connection_error)
                        .setAction("RETRY", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                dialog.setMessage("Connecting ...");
                                dialog.show();
                                liveChatAPI.reconnect();
                            }
                        })
                        .show();
            }
        });
    }

    @Override
    public void onLogin(GuestObject object,final ErrorObject error) {
        Log.i ("success","login is successful");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dialog.setMessage("Loading history ...");
                AppUtils.showToast(ChatActivity.this,"Login successful",false);
            }
        });

        chatRoom.getAgentData(this);
    }

    @Override
    public void onAgentData(AgentObject agentObject,final ErrorObject error) {
        if (error!=null){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (dialog.isShowing()) {
                        dialog.dismiss();
                    }
                    dialog.setMessage("Contacting agent...");
                    AppUtils.showToast(ChatActivity.this, R.string.no_agent_available , false);
                }
            });
            chatRoom.subscribeLiveChatRoom(null, this);
        }else {
            processAgent(agentObject);
            chatRoom.getChatHistory(20,lastTimestamp,null,this);
        }
    }

    @Override
    public void onAgentConnect(final AgentObject agentObject) {
        processAgent(agentObject);
        chatRoom.getChatHistory(1,lastTimestamp,null,this);
    }

    public void processAgent(final AgentObject agentObject){
        isAgentConnected=true;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                menu.findItem(R.id.contact_via_mail).setVisible(true);
                menu.findItem(R.id.action_close_conversation).setVisible(true);

                getSupportActionBar().setTitle(agentObject.getUsername());
                if (agentObject.getEmails().optJSONObject(0)!=null) {
                    agentEmail=agentObject.getEmails().optJSONObject(0).optString("address");
                    getSupportActionBar().setSubtitle(agentEmail);
                }
                if (dialog.isShowing()){
                    dialog.dismiss();
                }
                AppUtils.showToast(ChatActivity.this,"Agent connected",true);

            }
        });

        chatRoom.subscribeRoom(null,this);
        chatRoom.subscribeTyping(null,this);
    }


    @Override
    public void onLoadHistory(ArrayList<MessageObject> list, int unreadNotLoaded,final ErrorObject error) {

            lastTimestamp = list.get(list.size() - 1).getMsgTimestamp();
            final ArrayList<Message> messages = new ArrayList<>();
            for (MessageObject object : list) {
                if (!object.getMessagetype().equalsIgnoreCase("command")) {
                    messages.add(new Message(object.getMessageId(), new User(object.getSender().getUserId(), object.getSender().getUserName(), null, true), object.getMessage(), object.getMsgTimestamp()));
                }
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (dialog.isShowing()) {
                        dialog.dismiss();
                    }
                    messagesAdapter.addToEnd(messages, false);
                }
            });
    }


    @Override
    public void onMessage(String roomId, final MessageObject object) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    r.play();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                messagesAdapter.addToStart(new Message(object.getMessageId(),new User(object.getSender().getUserId(), object.getSender().getUserName(), null, true), object.getMessage(),object.getMsgTimestamp()),true);
            }
        });
    }

    @UiThread
    @Override
    public void onAgentDisconnect(String roomId, MessageObject object) {
        Log.i ("success","agent disconnect");
        editor.clear();
        editor.commit();

        if (dialog.isShowing()){
            dialog.dismiss();
        }
        if (object.getSender().getUserId().equals(chatRoom.getUserId())){
            finish();
        }else{

            builder = new AlertDialog.Builder(this)
                    .setTitle("Conversation closed by agent")
                    .setCancelable(false)
                    .setMessage("Message is \""+object.getMessage()+"\"")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            finish();
                        }
                    });

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    builder.show();
                }
            });

        }
    }

    @Override
    public void onTyping(String roomId, final String user, final Boolean istyping) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (istyping) {
                    getSupportActionBar().setSubtitle(user + " is typing...");
                }else{
                    getSupportActionBar().setSubtitle(agentEmail);
                }
            }
        });
    }

    public void composeEmail(String[] addresses, String subject) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:")); // only email apps should handle this
        intent.putExtra(Intent.EXTRA_EMAIL, addresses);
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }


    @Override
    public void onMessageAck(MessageObject object, final ErrorObject error) {
        Log.i ("success","got error here");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (dialog.isShowing()){
                    dialog.dismiss();
                }
                if (error!=null){
                    AppUtils.showToast(ChatActivity.this, error.getMessage() , false);
                }
            }
        });
    }
}
