package model;

import model.enums.FileTransferStatus;
import model.enums.MessageType;

import java.time.LocalDateTime;

public class FileMessage extends Message {
    int fileTransferId;
    String fileName;
    String filePath;
    FileTransferStatus status;

    public FileMessage(int id, User sender, ChatRoom chatRoom, LocalDateTime timestamp, int fileTransferId,
                       String fileName, String filePath, FileTransferStatus status) {
        super(id,sender,chatRoom,timestamp, MessageType.FILE);
        this.fileTransferId = fileTransferId;
        this.fileName = fileName;
        this.filePath = filePath;
        this.status = status;
    }
    public int getFileTransferId() {
        return fileTransferId;
    }
    public void setFileTransferId(int fileTransferId) {
        this.fileTransferId = fileTransferId;
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
    @Override
    public String formatForDisplay() {
        return "[" + getTimestamp() + "] "
                + getSender().getUsername() +
                " a trimis fisierul: " + fileName +
                " [" + status + "]";
    }
}
