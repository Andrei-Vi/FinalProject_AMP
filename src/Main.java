import app.ChatApplicationContext;
import app.ChatApplicationFactory;
import exception.CsvReadException;
import exception.CsvWriteException;
import exception.PermissionDeniedException;
import model.ChatRoom;
import model.FileTransfer;
import model.Message;
import model.Session;
import model.TextMessage;
import model.User;
import service.AuditService;
import service.ChatService;

import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

public class Main {
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        Path dataDirectory = args.length > 0 ? Path.of(args[0]) : Path.of("data");

        try {
            ChatApplicationContext context = new ChatApplicationFactory().create(dataDirectory);
            runApplication(context.getChatService(), context.getAuditService());
        } catch (CsvReadException | CsvWriteException exception) {
            System.out.println("Aplicatia nu a putut incarca datele CSV: " + exception.getMessage());
        }
    }

    private static void runApplication(ChatService chatService, AuditService auditService) {
        boolean running = true;

        while (running) {
            printStartMenu();
            String option = readLine("Optiune: ");

            switch (option) {
                case "1":
                    Session session = login(chatService);
                    if (session != null) {
                        running = runUserMenu(chatService, auditService, session);
                    }
                    break;
                case "2":
                    printDemoAccounts(chatService);
                    break;
                case "0":
                    running = false;
                    break;
                default:
                    System.out.println("Optiune invalida.");
            }
        }

        System.out.println("Aplicatia s-a inchis.");
    }

    private static void printStartMenu() {
        System.out.println();
        System.out.println("=== Chat App ===");
        System.out.println("1. Login");
        System.out.println("2. Afiseaza conturi demo");
        System.out.println("0. Exit");
    }

    private static void printDemoAccounts(ChatService chatService) {
        System.out.println();
        System.out.println("Conturi disponibile:");
        for (User user : chatService.getUsers()) {
            System.out.println(user.getUsername() + " / " + user.getPassword() + " (" + user.getRole() + ")");
        }
    }

    private static Session login(ChatService chatService) {
        String username = readLine("Username: ");
        String password = readLine("Password: ");

        try {
            Session session = chatService.login(username, password);
            System.out.println("Login reusit. Bun venit, " + session.getUser().getUsername() + "!");
            return session;
        } catch (Exception exception) {
            System.out.println("Login esuat: " + exception.getMessage());
            return null;
        }
    }

    private static boolean runUserMenu(ChatService chatService, AuditService auditService, Session session) {
        boolean loggedIn = true;

        while (loggedIn) {
            printUserMenu(session);
            String option = readLine("Optiune: ");

            switch (option) {
                case "1":
                    listRooms(chatService);
                    break;
                case "2":
                    joinRoom(chatService, session.getUser());
                    break;
                case "3":
                    sendMessage(chatService, session.getUser());
                    break;
                case "4":
                    sendFile(chatService, session.getUser());
                    break;
                case "5":
                    showCurrentRoomMessages(session);
                    break;
                case "6":
                    leaveRoom(chatService, session.getUser());
                    break;
                case "7":
                    banUser(chatService, session.getUser());
                    break;
                case "8":
                    listFileTransfers(chatService, session.getUser());
                    break;
                case "9":
                    downloadFile(chatService, session.getUser());
                    break;
                case "10":
                    showAuditLogs(auditService);
                    break;
                case "11":
                    logout(chatService, session.getUser());
                    loggedIn = false;
                    break;
                case "0":
                    logout(chatService, session.getUser());
                    return false;
                default:
                    System.out.println("Optiune invalida.");
            }
        }

        return true;
    }

    private static void printUserMenu(Session session) {
        String roomName = session.getCurrentChatRoom() == null ? "niciuna" : session.getCurrentChatRoom().getName();

        System.out.println();
        System.out.println("=== Meniu User ===");
        System.out.println("User: " + session.getUser().getUsername() + " | Camera curenta: " + roomName);
        System.out.println("1. Afiseaza camere");
        System.out.println("2. Intra intr-o camera");
        System.out.println("3. Trimite mesaj");
        System.out.println("4. Trimite fisier");
        System.out.println("5. Afiseaza mesajele camerei curente");
        System.out.println("6. Iesi din camera curenta");
        System.out.println("7. Baneaza user (admin)");
        System.out.println("8. Afiseaza fisierele camerei curente");
        System.out.println("9. Descarca fisier");
        System.out.println("10. Afiseaza audit logs");
        System.out.println("11. Logout");
        System.out.println("0. Exit aplicatie");
    }

    private static void listRooms(ChatService chatService) {
        List<ChatRoom> chatRooms = chatService.getChatRooms();

        System.out.println();
        System.out.println("Camere disponibile:");
        for (ChatRoom chatRoom : chatRooms) {
            System.out.println(chatRoom.getId() + ". " + chatRoom.getName());
        }
    }

    private static void joinRoom(ChatService chatService, User user) {
        listRooms(chatService);
        int roomId = readInt("Alege id camera: ");

        try {
            chatService.joinRoom(user, roomId);
            System.out.println("Ai intrat in camera.");
        } catch (Exception exception) {
            System.out.println("Nu ai putut intra in camera: " + exception.getMessage());
        }
    }

    private static void sendMessage(ChatService chatService, User user) {
        String content = readLine("Mesaj: ");

        try {
            TextMessage message = chatService.sendMessage(user, content);
            System.out.println("Mesaj trimis si primit prin localhost:");
            System.out.println(message.formatForDisplay());
        } catch (Exception exception) {
            System.out.println("Mesajul nu a fost trimis: " + exception.getMessage());
        }
    }

    private static void sendFile(ChatService chatService, User user) {
        String filePath = readLine("Path fisier sursa: ");

        if (filePath == null || filePath.trim().isEmpty()) {
            System.out.println("Path-ul fisierului nu poate fi gol.");
            return;
        }

        Path sourcePath = Path.of(filePath);
        if (sourcePath.getFileName() == null) {
            System.out.println("Path-ul fisierului nu este valid.");
            return;
        }

        String fileName = sourcePath.getFileName().toString();

        try {
            FileTransfer fileTransfer = chatService.sendFile(user, fileName, filePath);
            System.out.println("Fisier trimis. Transfer id: " + fileTransfer.getId() + ", status: " + fileTransfer.getStatus());
            System.out.println("Il poti descarca din meniul 9.");
        } catch (Exception exception) {
            System.out.println("Fisierul nu a fost trimis: " + exception.getMessage());
        }
    }

    private static void showCurrentRoomMessages(Session session) {
        ChatRoom currentRoom = session.getCurrentChatRoom();

        if (currentRoom == null) {
            System.out.println("Nu esti intr-o camera.");
            return;
        }

        List<Message> messages = currentRoom.getMessages();

        if (messages.isEmpty()) {
            System.out.println("Camera nu are mesaje inca.");
            return;
        }

        System.out.println();
        System.out.println("Mesaje in camera " + currentRoom.getName() + ":");
        for (Message message : messages) {
            System.out.println(message.formatForDisplay());
        }
    }

    private static void leaveRoom(ChatService chatService, User user) {
        try {
            chatService.leaveRoom(user);
            System.out.println("Ai iesit din camera.");
        } catch (Exception exception) {
            System.out.println("Nu ai putut iesi din camera: " + exception.getMessage());
        }
    }

    private static void banUser(ChatService chatService, User admin) {
        String targetUsername = readLine("Username de banat: ");

        try {
            chatService.banUser(admin, targetUsername);
            System.out.println("User banat: " + targetUsername);
        } catch (PermissionDeniedException exception) {
            System.out.println("Nu ai permisiune: " + exception.getMessage());
        } catch (Exception exception) {
            System.out.println("Ban esuat: " + exception.getMessage());
        }
    }

    private static void listFileTransfers(ChatService chatService, User user) {
        try {
            List<FileTransfer> fileTransfers = chatService.getFileTransfersForCurrentRoom(user);

            if (fileTransfers.isEmpty()) {
                System.out.println("Camera curenta nu are fisiere trimise.");
                return;
            }

            System.out.println();
            System.out.println("Fisiere in camera curenta:");
            for (FileTransfer fileTransfer : fileTransfers) {
                System.out.println("id=" + fileTransfer.getId()
                        + " | " + fileTransfer.getFileName()
                        + " | status=" + fileTransfer.getStatus()
                        + " | source=" + fileTransfer.getFilePath());
            }
        } catch (Exception exception) {
            System.out.println("Nu se pot afisa fisierele: " + exception.getMessage());
        }
    }

    private static void downloadFile(ChatService chatService, User user) {
        try {
            List<FileTransfer> fileTransfers = chatService.getFileTransfersForCurrentRoom(user);

            if (fileTransfers.isEmpty()) {
                System.out.println("Camera curenta nu are fisiere trimise.");
                return;
            }

            System.out.println();
            System.out.println("Fisiere in camera curenta:");
            for (FileTransfer fileTransfer : fileTransfers) {
                System.out.println("id=" + fileTransfer.getId()
                        + " | " + fileTransfer.getFileName()
                        + " | status=" + fileTransfer.getStatus()
                        + " | source=" + fileTransfer.getFilePath());
            }

            int fileTransferId = readInt("Id transfer de descarcat: ");
            String destinationDirectory = readLine("Folder destinatie: ");
            Path downloadedPath = chatService.downloadFile(user, fileTransferId, destinationDirectory);
            System.out.println("Fisier descarcat la: " + downloadedPath);
            System.out.println("Mesajul fisierului are acum tag-ul DOWNLOADED.");
        } catch (Exception exception) {
            System.out.println("Download esuat: " + exception.getMessage());
        }
    }

    private static void showAuditLogs(AuditService auditService) {
        List<String> auditLogs = auditService.getAuditLogs();

        if (auditLogs.isEmpty()) {
            System.out.println("Nu exista audit logs in sesiunea curenta.");
            return;
        }

        System.out.println();
        System.out.println("Audit logs:");
        for (String log : auditLogs) {
            System.out.println(log);
        }
    }

    private static void logout(ChatService chatService, User user) {
        try {
            chatService.logout(user);
            System.out.println("Logout reusit.");
        } catch (Exception exception) {
            System.out.println("Logout esuat: " + exception.getMessage());
        }
    }

    private static String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine();
    }

    private static int readInt(String prompt) {
        while (true) {
            String value = readLine(prompt);

            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException exception) {
                System.out.println("Te rog introdu un numar.");
            }
        }
    }
}
