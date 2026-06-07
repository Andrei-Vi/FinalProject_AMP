package repository;

import exception.CsvReadException;
import exception.CsvWriteException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

final class CsvUtils {
    private CsvUtils() {
    }

    static List<String> readDataLines(Path path) throws CsvReadException {
        try {
            if (!Files.exists(path)) {
                return new ArrayList<>();
            }

            List<String> lines = Files.readAllLines(path);
            if (lines.size() <= 1) {
                return new ArrayList<>();
            }

            return new ArrayList<>(lines.subList(1, lines.size()));
        } catch (IOException exception) {
            throw new CsvReadException("Nu s-a putut citi fisierul CSV " + path + ": " + exception.getMessage());
        }
    }

    static void writeLines(Path path, String header, List<String> dataLines) throws CsvWriteException {
        List<String> lines = new ArrayList<>();
        lines.add(header);
        lines.addAll(dataLines);

        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            Files.write(path, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException exception) {
            throw new CsvWriteException("Nu s-a putut scrie fisierul CSV " + path + ": " + exception.getMessage());
        }
    }

    static String toCsvLine(String... values) {
        List<String> escapedValues = new ArrayList<>();

        for (String value : values) {
            escapedValues.add(escape(value));
        }

        return String.join(",", escapedValues);
    }

    static List<String> parseLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder currentValue = new StringBuilder();
        boolean insideQuotes = false;

        for (int index = 0; index < line.length(); index++) {
            char character = line.charAt(index);

            if (character == '"') {
                if (insideQuotes && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    currentValue.append('"');
                    index++;
                } else {
                    insideQuotes = !insideQuotes;
                }
            } else if (character == ',' && !insideQuotes) {
                values.add(currentValue.toString());
                currentValue.setLength(0);
            } else {
                currentValue.append(character);
            }
        }

        values.add(currentValue.toString());
        return values;
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }

        String escaped = value.replace("\"", "\"\"");

        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n") || escaped.contains("\r")) {
            return "\"" + escaped + "\"";
        }

        return escaped;
    }
}
