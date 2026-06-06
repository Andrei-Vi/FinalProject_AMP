package model;

import model.enums.ConnectionStatus;

import java.time.LocalDateTime;

public class Session {
    User user;
    ChatRoom currentChatRoom;
    ConnectionStatus connectionStatus;
    LocalDateTime connectedAt;

    public Session(User user, ChatRoom currentChatRoom, ConnectionStatus connectionStatus, LocalDateTime connectedAt) {
        this.user = user;
        this.currentChatRoom = currentChatRoom;
        this.connectionStatus = connectionStatus;
        this.connectedAt = connectedAt;
    }
    public void joinRoom(ChatRoom room) {
        this.currentChatRoom = room;
        this.connectionStatus = ConnectionStatus.IN_ROOM;
    }
    public void leaveRoom() {
        this.currentChatRoom = null;
        this.connectionStatus = ConnectionStatus.CONNECTED;
    }
    public ChatRoom getCurrentChatRoom() {
        return currentChatRoom;
    }
    public ConnectionStatus getConnectionStatus() {
        return connectionStatus;
    }
    public void setConnectionStatus(ConnectionStatus connectionStatus) {
        this.connectionStatus = connectionStatus;
    }
    public LocalDateTime getConnectedAt() {
        return connectedAt;
    }
    public void setConnectedAt(LocalDateTime connectedAt) {
        this.connectedAt = connectedAt;
    }
    public User getUser() {
        return user;
    }
    public void setUser(User user) {
        this.user = user;
    }
    public ChatRoom getChatRoom() {
        return currentChatRoom;
    }
    public void setChatRoom(ChatRoom currentChatRoom) {
        this.currentChatRoom = currentChatRoom;
    }

}
