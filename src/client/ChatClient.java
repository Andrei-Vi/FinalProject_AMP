package client;

import network.SocketProtocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ChatClient {
    private static final Scanner scanner = new Scanner(System.in);
    private static final String DEFAULT_HOST = "127.0.0.1";
    private static final int DEFAULT_PORT = 5000;

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : readHost();
        int port = args.length > 1 ? Integer.parseInt(args[1]) : readPort();

        try (Socket socket = new Socket(host, port);
             DataOutputStream output = new DataOutputStream(socket.getOutputStream());
             DataInputStream input = new DataInputStream(socket.getInputStream())) {
            System.out.println("Conectat la server " + host + ":" + port);
            BlockingQueue<SocketProtocol.Response> responses = new LinkedBlockingQueue<>();
            Thread listenerThread = new Thread(() -> listenToServer(input, responses), "server-listener");
            listenerThread.setDaemon(true);
            listenerThread.start();

            runClient(output, responses);
        } catch (IOException exception) {
            System.out.println("Clientul nu s-a putut conecta: " + exception.getMessage());
        }
    }

    private static String readHost() {
        String host = readLine("Adresa server [" + DEFAULT_HOST + "]: ");

        if (host.trim().isEmpty()) {
            return DEFAULT_HOST;
        }

        return host;
    }

    private static int readPort() {
        String port = readLine("Port [" + DEFAULT_PORT + "]: ");

        if (port.trim().isEmpty()) {
            return DEFAULT_PORT;
        }

        return Integer.parseInt(port);
    }

    private static void listenToServer(DataInputStream input, BlockingQueue<SocketProtocol.Response> responses) {
        try {
            while (true) {
                SocketProtocol.ServerPacket packet = SocketProtocol.readServerPacket(input);

                if (packet.isResponse()) {
                    responses.offer(packet.toResponse());
                } else if (packet.isEvent()) {
                    System.out.println();
                    System.out.println(packet.getMessage());
                }
            }
        } catch (IOException exception) {
            responses.offer(new SocketProtocol.Response(false,
                    "Conexiunea cu serverul s-a inchis: " + exception.getMessage()));
        }
    }

    private static void runClient(DataOutputStream output, BlockingQueue<SocketProtocol.Response> responses)
            throws IOException {
        boolean connected = true;
        boolean loggedIn = false;
        boolean admin = false;

        while (connected) {
            if (!loggedIn) {
                printStartMenu();
                String option = readLine("Optiune: ");

                switch (option) {
                    case "1" -> createAccount(output, responses);
                    case "2" -> {
                        loggedIn = login(output, responses);
                        admin = loggedIn && isAdmin(output, responses);
                    }
                    case "3" -> sendAndPrint(output, responses, "ACCOUNTS");
                    case "0" -> {
                        sendAndPrint(output, responses, "EXIT");
                        connected = false;
                    }
                    default -> System.out.println("Optiune invalida.");
                }
            } else {
                printUserMenu(output, responses, admin);
                String option = readLine("Optiune: ");
                String selectedOption = admin ? option : mapRegularMenuOption(option);

                switch (selectedOption) {
                    case "1" -> sendAndPrint(output, responses, "LIST_ROOMS");
                    case "2" -> joinRoom(output, responses);
                    case "3" -> sendMessage(output, responses);
                    case "4" -> sendFile(output, responses);
                    case "5" -> sendAndPrint(output, responses, "SHOW_MESSAGES");
                    case "6" -> sendAndPrint(output, responses, "LEAVE");
                    case "7" -> {
                        if (admin) {
                            banUser(output, responses);
                        } else {
                            System.out.println("Optiune invalida.");
                        }
                    }
                    case "8" -> {
                        if (admin) {
                            unbanUser(output, responses);
                        } else {
                            System.out.println("Optiune invalida.");
                        }
                    }
                    case "9" -> sendAndPrint(output, responses, "LIST_FILES");
                    case "10" -> downloadFile(output, responses);
                    case "11" -> {
                        if (admin) {
                            sendAndPrint(output, responses, "AUDIT");
                        } else {
                            System.out.println("Optiune invalida.");
                        }
                    }
                    case "12" -> {
                        if (admin) {
                            clearAuditLogs(output, responses);
                        } else {
                            System.out.println("Optiune invalida.");
                        }
                    }
                    case "13" -> {
                        SocketProtocol.Response response = send(output, responses, "LOGOUT");
                        System.out.println(response.getMessage());
                        loggedIn = !response.isSuccess();
                        admin = false;
                    }
                    case "0" -> {
                        sendAndPrint(output, responses, "EXIT");
                        connected = false;
                    }
                    default -> System.out.println("Optiune invalida.");
                }
            }
        }
    }

    private static String mapRegularMenuOption(String option) {
        return switch (option) {
            case "7" -> "9";
            case "8" -> "10";
            case "9" -> "13";
            default -> option;
        };
    }

    private static void createAccount(DataOutputStream output, BlockingQueue<SocketProtocol.Response> responses)
            throws IOException {
        String username = readLine("Username nou: ");
        String password = readLine("Parola noua: ");
        sendAndPrint(output, responses, "REGISTER", username, password);
    }

    private static boolean login(DataOutputStream output, BlockingQueue<SocketProtocol.Response> responses)
            throws IOException {
        String username = readLine("Username: ");
        String password = readLine("Password: ");
        SocketProtocol.Response response = send(output, responses, "LOGIN", username, password);
        System.out.println(response.getMessage());
        return response.isSuccess();
    }

    private static boolean isAdmin(DataOutputStream output, BlockingQueue<SocketProtocol.Response> responses)
            throws IOException {
        SocketProtocol.Response response = send(output, responses, "IS_ADMIN");
        return response.isSuccess() && Boolean.parseBoolean(response.getMessage());
    }

    private static void joinRoom(DataOutputStream output, BlockingQueue<SocketProtocol.Response> responses)
            throws IOException {
        sendAndPrint(output, responses, "LIST_ROOMS");
        String roomId = readLine("Alege id camera: ");
        sendAndPrint(output, responses, "JOIN", roomId);
    }

    private static void sendMessage(DataOutputStream output, BlockingQueue<SocketProtocol.Response> responses)
            throws IOException {
        String content = readLine("Mesaj: ");
        sendAndPrint(output, responses, "SEND_MESSAGE", content);
    }

    private static void sendFile(DataOutputStream output, BlockingQueue<SocketProtocol.Response> responses)
            throws IOException {
        String filePath = readLine("Path fisier sursa: ");
        sendAndPrint(output, responses, "SEND_FILE", filePath);
    }

    private static void banUser(DataOutputStream output, BlockingQueue<SocketProtocol.Response> responses)
            throws IOException {
        String targetUsername = readLine("Username de banat: ");
        sendAndPrint(output, responses, "BAN", targetUsername);
    }

    private static void unbanUser(DataOutputStream output, BlockingQueue<SocketProtocol.Response> responses)
            throws IOException {
        SocketProtocol.Response listResponse = send(output, responses, "LIST_BANNED_USERS");
        System.out.println(listResponse.getMessage());

        if (!listResponse.isSuccess() || listResponse.getMessage().contains("niciun user nu a primit ban")) {
            return;
        }

        String targetUsername = readLine("Username de debanat: ");
        sendAndPrint(output, responses, "UNBAN", targetUsername);
    }

    private static void clearAuditLogs(DataOutputStream output, BlockingQueue<SocketProtocol.Response> responses)
            throws IOException {
        String confirmation = readLine("Stergi toate audit logs? Scrie DA pentru confirmare: ");

        if (!"DA".equalsIgnoreCase(confirmation.trim())) {
            System.out.println("Stergere anulata.");
            return;
        }

        sendAndPrint(output, responses, "CLEAR_AUDIT");
    }

    private static void downloadFile(DataOutputStream output, BlockingQueue<SocketProtocol.Response> responses)
            throws IOException {
        SocketProtocol.Response listResponse = send(output, responses, "LIST_FILES");
        System.out.println(listResponse.getMessage());

        if (!listResponse.isSuccess() || listResponse.getMessage().contains("nu are fisiere")) {
            return;
        }

        String fileTransferId = readLine("Id transfer de descarcat: ");
        String destinationDirectory = readLine("Folder destinatie: ");
        sendAndPrint(output, responses, "DOWNLOAD", fileTransferId, destinationDirectory);
    }

    private static void printStartMenu() {
        System.out.println();
        System.out.println("=== Chat Client ===");
        System.out.println("1. Creeaza cont");
        System.out.println("2. Login");
        System.out.println("3. Afiseaza conturi");
        System.out.println("0. Exit");
    }

    private static void printUserMenu(DataOutputStream output, BlockingQueue<SocketProtocol.Response> responses,
                                      boolean admin) throws IOException {
        SocketProtocol.Response status = send(output, responses, "STATUS");

        System.out.println();
        System.out.println(admin ? "=== Meniu Admin ===" : "=== Meniu User ===");
        System.out.println(status.getMessage());
        System.out.println("1. Afiseaza camere");
        System.out.println("2. Intra intr-o camera");
        System.out.println("3. Trimite mesaj");
        System.out.println("4. Trimite fisier");
        System.out.println("5. Afiseaza mesajele camerei curente");
        System.out.println("6. Iesi din camera curenta");

        if (admin) {
            System.out.println("7. Baneaza user");
            System.out.println("8. Debaneaza user");
            System.out.println("9. Afiseaza fisierele camerei curente");
            System.out.println("10. Descarca fisier");
            System.out.println("11. Afiseaza audit logs");
            System.out.println("12. Sterge audit logs");
            System.out.println("13. Logout din cont");
        } else {
            System.out.println("7. Afiseaza fisierele camerei curente");
            System.out.println("8. Descarca fisier");
            System.out.println("9. Logout din cont");
        }

        System.out.println("0. Exit aplicatie");
    }

    private static void sendAndPrint(DataOutputStream output, BlockingQueue<SocketProtocol.Response> responses,
                                     String action, String... fields) throws IOException {
        SocketProtocol.Response response = send(output, responses, action, fields);
        System.out.println(response.getMessage());
    }

    private static SocketProtocol.Response send(DataOutputStream output,
                                                BlockingQueue<SocketProtocol.Response> responses,
                                                String action, String... fields) throws IOException {
        SocketProtocol.sendRequest(output, action, fields);

        try {
            return responses.take();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Asteptarea raspunsului de la server a fost intrerupta.");
        }
    }

    private static String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine();
    }
}
