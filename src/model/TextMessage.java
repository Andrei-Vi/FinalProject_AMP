package model;

import model.enums.MessageType;

import java.time.LocalDateTime;

public class TextMessage extends Message {
    String content;

    public TextMessage(int id, User sender, ChatRoom chatRoom, LocalDateTime timestamp, String content) {
        super(id,sender,chatRoom,timestamp,MessageType.TEXT);
        this.content = content;
    }
    public void setContent(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }
    @Override
    public String formatForDisplay() {
        return "[" + getTimestamp() + "] "
                + getSender().getUsername() +
                ": " + getContent();
    }
}
