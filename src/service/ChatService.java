package service;

import exception.*;
import model.ChatRoom;
import model.FileMessage;
import model.FileTransfer;
import model.Message;
import model.Session;
import model.TextMessage;
import model.User;
import model.enums.ConnectionStatus;
import model.enums.FileTransferStatus;
import model.enums.UserStatus;
import network.LocalhostMessageTransport;
import repository.FileTransferCsvRepository;
import repository.MessageCsvRepository;
import repository.UserCsvRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ChatService {
    private final List<User> users;
    private final List<ChatRoom> chatRooms;
    private final List<Session> sessions;
    private final List<FileTransfer> fileTransfers;
    private final AuditService auditService;
    private final LocalhostMessageTransport messageTransport;
    private final UserCsvRepository userCsvRepository;
    private final MessageCsvRepository messageCsvRepository;
    private final FileTransferCsvRepository fileTransferCsvRepository;
    private int nextMessageId;
    private int nextFileTransferId;

    public ChatService(AuditService auditService, UserCsvRepository userCsvRepository,
                       MessageCsvRepository messageCsvRepository,
                       FileTransferCsvRepository fileTransferCsvRepository) {
        this(auditService, new LocalhostMessageTransport(), userCsvRepository, messageCsvRepository,
                fileTransferCsvRepository);
    }

    public ChatService(AuditService auditService, LocalhostMessageTransport messageTransport,
                       UserCsvRepository userCsvRepository, MessageCsvRepository messageCsvRepository,
                       FileTransferCsvRepository fileTransferCsvRepository) {
        this.users = new ArrayList<>();
        this.chatRooms = new ArrayList<>();
        this.sessions = new ArrayList<>();
        this.fileTransfers = new ArrayList<>();
        this.auditService = auditService == null ? new AuditService() : auditService;
        this.messageTransport = messageTransport == null ? new LocalhostMessageTransport() : messageTransport;
        this.userCsvRepository = userCsvRepository;
        this.messageCsvRepository = messageCsvRepository;
        this.fileTransferCsvRepository = fileTransferCsvRepository;
        this.nextMessageId = 1;
        this.nextFileTransferId = 1;
    }

    public void addUser(User user) {
        users.add(user);
    }

    public void addChatRoom(ChatRoom chatRoom) {
        chatRooms.add(chatRoom);
    }

    public void addMessage(Message message) {
        if (message == null || message.getChatRoom() == null) {
            return;
        }

        message.getChatRoom().addMessage(message);
        nextMessageId = Math.max(nextMessageId, message.getId() + 1);
    }

    public void addFileTransfer(FileTransfer fileTransfer) {
        if (fileTransfer == null) {
            return;
        }

        fileTransfers.add(fileTransfer);
        nextFileTransferId = Math.max(nextFileTransferId, fileTransfer.getId() + 1);
    }

    public List<ChatRoom> getChatRooms() {
        return new ArrayList<>(chatRooms);
    }

    public List<User> getUsers() {
        return new ArrayList<>(users);
    }

    public List<FileTransfer> getFileTransfersForCurrentRoom(User user)
            throws SessionNotFoundException, NoActiveChatRoomException {
        Session session = findSessionByUser(user);

        if (session == null) {
            throw new SessionNotFoundException("Userul nu este logat.");
        }

        if (session.getCurrentChatRoom() == null) {
            throw new NoActiveChatRoomException("Userul " + user.getUsername() + " nu este intr-o camera.");
        }

        int currentRoomId = session.getCurrentChatRoom().getId();
        List<FileTransfer> roomTransfers = new ArrayList<>();

        for (FileTransfer fileTransfer : fileTransfers) {
            if (fileTransfer.getChatRoomId() == currentRoomId) {
                roomTransfers.add(fileTransfer);
            }
        }

        return roomTransfers;
    }

    private User findUserByUsername(String username) {
        for (User user : users) {
            if (user.getUsername().equals(username)) {
                return user;
            }
        }

        return null;
    }

    private ChatRoom findChatRoomById(int id) {
        for (ChatRoom chatRoom : chatRooms) {
            if (chatRoom.getId() == id) {
                return chatRoom;
            }
        }

        return null;
    }

    private Session findSessionByUser(User user) {
        for (Session session : sessions) {
            if (user != null && session.getUser().getId() == user.getId()) {
                return session;
            }
        }

        return null;
    }

    public Session login(String username, String password)
            throws UserNotFoundException, UserBannedException, InvalidCredentialsException, CsvWriteException {
        User user = findUserByUsername(username);

        if (user == null) {
            throw new UserNotFoundException("Nu exista userul cu username-ul: " + username);
        }

        if (user.isBanned()) {
            throw new UserBannedException("Userul " + username + " este banat.");
        }

        if (!user.getPassword().equals(password)) {
            throw new InvalidCredentialsException("Parola este gresita pentru userul: " + username);
        }

        Session existingSession = findSessionByUser(user);
        if (existingSession != null) {
            auditService.logAction("LOGIN", user, "existingSession=true");
            return existingSession;
        }

        user.setStatus(UserStatus.ONLINE);

        Session session = new Session(user, null, ConnectionStatus.CONNECTED, LocalDateTime.now());
        sessions.add(session);

        persistUsers();
        auditService.logAction("LOGIN", user);

        return session;
    }

    public void joinRoom(User user, int chatRoomId)
            throws SessionNotFoundException, ChatRoomNotFoundException, UserBannedException, CsvWriteException {
        if (user == null) {
            throw new SessionNotFoundException("Nu exista o sesiune activa pentru userul primit.");
        }

        if (user.isBanned()) {
            throw new UserBannedException("Userul " + user.getUsername() + " este banat.");
        }

        Session session = findSessionByUser(user);
        ChatRoom chatRoom = findChatRoomById(chatRoomId);

        if (session == null) {
            throw new SessionNotFoundException("Userul " + user.getUsername() + " nu este logat.");
        }

        if (chatRoom == null) {
            throw new ChatRoomNotFoundException("Nu exista camera cu id-ul: " + chatRoomId);
        }

        chatRoom.addMember(user);
        session.joinRoom(chatRoom);
        user.setStatus(UserStatus.IN_ROOM);

        persistUsers();
        auditService.logAction("JOIN_ROOM", user, "roomId=" + chatRoom.getId() + ", roomName=" + chatRoom.getName());
    }

    public void leaveRoom(User user) throws SessionNotFoundException, NoActiveChatRoomException, CsvWriteException {
        Session session = findSessionByUser(user);

        if (session == null) {
            throw new SessionNotFoundException("Userul nu este logat.");
        }

        if (session.getCurrentChatRoom() == null) {
            throw new NoActiveChatRoomException("Userul " + user.getUsername() + " nu este intr-o camera.");
        }

        ChatRoom chatRoom = session.getCurrentChatRoom();
        chatRoom.removeMember(user);
        session.leaveRoom();
        user.setStatus(UserStatus.ONLINE);

        persistUsers();
        auditService.logAction("LEAVE_ROOM", user, "roomId=" + chatRoom.getId() + ", roomName=" + chatRoom.getName());
    }

    public void logout(User user) throws SessionNotFoundException, CsvWriteException {
        Session session = findSessionByUser(user);

        if (session == null) {
            throw new SessionNotFoundException("Userul nu este logat.");
        }

        if (session.getCurrentChatRoom() != null) {
            session.getCurrentChatRoom().removeMember(user);
        }

        sessions.remove(session);
        user.setStatus(UserStatus.OFFLINE);

        persistUsers();
        auditService.logAction("LOGOUT", user);
    }

    public TextMessage sendMessage(User user, String content)
            throws SessionNotFoundException, NoActiveChatRoomException, UserBannedException, EmptyMessageException,
            CsvWriteException, NetworkException {
        if (user == null) {
            throw new SessionNotFoundException("Nu exista o sesiune activa pentru userul primit.");
        }

        if (user.isBanned()) {
            throw new UserBannedException("Userul " + user.getUsername() + " este banat.");
        }

        Session session = findSessionByUser(user);

        if (session == null) {
            throw new SessionNotFoundException("Userul " + user.getUsername() + " nu este logat.");
        }

        if (session.getCurrentChatRoom() == null) {
            throw new NoActiveChatRoomException("Userul " + user.getUsername() + " nu este intr-o camera.");
        }

        if (content == null || content.trim().isEmpty()) {
            throw new EmptyMessageException("Mesajul nu poate fi gol.");
        }

        String deliveredContent = messageTransport.send(content);

        TextMessage message = new TextMessage(
                nextMessageId++,
                user,
                session.getCurrentChatRoom(),
                LocalDateTime.now(),
                deliveredContent
        );

        session.getCurrentChatRoom().addMessage(message);
        persistMessages();
        auditService.logAction("SEND_MESSAGE", user, "roomId=" + session.getCurrentChatRoom().getId() + ", messageId=" + message.getId());

        return message;
    }

    public FileTransfer sendFile(User user, String fileName, String filePath)
            throws SessionNotFoundException, NoActiveChatRoomException, UserBannedException, InvalidFileException,
            CsvWriteException {
        if (user == null) {
            throw new SessionNotFoundException("Nu exista o sesiune activa pentru userul primit.");
        }

        if (user.isBanned()) {
            throw new UserBannedException("Userul " + user.getUsername() + " este banat.");
        }

        Session session = findSessionByUser(user);

        if (session == null) {
            throw new SessionNotFoundException("Userul " + user.getUsername() + " nu este logat.");
        }

        if (session.getCurrentChatRoom() == null) {
            throw new NoActiveChatRoomException("Userul " + user.getUsername() + " nu este intr-o camera.");
        }

        if (fileName == null || fileName.trim().isEmpty() || filePath == null || filePath.trim().isEmpty()) {
            throw new InvalidFileException("Numele fisierului si path-ul trebuie completate.");
        }

        Path sourcePath = Path.of(filePath);
        if (!Files.exists(sourcePath) || !Files.isRegularFile(sourcePath)) {
            throw new InvalidFileException("Fisierul sursa nu exista sau nu este un fisier valid: " + filePath);
        }

        LocalDateTime timestamp = LocalDateTime.now();
        ChatRoom chatRoom = session.getCurrentChatRoom();

        FileTransfer fileTransfer = new FileTransfer(
                user,
                chatRoom.getId(),
                nextFileTransferId++,
                FileTransferStatus.SENT,
                filePath,
                fileName,
                timestamp
        );

        FileMessage fileMessage = new FileMessage(
                nextMessageId++,
                user,
                chatRoom,
                timestamp,
                fileTransfer.getId(),
                fileName,
                filePath,
                fileTransfer.getStatus()
        );

        fileTransfers.add(fileTransfer);
        chatRoom.addMessage(fileMessage);
        persistFileTransfers();
        persistMessages();
        auditService.logAction("SEND_FILE", user, "roomId=" + chatRoom.getId() + ", transferId=" + fileTransfer.getId() + ", fileName=" + fileName);

        return fileTransfer;
    }

    public Path downloadFile(User user, int fileTransferId, String destinationDirectory)
            throws SessionNotFoundException, NoActiveChatRoomException, InvalidFileException, FileDownloadException,
            CsvWriteException {
        Session session = findSessionByUser(user);

        if (session == null) {
            throw new SessionNotFoundException("Userul nu este logat.");
        }

        if (session.getCurrentChatRoom() == null) {
            throw new NoActiveChatRoomException("Userul " + user.getUsername() + " nu este intr-o camera.");
        }

        if (destinationDirectory == null || destinationDirectory.trim().isEmpty()) {
            throw new InvalidFileException("Folderul de destinatie trebuie completat.");
        }

        FileTransfer fileTransfer = findFileTransferById(fileTransferId);

        if (fileTransfer == null || fileTransfer.getChatRoomId() != session.getCurrentChatRoom().getId()) {
            throw new InvalidFileException("Nu exista transferul cu id-ul " + fileTransferId + " in camera curenta.");
        }

        Path sourcePath = Path.of(fileTransfer.getFilePath());
        Path destinationDirectoryPath = Path.of(destinationDirectory);
        Path destinationPath = destinationDirectoryPath.resolve(fileTransfer.getFileName());

        try {
            Files.createDirectories(destinationDirectoryPath);
            Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new FileDownloadException("Nu s-a putut descarca fisierul: " + exception.getMessage());
        }

        fileTransfer.markDownloaded();
        updateFileMessageStatus(session.getCurrentChatRoom(), fileTransferId);
        persistFileTransfers();
        persistMessages();
        auditService.logAction("DOWNLOAD_FILE", user, "transferId=" + fileTransferId + ", destination=" + destinationPath);

        return destinationPath;
    }

    private FileTransfer findFileTransferById(int fileTransferId) {
        for (FileTransfer fileTransfer : fileTransfers) {
            if (fileTransfer.getId() == fileTransferId) {
                return fileTransfer;
            }
        }

        return null;
    }

    private void updateFileMessageStatus(ChatRoom chatRoom, int fileTransferId) {
        for (var message : chatRoom.getMessages()) {
            if (message instanceof FileMessage fileMessage && fileMessage.getFileTransferId() == fileTransferId) {
                fileMessage.setStatus(FileTransferStatus.DOWNLOADED);
            }
        }
    }

    public void banUser(User admin, String targetUsername)
            throws PermissionDeniedException, UserNotFoundException, UserAlreadyBannedException, UserBannedException,
            CsvWriteException {
        if (admin == null || !admin.isAdmin()) {
            throw new PermissionDeniedException("Doar adminii pot bana useri.");
        }

        if (admin.isBanned()) {
            throw new UserBannedException("Adminul " + admin.getUsername() + " este banat.");
        }

        User target = findUserByUsername(targetUsername);

        if (target == null) {
            throw new UserNotFoundException("Nu exista userul cu username-ul: " + targetUsername);
        }

        if (target.isAdmin()) {
            throw new PermissionDeniedException("Un admin nu poate bana alt admin.");
        }

        if (target.isBanned()) {
            throw new UserAlreadyBannedException("Userul " + targetUsername + " este deja banat.");
        }

        Session targetSession = findSessionByUser(target);
        if (targetSession != null) {
            if (targetSession.getCurrentChatRoom() != null) {
                targetSession.getCurrentChatRoom().removeMember(target);
            }

            sessions.remove(targetSession);
        }

        target.setStatus(UserStatus.BANNED);
        persistUsers();
        auditService.logAction("BAN_USER", admin, "target=" + target.getUsername());
    }

    private void persistUsers() throws CsvWriteException {
        if (userCsvRepository != null) {
            userCsvRepository.saveAll(users);
        }
    }

    private void persistMessages() throws CsvWriteException {
        if (messageCsvRepository != null) {
            messageCsvRepository.saveAll(chatRooms);
        }
    }

    private void persistFileTransfers() throws CsvWriteException {
        if (fileTransferCsvRepository != null) {
            fileTransferCsvRepository.saveAll(fileTransfers);
        }
    }
}
