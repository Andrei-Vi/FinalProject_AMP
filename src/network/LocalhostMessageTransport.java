package network;

import exception.NetworkException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class LocalhostMessageTransport {
    private static final String HOST = "127.0.0.1";
    private static final int TIMEOUT_SECONDS = 3;

    public String send(String message) throws NetworkException {
        ExecutorService executorService = Executors.newSingleThreadExecutor();

        try (ServerSocket serverSocket = new ServerSocket(0, 1, InetAddress.getByName(HOST))) {
            Future<String> receivedMessage = executorService.submit(receiveMessage(serverSocket));

            try (Socket clientSocket = new Socket(HOST, serverSocket.getLocalPort());
                 PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)) {
                writer.println(message);
            }

            return receivedMessage.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (IOException exception) {
            throw new NetworkException("Eroare la trimiterea mesajului prin localhost: " + exception.getMessage());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new NetworkException("Trimiterea mesajului prin localhost a fost intrerupta.");
        } catch (ExecutionException | TimeoutException exception) {
            throw new NetworkException("Mesajul nu a fost primit prin localhost: " + exception.getMessage());
        } finally {
            executorService.shutdownNow();
        }
    }

    private Callable<String> receiveMessage(ServerSocket serverSocket) {
        return () -> {
            try (Socket serverSideSocket = serverSocket.accept();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(serverSideSocket.getInputStream()))) {
                return reader.readLine();
            }
        };
    }
}
