package network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class SocketProtocol {
    private static final String PACKET_RESPONSE = "RESPONSE";
    private static final String PACKET_EVENT = "EVENT";

    private SocketProtocol() {
    }

    public static void sendRequest(DataOutputStream output, String action, String... fields) throws IOException {
        output.writeUTF(action);
        output.writeInt(fields.length);

        for (String field : fields) {
            output.writeUTF(field == null ? "" : field);
        }

        output.flush();
    }

    public static Request readRequest(DataInputStream input) throws IOException {
        String action = input.readUTF();
        int fieldCount = input.readInt();
        List<String> fields = new ArrayList<>();

        for (int index = 0; index < fieldCount; index++) {
            fields.add(input.readUTF());
        }

        return new Request(action, fields);
    }

    public static void sendResponse(DataOutputStream output, boolean success, String message) throws IOException {
        synchronized (output) {
            output.writeUTF(PACKET_RESPONSE);
            output.writeBoolean(success);
            output.writeUTF(message == null ? "" : message);
            output.flush();
        }
    }

    public static void sendEvent(DataOutputStream output, String message) throws IOException {
        synchronized (output) {
            output.writeUTF(PACKET_EVENT);
            output.writeUTF(message == null ? "" : message);
            output.flush();
        }
    }

    public static Response readResponse(DataInputStream input) throws IOException {
        ServerPacket packet = readServerPacket(input);

        if (!packet.isResponse()) {
            throw new IOException("Se astepta RESPONSE, dar s-a primit " + packet.getType());
        }

        return new Response(packet.isSuccess(), packet.getMessage());
    }

    public static ServerPacket readServerPacket(DataInputStream input) throws IOException {
        String packetType = input.readUTF();

        if (PACKET_RESPONSE.equals(packetType)) {
            return new ServerPacket(packetType, input.readBoolean(), input.readUTF());
        }

        if (PACKET_EVENT.equals(packetType)) {
            return new ServerPacket(packetType, true, input.readUTF());
        }

        throw new IOException("Tip de pachet necunoscut: " + packetType);
    }

    public static class Request {
        private final String action;
        private final List<String> fields;

        public Request(String action, List<String> fields) {
            this.action = action;
            this.fields = fields;
        }

        public String getAction() {
            return action;
        }

        public String getField(int index) {
            return fields.get(index);
        }
    }

    public static class Response {
        private final boolean success;
        private final String message;

        public Response(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class ServerPacket {
        private final String type;
        private final boolean success;
        private final String message;

        public ServerPacket(String type, boolean success, String message) {
            this.type = type;
            this.success = success;
            this.message = message;
        }

        public boolean isResponse() {
            return PACKET_RESPONSE.equals(type);
        }

        public boolean isEvent() {
            return PACKET_EVENT.equals(type);
        }

        public String getType() {
            return type;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public Response toResponse() {
            return new Response(success, message);
        }
    }
}
