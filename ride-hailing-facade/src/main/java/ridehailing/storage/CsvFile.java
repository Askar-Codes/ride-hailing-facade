package ridehailing.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Stores rows of text in a real file on disk (columns are separated by ';').
 * It keeps NOTHING in memory: every read goes to the file, so a restarted
 * application sees exactly what was written before.
 * The first column of every row is the id of the row.
 */
public class CsvFile {

    private static final String COLUMN_SEPARATOR = ";";

    private final Path pathOfCsvFile;

    public CsvFile(Path pathOfCsvFile) {
        this.pathOfCsvFile = pathOfCsvFile;
    }

    public List<String[]> readAllRowsFromFile() {
        List<String[]> allRows = new ArrayList<>();
        if (!Files.exists(pathOfCsvFile)) {
            return allRows;
        }
        try {
            for (String lineOfFile : Files.readAllLines(pathOfCsvFile)) {
                if (!lineOfFile.isBlank()) {
                    allRows.add(lineOfFile.split(COLUMN_SEPARATOR, -1));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read file " + pathOfCsvFile, e);
        }
        return allRows;
    }

    /** Replaces the row that has the same id (first column), or adds a new row at the end. */
    public void saveOrReplaceRowById(String[] rowWithIdInFirstColumn) {
        String[] rowWithoutSeparators = removeSeparatorsFromValues(rowWithIdInFirstColumn);
        List<String[]> allRows = readAllRowsFromFile();

        boolean rowWithSameIdWasReplaced = false;
        for (int rowIndex = 0; rowIndex < allRows.size(); rowIndex++) {
            if (allRows.get(rowIndex)[0].equals(rowWithoutSeparators[0])) {
                allRows.set(rowIndex, rowWithoutSeparators);
                rowWithSameIdWasReplaced = true;
            }
        }
        if (!rowWithSameIdWasReplaced) {
            allRows.add(rowWithoutSeparators);
        }
        writeAllRowsToFile(allRows);
    }

    private void writeAllRowsToFile(List<String[]> allRows) {
        List<String> linesOfFile = new ArrayList<>();
        for (String[] row : allRows) {
            linesOfFile.add(String.join(COLUMN_SEPARATOR, row));
        }
        try {
            Files.createDirectories(pathOfCsvFile.toAbsolutePath().getParent());
            Files.write(pathOfCsvFile, linesOfFile);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write file " + pathOfCsvFile, e);
        }
    }

    /** A typed ';' would break the columns of the file, so it becomes ','. */
    private String[] removeSeparatorsFromValues(String[] row) {
        String[] rowWithoutSeparators = new String[row.length];
        for (int columnIndex = 0; columnIndex < row.length; columnIndex++) {
            rowWithoutSeparators[columnIndex] = row[columnIndex].replace(COLUMN_SEPARATOR, ",");
        }
        return rowWithoutSeparators;
    }
}
