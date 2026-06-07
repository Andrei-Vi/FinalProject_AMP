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
    private static final String HEADER = "timestamp,action,username,details";

    private final Path auditCsvPath;

    public AuditCsvRepository(String auditCsvPath) {
        this.auditCsvPath = Path.of(auditCsvPath);
    }

    public void save(String action, String username, String details) throws CsvWriteException {
        String line = toCsvLine(LocalDateTime.now().toString(), action, username, details);

        try {
            Path parent = auditCsvPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            if (!Files.exists(auditCsvPath) || Files.size(auditCsvPath) == 0) {
                Files.writeString(
                        auditCsvPath,
                        HEADER + System.lineSeparator(),
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND
                );
            }

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
            if (!Files.exists(auditCsvPath)) {
                return List.of();
            }

            return Files.readAllLines(auditCsvPath);
        } catch (IOException exception) {
            throw new CsvReadException("Nu s-a putut citi audit.csv: " + exception.getMessage());
        }
    }

    public void clear() throws CsvWriteException {
        try {
            Path parent = auditCsvPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            Files.writeString(
                    auditCsvPath,
                    "",
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
        } catch (IOException exception) {
            throw new CsvWriteException("Nu s-a putut sterge audit.csv: " + exception.getMessage());
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
