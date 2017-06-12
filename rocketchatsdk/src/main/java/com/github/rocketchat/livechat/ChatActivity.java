package com.github.rocketchat.livechat;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.github.rocketchat.R;
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

import io.rocketchat.common.utils.Utils;
import io.rocketchat.livechat.LiveChatAPI;
import io.rocketchat.livechat.callback.AgentCallback;
import io.rocketchat.livechat.callback.ConnectCallback;
import io.rocketchat.livechat.callback.GuestCallback;
import io.rocketchat.livechat.callback.HistoryCallback;
import io.rocketchat.livechat.callback.MessageCallback;
import io.rocketchat.livechat.callback.TypingCallback;
import io.rocketchat.livechat.middleware.LiveChatMiddleware;
import io.rocketchat.livechat.model.AgentObject;
import io.rocketchat.livechat.model.GuestObject;
import io.rocketchat.livechat.model.MessageObject;

public class ChatActivity extends AppCompatActivity implements
        MessagesListAdapter.SelectionListener,
        MessagesListAdapter.OnLoadMoreListener,
        MessageInput.InputListener,
        MessageInput.AttachmentsListener,
        DateFormatter.Formatter,
        ConnectCallback,
        GuestCallback,
        HistoryCallback,
        MessageCallback,
        TypingCallback,
        AgentCallback{


    private String url="ws://192.168.43.149:3000/websocket";

    public static String authToken="ubS92xhRYz6pRklXXNxU86z7bzxMo9a4wjq7KtVV8kh";
    public static String visitorToken="gxCgQjdSisYWJGuSf";
    public static String userID="CPse2MSPxc5YbAgzJ";
    public static String roomID="qdyaxcrgqgxl";
    public static String username="guest-5";

    /**
     * This will restrict total messages to 100
     */
    private static final int TOTAL_MESSAGES_COUNT = 100;
    /**
     * Variables for storing temporary references
     */
    private Menu menu;
    private int selectionCount;

    /**
     * MessageAdapter for loading messages, has 2 callbacks (on selection and onloadmore)
     */
    protected MessagesListAdapter<Message> messagesAdapter;

    private LiveChatAPI liveChat;



    Handler Typinghandler=new Handler();
    Boolean typing=false;

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

        }
    }

    /**
     * On message submit
     * @param input
     * @return
     */

    @Override
    public boolean onSubmit(final CharSequence input) {
        String shortID= Utils.shortUUID();
        liveChat.sendMessage(shortID,roomID,input.toString(),visitorToken);
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
        initAdapter();

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
                    liveChat.sendIsTyping(roomID,username,true);
                }
                Typinghandler.removeCallbacks(onTypingTimeout);
                Typinghandler.postDelayed(onTypingTimeout,600);
            }

            Runnable onTypingTimeout=new Runnable() {
                @Override
                public void run() {
                    typing=false;
                    liveChat.sendIsTyping(roomID,username,false);
                }
            };

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        liveChat=new LiveChatAPI(url);
        liveChat.connectAsync(this);
    }

    private void initAdapter() {
        messagesAdapter = new MessagesListAdapter<>(userID, null);
        messagesAdapter.enableSelectionMode(this);
        messagesAdapter.setLoadMoreListener(this);
        messagesAdapter.setDateHeadersFormatter(this);
        messagesList.setAdapter(messagesAdapter);
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
        liveChat.login(authToken,ChatActivity.this);
    }

    @Override
    public void call(LiveChatMiddleware.CallbackType guestCallbackType, GuestObject object) {
        Log.i ("success","Login is successfull");
//        liveChat.subscribeLiveChatRoom(roomID,false,null,this);
        liveChat.subscribeRoom(roomID,false,null,this);
        liveChat.subscribeTyping(roomID,false,null,this);
        liveChat.getAgentData(roomID,this);
        liveChat.getChatHistory(roomID,50,new Date(),this);
    }

    @Override
    public void call(ArrayList<MessageObject> list, int unreadNotLoaded) {
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
    public void call(String roomId, final MessageObject object) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                messagesAdapter.addToStart(new Message(object.getMessageId(),new User(object.getSender().getUserId(), object.getSender().getUserName(), null, true), object.getMessage(),object.getMsgTimestamp()),true);
            }
        });
    }

    @Override
    public void call(String roomId, final String user, final Boolean istyping) {
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

    @Override
    public void call(LiveChatMiddleware.AgentCallbackType agentCallbackType, final AgentObject object) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                getSupportActionBar().setTitle(object.getUsername());
                if (object.getEmails().optJSONObject(0)!=null) {
                    getSupportActionBar().setSubtitle(object.getEmails().optJSONObject(0).optString("address"));
                }
            }
        });
    }
}
