package com.getstream.sdk.chat.utils;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.getstream.sdk.chat.adapter.MessageListItem;
import com.getstream.sdk.chat.adapter.MessageListItemAdapter;
import com.getstream.sdk.chat.adapter.MessageViewHolderFactory;
import com.getstream.sdk.chat.rest.Message;
import com.getstream.sdk.chat.rest.User;
import com.getstream.sdk.chat.rest.response.ChannelUserRead;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;


public class MessageListItemLiveData extends LiveData<MessageListItemWrapper> {
    private MutableLiveData<List<Message>> messages;
    private MutableLiveData<List<User>> typing;
    private MutableLiveData<List<ChannelUserRead>> reads;

    private User currentUser;
    private List<MessageListItem> messageEntities;
    private List<MessageListItem> typingEntities;
    private List<ChannelUserRead> listReads;
    private Boolean isLoadingMore;


    public MessageListItemLiveData(User currentUser, MutableLiveData<List<Message>> messages, MutableLiveData<List<User>> typing, MutableLiveData<List<ChannelUserRead>> reads) {
        this.messages = messages;
        this.currentUser = currentUser;
        this.typing = typing;
        this.reads = reads;
        this.messageEntities = new ArrayList<>();
        this.typingEntities = new ArrayList<>();
        this.listReads = new ArrayList<>();
        this.isLoadingMore = false;
    }

    public void setIsLoadingMore(Boolean loading) {
        isLoadingMore = loading;
    }

    private void broadcastValue() {
        List<MessageListItem> merged = new ArrayList<>();
        merged.addAll(messageEntities);

        // TODO replace with more efficient approach
        // this wil become slow with many users and many messages
        for (ChannelUserRead r : listReads) {
            for (int i = merged.size(); i-- > 0; ) {
                MessageListItem e = merged.get(i);
                // TODO: make sure this is a good check, atm without this everything breaks :)
                if (e.getType() != MessageListItemAdapter.EntityType.MESSAGE) {
                    continue;
                }
                if (r.getLastRead().getTime() > e.getMessage().getCreatedAt().getTime()) {
                    // set the read state on this entity
                    e.addMessageReadBy(r);
                }

            }
        }

        merged.addAll(typingEntities);
        MessageListItemWrapper wrapper = new MessageListItemWrapper(isLoadingMore, merged);
        setValue(wrapper);
        // isLoadingMore is only true once...
        if (isLoadingMore) {
            this.setIsLoadingMore(true);
        }
    }

    private boolean isSameDay(Message a, Message b) {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd");
        return fmt.format(a.getCreatedAt()).equals(fmt.format(b.getCreatedAt()));
    }

    @Override
    public void observe(@NonNull LifecycleOwner owner, @NonNull Observer<? super MessageListItemWrapper> observer) {
        super.observe(owner, observer);
        this.reads.observe(owner, reads -> {
            listReads = reads;
            broadcastValue();
        });
        this.messages.observe(owner, messages -> {
            // update based on messages
            List<MessageListItem> entities = new ArrayList<MessageListItem>();
            // iterate over messages and stick in the date entities
            Message previousMessage = null;
            int size = messages.size();
            int topIndex = Math.max(0, size -1);
            for (int i = 0; i < size; i++) {
                Message message = messages.get(i);
                Message nextMessage = null;
                if (i +1 <= topIndex){
                    nextMessage = messages.get(i+1);
                }

                // determine if the message is written by the current user
                Boolean mine = message.getUser().equals(currentUser);
                // determine the position (top, middle, bottom)
                User user = message.getUser();
                List<MessageViewHolderFactory.Position> positions = new ArrayList<MessageViewHolderFactory.Position>();
                if (previousMessage == null || !previousMessage.getUser().equals(user)) {
                    positions.add(MessageViewHolderFactory.Position.TOP);
                }

                if (nextMessage == null || !nextMessage.getUser().equals(user)) {
                    positions.add(MessageViewHolderFactory.Position.BOTTOM);
                }

                if (previousMessage != null && nextMessage != null) {
                    if (previousMessage.getUser().equals(user) && nextMessage.getUser().equals(user)) {
                        positions.add(MessageViewHolderFactory.Position.MIDDLE);
                    }
                }
                // date separator
                if (previousMessage != null && !isSameDay(previousMessage, message)) {
                    entities.add(new MessageListItem(message.getCreatedAt()));
                }

                MessageListItem messageListItem = new MessageListItem(message,positions, mine);
                entities.add(messageListItem);
                // set the previous message for the next iteration
                previousMessage = message;
            }

            this.messageEntities = entities;
            broadcastValue();
        });
        this.typing.observe(owner, users -> {
            // update
            List<MessageListItem> typingEntities = new ArrayList<MessageListItem>();
            if (users.size() > 0) {
                MessageListItem messageListItem = new MessageListItem(users);
                typingEntities.add(messageListItem);
            }
            this.typingEntities = typingEntities;
            broadcastValue();
        });

    }
}