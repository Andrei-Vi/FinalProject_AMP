package model;

import model.enums.FileTransferStatus;

import java.time.LocalDateTime;

public class FileTransfer {
    int id;
    User sender;
    int ChatRoomId;
    String fileName;
    String filePath;
    FileTransferStatus status;
    LocalDateTime timestamp;

    public FileTransfer(User sender, int chatRoomId, int id, FileTransferStatus status, String filePath,
                        String fileName, LocalDateTime timestamp) {
        this.sender = sender;
        ChatRoomId = chatRoomId;
        this.id = id;
        this.status = status;
        this.filePath = filePath;
        this.fileName = fileName;
        this.timestamp = timestamp;
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

    public int getChatRoomId() {
        return ChatRoomId;
    }

    public void setChatRoomId(int chatRoomId) {
        ChatRoomId = chatRoomId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public FileTransferStatus getStatus() {
        return status;
    }

    public void setStatus(FileTransferStatus status) {
        this.status = status;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public void accept() {
        status = FileTransferStatus.ACCEPTED;
    }
    public void reject() {
        status = FileTransferStatus.REJECTED;
    }
    public void markDownloaded() {
        status = FileTransferStatus.DOWNLOADED;
    }
}