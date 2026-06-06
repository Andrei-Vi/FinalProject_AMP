package repository;

import exception.CsvReadException;
import exception.CsvWriteException;
import model.ChatRoom;
import model.FileMessage;
import model.Message;
import model.TextMessage;
import model.User;
import model.enums.FileTransferStatus;
import model.enums.MessageType;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MessageCsvRepository {
    private static final String HEADER = "id,senderId,chatRoomId,timestamp,type,content,fileTransferId,fileName,filePath,status";

    private final Path messagesCsvPath;

    public MessageCsvRepository(String messagesCsvPath) {
        this.messagesCsvPath = Path.of(messagesCsvPath);
    }

    public List<Message> findAll(List<User> users, List<ChatRoom> chatRooms) throws CsvReadException {
        List<Message> messages = new ArrayList<>();

        for (String line : CsvUtils.readDataLines(messagesCsvPath)) {
            if (line.trim().isEmpty()) {
                continue;
            }

            List<String> values = CsvUtils.parseLine(line);
            if (values.size() < 10) {
                throw new CsvReadException("Linie invalida in messages.csv: " + line);
            }

            int id = parseInt(values.get(0), "id mesaj");
            int senderId = parseInt(values.get(1), "senderId");
            int chatRoomId = parseInt(values.get(2), "chatRoomId");
            LocalDateTime timestamp = LocalDateTime.parse(values.get(3));
            MessageType type = MessageType.valueOf(values.get(4));
            User sender = findUserById(users, senderId);
            ChatRoom chatRoom = findChatRoomById(chatRooms, chatRoomId);

            if (sender == null) {
                throw new CsvReadException("Nu exista senderId=" + senderId + " pentru mesajul " + id);
            }

            if (chatRoom == null) {
                throw new CsvReadException("Nu exista chatRoomId=" + chatRoomId + " pentru mesajul " + id);
            }

            if (type == MessageType.TEXT) {
                messages.add(new TextMessage(id, sender, chatRoom, timestamp, values.get(5)));
            } else {
                messages.add(new FileMessage(
                        id,
                        sender,
                        chatRoom,
                        timestamp,
                        parseInt(values.get(6), "fileTransferId"),
                        values.get(7),
                        values.get(8),
                        FileTransferStatus.valueOf(values.get(9))
                ));
            }
        }

        return messages;
    }

    public void saveAll(List<ChatRoom> chatRooms) throws CsvWriteException {
        List<String> lines = new ArrayList<>();

        for (ChatRoom chatRoom : chatRooms) {
            for (Message message : chatRoom.getMessages()) {
                if (message instanceof TextMessage textMessage) {
                    lines.add(CsvUtils.toCsvLine(
                            String.valueOf(textMessage.getId()),
                            String.valueOf(textMessage.getSender().getId()),
                            String.valueOf(textMessage.getChatRoom().getId()),
                            textMessage.getTimestamp().toString(),
                            textMessage.getType().name(),
                            textMessage.getContent(),
                            "",
                            "",
                            "",
                            ""
                    ));
                } else if (message instanceof FileMessage fileMessage) {
                    lines.add(CsvUtils.toCsvLine(
                            String.valueOf(fileMessage.getId()),
                            String.valueOf(fileMessage.getSender().getId()),
                            String.valueOf(fileMessage.getChatRoom().getId()),
                            fileMessage.getTimestamp().toString(),
                            fileMessage.getType().name(),
                            "",
                            String.valueOf(fileMessage.getFileTransferId()),
                            fileMessage.getFileName(),
                            fileMessage.getFilePath(),
                            fileMessage.getStatus().name()
                    ));
                }
            }
        }

        CsvUtils.writeLines(messagesCsvPath, HEADER, lines);
    }

    private User findUserById(List<User> users, int id) {
        for (User user : users) {
            if (user.getId() == id) {
                return user;
            }
        }

        return null;
    }

    private ChatRoom findChatRoomById(List<ChatRoom> chatRooms, int id) {
        for (ChatRoom chatRoom : chatRooms) {
            if (chatRoom.getId() == id) {
                return chatRoom;
            }
        }

        return null;
    }

    private int parseInt(String value, String fieldName) throws CsvReadException {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new CsvReadException("Valoare invalida pentru " + fieldName + ": " + value);
        }
    }
}
