package com.taskmanagement.util;

import com.opencsv.CSVWriter;
import com.taskmanagement.dto.response.ValidationError;
import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Generates CSV files containing validation errors
 */
@Component
public class ErrorCsvGenerator {

    private static final String ERROR_REPORTS_DIR = "error-reports";
    private static final String[] HEADERS = {
            "Line Number", "Field", "Value", "Error Code", "Error Message"
    };

    /**
     * Generate error report CSV file
     *
     * @param errors List of validation errors
     * @param jobId  Import job ID
     * @return Path to generated CSV file
     * @throws IOException If file cannot be written
     */
    public String generateErrorCsv(List<ValidationError> errors, Long jobId) throws IOException {
        // Create error reports directory if it doesn't exist
        Path dirPath = Paths.get(ERROR_REPORTS_DIR);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }

        // Generate filename
        String filename = String.format("import-errors-%d.csv", jobId);
        Path filePath = dirPath.resolve(filename);

        // Write CSV
        try (CSVWriter writer = new CSVWriter(new FileWriter(filePath.toFile()))) {
            // Write header
            writer.writeNext(HEADERS);

            // Write error rows
            for (ValidationError error : errors) {
                String[] row = {
                        error.getLineNumber() != null ? error.getLineNumber().toString() : "",
                        error.getField() != null ? error.getField() : "",
                        error.getValue() != null ? error.getValue() : "",
                        error.getErrorCode() != null ? error.getErrorCode() : "",
                        error.getErrorMessage() != null ? error.getErrorMessage() : ""
                };
                writer.writeNext(row);
            }
        }

        return filePath.toString();
    }

    /**
     * Get error report file path for a job
     *
     * @param jobId Import job ID
     * @return Path to error report file, or null if not found
     */
    public Path getErrorReportPath(Long jobId) {
        String filename = String.format("import-errors-%d.csv", jobId);
        Path filePath = Paths.get(ERROR_REPORTS_DIR, filename);

        if (Files.exists(filePath)) {
            return filePath;
        }
        return null;
    }

    /**
     * Delete error report file
     *
     * @param filePath Path to file to delete
     * @throws IOException If file cannot be deleted
     */
    public void deleteErrorReport(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        if (Files.exists(path)) {
            Files.delete(path);
        }
    }
}
