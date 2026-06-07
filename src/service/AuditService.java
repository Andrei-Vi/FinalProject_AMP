package service;

import exception.CsvWriteException;
import exception.CsvReadException;
import model.User;
import repository.AuditCsvRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuditService {
    private final List<String> auditLogs;
    private final AuditCsvRepository auditCsvRepository;

    public AuditService() {
        this(null);
    }

    public AuditService(AuditCsvRepository auditCsvRepository) {
        this.auditLogs = new ArrayList<>();
        this.auditCsvRepository = auditCsvRepository;
    }

    public void logAction(String action, User user) throws CsvWriteException {
        logAction(action, user, "");
    }

    public void logAction(String action, User user, String details) throws CsvWriteException {
        String username = user == null ? "SYSTEM" : user.getUsername();
        String log = LocalDateTime.now() + " | " + action + " | user=" + username;

        if (details != null && !details.trim().isEmpty()) {
            log += " | " + details;
        }

        auditLogs.add(log);

        if (auditCsvRepository != null) {
            auditCsvRepository.save(action, username, details);
        }
    }

    public List<String> getAuditLogs() {
        if (auditCsvRepository != null) {
            try {
                return auditCsvRepository.findAll();
            } catch (CsvReadException exception) {
                List<String> logs = new ArrayList<>(auditLogs);
                logs.add("Eroare la citirea audit.csv: " + exception.getMessage());
                return logs;
            }
        }

        return new ArrayList<>(auditLogs);
    }

    public void clearAuditLogs() throws CsvWriteException {
        auditLogs.clear();

        if (auditCsvRepository != null) {
            auditCsvRepository.clear();
        }
    }
}
