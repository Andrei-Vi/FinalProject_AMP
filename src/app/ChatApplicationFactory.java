package app;

import exception.CsvReadException;
import exception.CsvWriteException;
import model.AdminUser;
import model.ChatRoom;
import model.FileTransfer;
import model.Message;
import model.RegularUser;
import model.User;
import model.enums.UserRole;
import model.enums.UserStatus;
import repository.AuditCsvRepository;
import repository.ChatRoomCsvRepository;
import repository.FileTransferCsvRepository;
import repository.MessageCsvRepository;
import repository.UserCsvRepository;
import service.AuditService;
import service.ChatService;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ChatApplicationFactory {
    public ChatApplicationContext create(Path dataDirectory) throws CsvReadException, CsvWriteException {
        AuditCsvRepository auditCsvRepository = new AuditCsvRepository(dataDirectory.resolve("audit.csv").toString());
        UserCsvRepository userCsvRepository = new UserCsvRepository(dataDirectory.resolve("users.csv").toString());
        ChatRoomCsvRepository chatRoomCsvRepository =
                new ChatRoomCsvRepository(dataDirectory.resolve("chatrooms.csv").toString());
        MessageCsvRepository messageCsvRepository =
                new MessageCsvRepository(dataDirectory.resolve("messages.csv").toString());
        FileTransferCsvRepository fileTransferCsvRepository =
                new FileTransferCsvRepository(dataDirectory.resolve("filetransfers.csv").toString());

        AuditService auditService = new AuditService(auditCsvRepository);
        ChatService chatService = new ChatService(auditService, userCsvRepository, messageCsvRepository,
                fileTransferCsvRepository);

        loadData(chatService, userCsvRepository, chatRoomCsvRepository, messageCsvRepository,
                fileTransferCsvRepository);

        return new ChatApplicationContext(chatService, auditService);
    }

    private void loadData(ChatService chatService, UserCsvRepository userCsvRepository,
                          ChatRoomCsvRepository chatRoomCsvRepository,
                          MessageCsvRepository messageCsvRepository,
                          FileTransferCsvRepository fileTransferCsvRepository)
            throws CsvReadException, CsvWriteException {
        List<User> users = userCsvRepository.findAll();
        List<ChatRoom> chatRooms = chatRoomCsvRepository.findAll();

        if (users.isEmpty()) {
            users = createDefaultUsers();
            userCsvRepository.saveAll(users);
        }

        if (chatRooms.isEmpty()) {
            chatRooms = createDefaultChatRooms();
            chatRoomCsvRepository.saveAll(chatRooms);
        }

        for (User user : users) {
            chatService.addUser(user);
        }

        for (ChatRoom chatRoom : chatRooms) {
            chatService.addChatRoom(chatRoom);
        }

        for (FileTransfer fileTransfer : fileTransferCsvRepository.findAll(users)) {
            chatService.addFileTransfer(fileTransfer);
        }

        for (Message message : messageCsvRepository.findAll(users, chatRooms)) {
            chatService.addMessage(message);
        }
    }

    private List<User> createDefaultUsers() {
        List<User> users = new ArrayList<>();
        users.add(new AdminUser(1, "admin", "admin123", UserRole.ADMIN, UserStatus.OFFLINE));
        users.add(new RegularUser(2, "ana", "ana123", UserRole.REGULAR, UserStatus.OFFLINE));
        users.add(new RegularUser(3, "mihai", "mihai123", UserRole.REGULAR, UserStatus.OFFLINE));
        return users;
    }

    private List<ChatRoom> createDefaultChatRooms() {
        List<ChatRoom> chatRooms = new ArrayList<>();
        chatRooms.add(new ChatRoom(1, "General"));
        chatRooms.add(new ChatRoom(2, "Facultate"));
        return chatRooms;
    }
}
