package repository;

import exception.CsvReadException;
import exception.CsvWriteException;
import model.FileTransfer;
import model.User;
import model.enums.FileTransferStatus;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FileTransferCsvRepository {
    private static final String HEADER = "id,senderId,chatRoomId,fileName,filePath,status,timestamp";

    private final Path fileTransfersCsvPath;

    public FileTransferCsvRepository(String fileTransfersCsvPath) {
        this.fileTransfersCsvPath = Path.of(fileTransfersCsvPath);
    }

    public List<FileTransfer> findAll(List<User> users) throws CsvReadException {
        List<FileTransfer> fileTransfers = new ArrayList<>();

        for (String line : CsvUtils.readDataLines(fileTransfersCsvPath)) {
            if (line.trim().isEmpty()) {
                continue;
            }

            List<String> values = CsvUtils.parseLine(line);
            if (values.size() < 7) {
                throw new CsvReadException("Linie invalida in filetransfers.csv: " + line);
            }

            int id = parseInt(values.get(0), "id transfer");
            int senderId = parseInt(values.get(1), "senderId");
            int chatRoomId = parseInt(values.get(2), "chatRoomId");
            User sender = findUserById(users, senderId);

            if (sender == null) {
                throw new CsvReadException("Nu exista senderId=" + senderId + " pentru transferul " + id);
            }

            fileTransfers.add(new FileTransfer(
                    sender,
                    chatRoomId,
                    id,
                    FileTransferStatus.valueOf(values.get(5)),
                    values.get(4),
                    values.get(3),
                    LocalDateTime.parse(values.get(6))
            ));
        }

        return fileTransfers;
    }

    public void saveAll(List<FileTransfer> fileTransfers) throws CsvWriteException {
        List<String> lines = new ArrayList<>();

        for (FileTransfer fileTransfer : fileTransfers) {
            lines.add(CsvUtils.toCsvLine(
                    String.valueOf(fileTransfer.getId()),
                    String.valueOf(fileTransfer.getSender().getId()),
                    String.valueOf(fileTransfer.getChatRoomId()),
                    fileTransfer.getFileName(),
                    fileTransfer.getFilePath(),
                    fileTransfer.getStatus().name(),
                    fileTransfer.getTimestamp().toString()
            ));
        }

        CsvUtils.writeLines(fileTransfersCsvPath, HEADER, lines);
    }

    private User findUserById(List<User> users, int id) {
        for (User user : users) {
            if (user.getId() == id) {
                return user;
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
