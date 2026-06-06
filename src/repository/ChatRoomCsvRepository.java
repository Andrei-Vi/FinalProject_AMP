package repository;

import exception.CsvReadException;
import exception.CsvWriteException;
import model.ChatRoom;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ChatRoomCsvRepository {
    private static final String HEADER = "id,name";

    private final Path chatRoomsCsvPath;

    public ChatRoomCsvRepository(String chatRoomsCsvPath) {
        this.chatRoomsCsvPath = Path.of(chatRoomsCsvPath);
    }

    public List<ChatRoom> findAll() throws CsvReadException {
        List<ChatRoom> chatRooms = new ArrayList<>();

        for (String line : CsvUtils.readDataLines(chatRoomsCsvPath)) {
            if (line.trim().isEmpty()) {
                continue;
            }

            List<String> values = CsvUtils.parseLine(line);
            if (values.size() < 2) {
                throw new CsvReadException("Linie invalida in chatrooms.csv: " + line);
            }

            chatRooms.add(new ChatRoom(parseInt(values.get(0)), values.get(1)));
        }

        return chatRooms;
    }

    public void saveAll(List<ChatRoom> chatRooms) throws CsvWriteException {
        List<String> lines = new ArrayList<>();

        for (ChatRoom chatRoom : chatRooms) {
            lines.add(CsvUtils.toCsvLine(String.valueOf(chatRoom.getId()), chatRoom.getName()));
        }

        CsvUtils.writeLines(chatRoomsCsvPath, HEADER, lines);
    }

    private int parseInt(String value) throws CsvReadException {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new CsvReadException("Valoare invalida pentru id camera: " + value);
        }
    }
}
