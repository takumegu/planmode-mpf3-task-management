package com.taskmanagement.domain.importjob.parser;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses CSV files containing task data
 * Handles UTF-8 encoding and BOM (Byte Order Mark)
 */
@Component
public class CsvParser {

    /**
     * Parse CSV file and return list of parsed task data
     *
     * @param file Uploaded CSV file
     * @return List of parsed task data with line numbers
     * @throws IOException If file cannot be read
     */
    public List<ParsedTaskData> parse(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        if (file.getSize() > 10 * 1024 * 1024) { // 10MB limit
            throw new IllegalArgumentException("File size exceeds 10MB limit");
        }

        List<ParsedTaskData> result = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            // Remove BOM if present
            reader.mark(1);
            if (reader.read() != 0xFEFF) {
                reader.reset();
            }

            try (CSVReader csvReader = new CSVReaderBuilder(reader)
                    .withSkipLines(0)
                    .build()) {

                List<String[]> allRows = csvReader.readAll();

                if (allRows.isEmpty()) {
                    throw new IllegalArgumentException("CSV file is empty");
                }

                // First row is header
                String[] headers = allRows.get(0);
                Map<String, Integer> columnMap = buildColumnMap(headers);

                // Parse data rows (skip header)
                for (int i = 1; i < allRows.size(); i++) {
                    String[] row = allRows.get(i);
                    int lineNumber = i + 1; // Line number in file (1-indexed)

                    // Skip empty rows
                    if (isEmptyRow(row)) {
                        continue;
                    }

                    ParsedTaskData data = ParsedTaskData.builder()
                            .lineNumber(lineNumber)
                            .taskCode(getColumnValue(row, columnMap, "task_code"))
                            .name(getColumnValue(row, columnMap, "name"))
                            .assignee(getColumnValue(row, columnMap, "assignee"))
                            .startDate(getColumnValue(row, columnMap, "start_date"))
                            .endDate(getColumnValue(row, columnMap, "end_date"))
                            .progress(getColumnValue(row, columnMap, "progress"))
                            .status(getColumnValue(row, columnMap, "status"))
                            .parentTaskCode(getColumnValue(row, columnMap, "parent_task_code"))
                            .isMilestone(getColumnValue(row, columnMap, "is_milestone"))
                            .predecessorTaskCodes(getColumnValue(row, columnMap, "predecessor_task_codes"))
                            .dependencyType(getColumnValue(row, columnMap, "dependency_type"))
                            .notes(getColumnValue(row, columnMap, "notes"))
                            .build();

                    result.add(data);
                }

            } catch (CsvException e) {
                throw new IOException("Failed to parse CSV file: " + e.getMessage(), e);
            }
        }

        return result;
    }

    /**
     * Build a map of column names to their indices
     */
    private Map<String, Integer> buildColumnMap(String[] headers) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            String normalizedHeader = headers[i].trim().toLowerCase();
            map.put(normalizedHeader, i);
        }
        return map;
    }

    /**
     * Get value from row by column name, return null if column doesn't exist or value is empty
     */
    private String getColumnValue(String[] row, Map<String, Integer> columnMap, String columnName) {
        Integer index = columnMap.get(columnName.toLowerCase());
        if (index == null || index >= row.length) {
            return null;
        }

        String value = row[index].trim();
        return value.isEmpty() ? null : value;
    }

    /**
     * Check if row is empty (all columns are null or whitespace)
     */
    private boolean isEmptyRow(String[] row) {
        for (String cell : row) {
            if (cell != null && !cell.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
