package repository;

import exception.CsvReadException;
import exception.CsvWriteException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.List;

public class AuditCsvRepository {
    private final Path auditCsvPath;

    public AuditCsvRepository(String auditCsvPath) {
        this.auditCsvPath = Path.of(auditCsvPath);
    }

    public void save(String action, String username, String details) throws CsvWriteException {
        String line = toCsvLine(LocalDateTime.now().toString(), action, username, details);

        try {
            Files.writeString(
                    auditCsvPath,
                    line + System.lineSeparator(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException exception) {
            throw new CsvWriteException("Nu s-a putut scrie in audit.csv: " + exception.getMessage());
        }
    }

    public List<String> findAll() throws CsvReadException {
        try {
            return Files.readAllLines(auditCsvPath);
        } catch (IOException exception) {
            throw new CsvReadException("Nu s-a putut citi audit.csv: " + exception.getMessage());
        }
    }

    private String toCsvLine(String timestamp, String action, String username, String details) {
        return escape(timestamp) + ","
                + escape(action) + ","
                + escape(username) + ","
                + escape(details);
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }

        String escaped = value.replace("\"", "\"\"");

        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }

        return escaped;
    }
}
