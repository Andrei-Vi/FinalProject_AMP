package repository;

import exception.CsvReadException;
import exception.CsvWriteException;
import model.AdminUser;
import model.RegularUser;
import model.User;
import model.enums.UserRole;
import model.enums.UserStatus;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class UserCsvRepository {
    private static final String HEADER = "id,username,password,role,status";

    private final Path usersCsvPath;

    public UserCsvRepository(String usersCsvPath) {
        this.usersCsvPath = Path.of(usersCsvPath);
    }

    public List<User> findAll() throws CsvReadException {
        List<User> users = new ArrayList<>();

        for (String line : CsvUtils.readDataLines(usersCsvPath)) {
            if (line.trim().isEmpty()) {
                continue;
            }

            List<String> values = CsvUtils.parseLine(line);
            if (values.size() < 5) {
                throw new CsvReadException("Linie invalida in users.csv: " + line);
            }

            int id = parseInt(values.get(0));
            String username = values.get(1);
            String password = values.get(2);
            UserRole role = UserRole.valueOf(values.get(3));
            UserStatus status = UserStatus.valueOf(values.get(4));

            if (role == UserRole.ADMIN) {
                users.add(new AdminUser(id, username, password, role, status));
            } else {
                users.add(new RegularUser(id, username, password, role, status));
            }
        }

        return users;
    }

    public void saveAll(List<User> users) throws CsvWriteException {
        List<String> lines = new ArrayList<>();

        for (User user : users) {
            lines.add(CsvUtils.toCsvLine(
                    String.valueOf(user.getId()),
                    user.getUsername(),
                    user.getPassword(),
                    user.getRole().name(),
                    user.getStatus().name()
            ));
        }

        CsvUtils.writeLines(usersCsvPath, HEADER, lines);
    }

    private int parseInt(String value) throws CsvReadException {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new CsvReadException("Valoare invalida pentru id user: " + value);
        }
    }
}
