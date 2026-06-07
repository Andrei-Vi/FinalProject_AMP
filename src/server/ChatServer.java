package server;

import app.ChatApplicationContext;
import app.ChatApplicationFactory;
import model.ChatRoom;
import model.FileTransfer;
import model.Message;
import model.Session;
import model.User;
import network.SocketProtocol;
import service.AuditService;
import service.ChatService;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer {
    private static final int DEFAULT_PORT = 5000;

    public static void main(String[] args) {
        int port = readPort(args);
        Path dataDirectory = args.length > 1 ? Path.of(args[1]) : Path.of("data");

        try {
            ChatApplicationContext context = new ChatApplicationFactory().create(dataDirectory);
            startServer(port, context.getChatService(), context.getAuditService());
        } catch (Exception exception) {
            System.out.println("Serverul nu a putut porni: " + exception.getMessage());
        }
    }

    private static int readPort(String[] args) {
        if (args.length > 0) {
            return Integer.parseInt(args[0]);
        }

        Scanner scanner = new Scanner(System.in);
        System.out.print("Port server [" + DEFAULT_PORT + "]: ");
        String value = scanner.nextLine();

        if (value.trim().isEmpty()) {
            return DEFAULT_PORT;
        }

        return Integer.parseInt(value);
    }

    private static void startServer(int port, ChatService chatService, AuditService auditService) throws IOException {
        ExecutorService executorService = Executors.newCachedThreadPool();
        List<ClientHandler> connectedClients = Collections.synchronizedList(new ArrayList<>());

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server pornit pe localhost:" + port);
            System.out.println("Porneste clientul cu: java -cp out client.ChatClient 127.0.0.1 " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client conectat: " + clientSocket.getRemoteSocketAddress());
                executorService.submit(new ClientHandler(clientSocket, chatService, auditService, connectedClients));
            }
        } finally {
            executorService.shutdownNow();
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket socket;
        private final ChatService chatService;
        private final AuditService auditService;
        private final List<ClientHandler> connectedClients;
        private volatile User currentUser;
        private volatile Session currentSession;
        private volatile DataOutputStream output;

        ClientHandler(Socket socket, ChatService chatService, AuditService auditService,
                      List<ClientHandler> connectedClients) {
            this.socket = socket;
            this.chatService = chatService;
            this.auditService = auditService;
            this.connectedClients = connectedClients;
        }

        @Override
        public void run() {
            try (Socket clientSocket = socket;
                 DataInputStream input = new DataInputStream(clientSocket.getInputStream());
                 DataOutputStream output = new DataOutputStream(clientSocket.getOutputStream())) {
                this.output = output;
                connectedClients.add(this);
                boolean connected = true;

                while (connected) {
                    SocketProtocol.Request request = SocketProtocol.readRequest(input);
                    ServerResult result = handle(request);
                    SocketProtocol.sendResponse(output, result.success, result.message);
                    connected = !result.closeConnection;
                }
            } catch (IOException exception) {
                System.out.println("Client deconectat: " + exception.getMessage());
            } finally {
                connectedClients.remove(this);
            }
        }

        private ServerResult handle(SocketProtocol.Request request) {
            try {
                synchronized (chatService) {
                    return switch (request.getAction()) {
                        case "ACCOUNTS" -> success(formatAccounts());
                        case "REGISTER" -> register(request.getField(0), request.getField(1));
                        case "LOGIN" -> login(request.getField(0), request.getField(1));
                        case "IS_ADMIN" -> success(String.valueOf(currentUser != null && currentUser.isAdmin()));
                        case "STATUS" -> success(formatStatus());
                        case "LIST_ROOMS" -> success(formatRooms());
                        case "JOIN" -> joinRoom(request.getField(0));
                        case "SEND_MESSAGE" -> sendMessage(request.getField(0));
                        case "SEND_FILE" -> sendFile(request.getField(0));
                        case "SHOW_MESSAGES" -> success(formatCurrentRoomMessages());
                        case "LEAVE" -> leaveRoom();
                        case "BAN" -> banUser(request.getField(0));
                        case "LIST_BANNED_USERS" -> success(formatBannedUsers());
                        case "UNBAN" -> unbanUser(request.getField(0));
                        case "LIST_FILES" -> success(formatCurrentRoomFiles());
                        case "DOWNLOAD" -> downloadFile(request.getField(0), request.getField(1));
                        case "AUDIT" -> showAuditLogs();
                        case "CLEAR_AUDIT" -> clearAuditLogs();
                        case "LOGOUT" -> logout(false);
                        case "EXIT" -> logout(true);
                        default -> failure("Comanda necunoscuta: " + request.getAction());
                    };
                }
            } catch (Exception exception) {
                return failure(exception.getMessage());
            }
        }

        private ServerResult register(String username, String password) throws Exception {
            User user = chatService.createRegularAccount(username, password);
            return success("Cont creat pentru " + user.getUsername() + ". Te poti loga acum.");
        }

        private ServerResult login(String username, String password) throws Exception {
            currentSession = chatService.login(username, password);
            currentUser = currentSession.getUser();
            return success("Login reusit. Bun venit, " + currentUser.getUsername() + "!");
        }

        private ServerResult joinRoom(String roomIdText) throws Exception {
            requireLogin();
            chatService.joinRoom(currentUser, Integer.parseInt(roomIdText));
            broadcastToCurrentRoom("[INFO] " + currentUser.getUsername() + " a intrat in camera.");
            return success("Ai intrat in camera.");
        }

        private ServerResult sendMessage(String content) throws Exception {
            requireLogin();
            Message message = chatService.sendMessage(currentUser, content);
            broadcastToCurrentRoom("[MESAJ NOU]\n" + message.formatForDisplay());
            return success("Mesaj trimis prin server:\n" + message.formatForDisplay());
        }

        private ServerResult sendFile(String filePath) throws Exception {
            requireLogin();
            Path sourcePath = Path.of(filePath);

            if (sourcePath.getFileName() == null) {
                return failure("Path-ul fisierului nu este valid.");
            }

            FileTransfer fileTransfer = chatService.sendFile(currentUser, sourcePath.getFileName().toString(), filePath);
            broadcastToCurrentRoom("[FISIER NOU]\n" + currentUser.getUsername()
                    + " a trimis fisierul: " + fileTransfer.getFileName() + " [SENT]");
            return success("Fisier trimis. Transfer id: " + fileTransfer.getId()
                    + ", status: " + fileTransfer.getStatus());
        }

        private ServerResult leaveRoom() throws Exception {
            requireLogin();
            chatService.leaveRoom(currentUser);
            return success("Ai iesit din camera.");
        }

        private ServerResult banUser(String targetUsername) throws Exception {
            requireLogin();
            requireAdmin();
            chatService.banUser(currentUser, targetUsername);
            return success("User banat: " + targetUsername);
        }

        private ServerResult unbanUser(String targetUsername) throws Exception {
            requireLogin();
            requireAdmin();
            chatService.unbanUser(currentUser, targetUsername);
            return success("User debanat: " + targetUsername);
        }

        private ServerResult downloadFile(String fileTransferIdText, String destinationDirectory) throws Exception {
            requireLogin();
            Path downloadedPath = chatService.downloadFile(currentUser, Integer.parseInt(fileTransferIdText),
                    destinationDirectory);
            broadcastToCurrentRoom("[FISIER ACTUALIZAT]\nTransferul " + fileTransferIdText
                    + " a fost descarcat de " + currentUser.getUsername() + " [DOWNLOADED]");
            return success("Fisier descarcat la: " + downloadedPath
                    + "\nMesajul fisierului are acum tag-ul DOWNLOADED.");
        }

        private ServerResult logout(boolean closeConnection) throws Exception {
            if (currentUser != null) {
                chatService.logout(currentUser);
            }

            currentUser = null;
            currentSession = null;

            if (closeConnection) {
                return new ServerResult(true, "Conexiune inchisa.", true);
            }

            return success("Logout reusit.");
        }

        private String formatStatus() {
            if (currentUser == null) {
                return "Nu esti logat.";
            }

            String roomName = currentSession.getCurrentChatRoom() == null
                    ? "niciuna"
                    : currentSession.getCurrentChatRoom().getName();
            return "User: " + currentUser.getUsername()
                    + " | Rol: " + currentUser.getRole()
                    + " | Camera curenta: " + roomName;
        }

        private String formatAccounts() {
            StringBuilder builder = new StringBuilder("Conturi disponibile:");

            for (User user : chatService.getUsers()) {
                builder.append(System.lineSeparator())
                        .append(user.getUsername())
                        .append(" / ")
                        .append(user.getPassword())
                        .append(" (")
                        .append(user.getRole())
                        .append(")");
            }

            return builder.toString();
        }

        private String formatRooms() {
            StringBuilder builder = new StringBuilder("Camere disponibile:");

            for (ChatRoom chatRoom : chatService.getChatRooms()) {
                builder.append(System.lineSeparator())
                        .append(chatRoom.getId())
                        .append(". ")
                        .append(chatRoom.getName());
            }

            return builder.toString();
        }

        private String formatCurrentRoomMessages() throws Exception {
            requireLogin();

            if (currentSession.getCurrentChatRoom() == null) {
                return "Nu esti intr-o camera.";
            }

            List<Message> messages = currentSession.getCurrentChatRoom().getMessages();

            if (messages.isEmpty()) {
                return "Camera nu are mesaje inca.";
            }

            StringBuilder builder = new StringBuilder("Mesaje in camera ")
                    .append(currentSession.getCurrentChatRoom().getName())
                    .append(":");

            for (Message message : messages) {
                builder.append(System.lineSeparator()).append(message.formatForDisplay());
            }

            return builder.toString();
        }

        private String formatCurrentRoomFiles() throws Exception {
            requireLogin();
            List<FileTransfer> fileTransfers = chatService.getFileTransfersForCurrentRoom(currentUser);

            if (fileTransfers.isEmpty()) {
                return "Camera curenta nu are fisiere trimise.";
            }

            StringBuilder builder = new StringBuilder("Fisiere in camera curenta:");

            for (FileTransfer fileTransfer : fileTransfers) {
                builder.append(System.lineSeparator())
                        .append("id=")
                        .append(fileTransfer.getId())
                        .append(" | ")
                        .append(fileTransfer.getFileName())
                        .append(" | status=")
                        .append(fileTransfer.getStatus())
                        .append(" | source=")
                        .append(fileTransfer.getFilePath());
            }

            return builder.toString();
        }

        private String formatBannedUsers() {
            requireAdmin();
            List<User> bannedUsers = chatService.getBannedUsers();

            if (bannedUsers.isEmpty()) {
                return "niciun user nu a primit ban";
            }

            StringBuilder builder = new StringBuilder("Useri banati:");

            for (User user : bannedUsers) {
                builder.append(System.lineSeparator())
                        .append("- ")
                        .append(user.getUsername());
            }

            return builder.toString();
        }

        private ServerResult showAuditLogs() {
            requireAdmin();
            List<String> auditLogs = auditService.getAuditLogs();

            if (auditLogs.isEmpty()) {
                return success("Nu exista audit logs.");
            }

            return success(String.join(System.lineSeparator(), auditLogs));
        }

        private ServerResult clearAuditLogs() throws Exception {
            requireAdmin();
            auditService.clearAuditLogs();
            return success("Audit logs sterse.");
        }

        private void requireLogin() {
            if (currentUser == null || currentSession == null) {
                throw new IllegalStateException("Trebuie sa fii logat pentru aceasta actiune.");
            }
        }

        private void requireAdmin() {
            requireLogin();

            if (!currentUser.isAdmin()) {
                throw new IllegalStateException("Doar adminii pot folosi aceasta optiune.");
            }
        }

        private void broadcastToCurrentRoom(String message) {
            Integer roomId = getCurrentRoomId();

            if (roomId == null) {
                return;
            }

            synchronized (connectedClients) {
                for (ClientHandler clientHandler : connectedClients) {
                    if (clientHandler != this && clientHandler.isInRoom(roomId)) {
                        clientHandler.sendEvent(message);
                    }
                }
            }
        }

        private boolean isInRoom(int roomId) {
            Integer currentRoomId = getCurrentRoomId();
            return currentRoomId != null && currentRoomId == roomId;
        }

        private Integer getCurrentRoomId() {
            if (currentSession == null || currentSession.getCurrentChatRoom() == null) {
                return null;
            }

            return currentSession.getCurrentChatRoom().getId();
        }

        private void sendEvent(String message) {
            try {
                if (output != null) {
                    SocketProtocol.sendEvent(output, message);
                }
            } catch (IOException exception) {
                System.out.println("Nu s-a putut trimite eveniment catre client: " + exception.getMessage());
            }
        }

        private ServerResult success(String message) {
            return new ServerResult(true, message, false);
        }

        private ServerResult failure(String message) {
            return new ServerResult(false, "Eroare: " + message, false);
        }
    }

    private static class ServerResult {
        private final boolean success;
        private final String message;
        private final boolean closeConnection;

        ServerResult(boolean success, String message, boolean closeConnection) {
            this.success = success;
            this.message = message;
            this.closeConnection = closeConnection;
        }
    }
}
