package com.github.rocketchat.livechat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

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
        DateFormatter.Formatter, LoadHistoryListener, AuthListener.LoginListener, ConnectListener, AgentListener.AgentConnectListener, MessageListener, TypingListener, AgentListener.AgentDataListener {


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
    private Ringtone r;
    private Date lastTimestamp;

    /**
     * This function will be called whenever messages are being selected and deselected
     * @param count
     */
    @Override
    public void onSelectionChanged(int count) {
        this.selectionCount = count;
        menu.findItem(R.id.action_delete).setVisible(count > 0);
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
        chatRoom.sendMessage(input.toString());
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
        getSupportActionBar().setTitle("LiveChat");
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

        SharedPreferences sharedPref=getPreferences(MODE_PRIVATE);
        String roomInfo=sharedPref.getString("roomInfo",null);

        if (roomInfo==null){
            Intent intent=new Intent(this,SignupActivity.class);
            startActivityForResult(intent,REQUEST_REGISTER);
        }else {
            liveChatAPI=((LiveChatApplication)getApplicationContext()).getLiveChatAPI();
            liveChatAPI.setReconnectionStrategy(null);
            chatRoom=liveChatAPI.new ChatRoom(roomInfo);
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
        }
        SharedPreferences sharedPref = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        String roonInfo=data.getStringExtra("roomInfo");
        editor.putString("roomInfo",roonInfo);
        editor.commit();
        liveChatAPI=((LiveChatApplication)getApplicationContext()).getLiveChatAPI();
        chatRoom=liveChatAPI.new ChatRoom(roonInfo);
        chatRoom.subscribeLiveChatRoom(null,this);
        initAdapter();
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
        if (i == R.id.action_delete) {
            messagesAdapter.deleteSelectedMessages();
        } else if (i == R.id.action_copy) {
            messagesAdapter.copySelectedMessagesText(this, getMessageStringFormatter(), true);
            AppUtils.showToast(this, R.string.copied_message, true);
        }else if (i == android.R.id.home) {
            onBackPressed();
        }
        return true;
    }

    /**
     * On back pressed, if messages are selected, unselect them or go back
     */
    @Override
    public void onBackPressed() {
        if (selectionCount == 0) {
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
                AppUtils.showToast(ChatActivity.this,"Connected to server",false);
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
                AppUtils.showToast(ChatActivity.this,"Disconnected from server",false);
            }
        });
    }

    @Override
    public void onConnectError(Exception websocketException) {
        Log.i ("connect error","Connect error with server");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AppUtils.showToast(ChatActivity.this,"Connection error",false);
            }
        });
    }

    @Override
    public void onLogin(GuestObject object) {
        Log.i ("success","login is successful");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AppUtils.showToast(ChatActivity.this,"Login successful",false);
            }
        });

        chatRoom.getAgentData(this);
        chatRoom.getChatHistory(20,lastTimestamp,null,this);

    }

    @Override
    public void onAgentData(AgentObject agentObject) {
        processAgent(agentObject);
    }

    @Override
    public void onAgentConnect(final AgentObject agentObject) {
        chatRoom.getChatHistory(1,lastTimestamp,null,this);
        processAgent(agentObject);
    }

    public void processAgent(final AgentObject agentObject){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                getSupportActionBar().setTitle(agentObject.getUsername());
                if (agentObject.getEmails().optJSONObject(0)!=null) {
                    getSupportActionBar().setSubtitle(agentObject.getEmails().optJSONObject(0).optString("address"));
                }
                AppUtils.showToast(ChatActivity.this,"Agent connected",true);
            }
        });

        chatRoom.subscribeRoom(null,this);
        chatRoom.subscribeTyping(null,this);
    }


    @Override
    public void onLoadHistory(ArrayList<MessageObject> list, int unreadNotLoaded) {
        lastTimestamp =list.get(list.size()-1).getMsgTimestamp();
        final ArrayList <Message> messages=new ArrayList<>();
        for (MessageObject object : list) {
            if (!object.getMessagetype().equalsIgnoreCase("command")) {
                messages.add(new Message(object.getMessageId(), new User(object.getSender().getUserId(), object.getSender().getUserName(), null, true), object.getMessage(),object.getMsgTimestamp()));
            }
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                messagesAdapter.addToEnd(messages,false);
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

    @Override
    public void onAgentDisconnect(String roomId, MessageObject object) {
        Log.i ("success","agent disconnect");
    }

    @Override
    public void onTyping(String roomId, final String user, final Boolean istyping) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (istyping) {
                    getSupportActionBar().setSubtitle(user + " is typing...");
                }else{
                    getSupportActionBar().setSubtitle("");
                }
            }
        });
    }


}
