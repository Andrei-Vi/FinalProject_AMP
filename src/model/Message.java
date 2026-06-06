package model;

import model.enums.MessageType;

import java.time.LocalDateTime;

abstract public class Message {
    int id;
    User sender;
    ChatRoom chatRoom;
    LocalDateTime timestamp;
    MessageType type;

    public Message(int id, User sender, ChatRoom chatRoom, LocalDateTime timestamp, MessageType type) {
        this.id = id;
        this.sender = sender;
        this.chatRoom = chatRoom;
        this.timestamp = timestamp;
        this.type = type;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public ChatRoom getChatRoom() {
        return chatRoom;
    }

    public void setChatRoom(ChatRoom chatRoom) {
        this.chatRoom = chatRoom;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }
    abstract public String formatForDisplay();
}
