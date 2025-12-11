package com.taskmanagement.domain.importjob.parser;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses Excel (.xlsx) files containing task data
 */
@Component
public class ExcelParser {

    /**
     * Parse Excel file and return list of parsed task data
     *
     * @param file Uploaded Excel file
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

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            // Read first sheet
            Sheet sheet = workbook.getSheetAt(0);

            if (sheet.getPhysicalNumberOfRows() == 0) {
                throw new IllegalArgumentException("Excel file is empty");
            }

            // First row is header
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                throw new IllegalArgumentException("Excel file has no header row");
            }

            Map<String, Integer> columnMap = buildColumnMap(headerRow);

            // Parse data rows (skip header)
            for (int i = sheet.getFirstRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);

                // Skip null or empty rows
                if (row == null || isEmptyRow(row)) {
                    continue;
                }

                int lineNumber = i + 1; // Line number in file (1-indexed)

                ParsedTaskData data = ParsedTaskData.builder()
                        .lineNumber(lineNumber)
                        .taskCode(getCellValue(row, columnMap, "task_code"))
                        .name(getCellValue(row, columnMap, "name"))
                        .assignee(getCellValue(row, columnMap, "assignee"))
                        .startDate(getCellValue(row, columnMap, "start_date"))
                        .endDate(getCellValue(row, columnMap, "end_date"))
                        .progress(getCellValue(row, columnMap, "progress"))
                        .status(getCellValue(row, columnMap, "status"))
                        .parentTaskCode(getCellValue(row, columnMap, "parent_task_code"))
                        .isMilestone(getCellValue(row, columnMap, "is_milestone"))
                        .predecessorTaskCodes(getCellValue(row, columnMap, "predecessor_task_codes"))
                        .dependencyType(getCellValue(row, columnMap, "dependency_type"))
                        .notes(getCellValue(row, columnMap, "notes"))
                        .build();

                result.add(data);
            }
        }

        return result;
    }

    /**
     * Build a map of column names to their indices
     */
    private Map<String, Integer> buildColumnMap(Row headerRow) {
        Map<String, Integer> map = new HashMap<>();

        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i);
            if (cell != null) {
                String headerValue = getCellValueAsString(cell).trim().toLowerCase();
                if (!headerValue.isEmpty()) {
                    map.put(headerValue, i);
                }
            }
        }

        return map;
    }

    /**
     * Get cell value as string by column name
     */
    private String getCellValue(Row row, Map<String, Integer> columnMap, String columnName) {
        Integer index = columnMap.get(columnName.toLowerCase());
        if (index == null) {
            return null;
        }

        Cell cell = row.getCell(index);
        if (cell == null) {
            return null;
        }

        String value = getCellValueAsString(cell).trim();
        return value.isEmpty() ? null : value;
    }

    /**
     * Convert cell value to string regardless of cell type
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    // Format date as yyyy-MM-dd
                    return cell.getLocalDateTimeCellValue().toLocalDate().toString();
                } else {
                    // Format number without decimal if it's a whole number
                    double numValue = cell.getNumericCellValue();
                    if (numValue == (long) numValue) {
                        return String.format("%d", (long) numValue);
                    } else {
                        return String.valueOf(numValue);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            case BLANK:
                return "";
            default:
                return "";
        }
    }

    /**
     * Check if row is empty (all cells are null or blank)
     */
    private boolean isEmptyRow(Row row) {
        for (int i = 0; i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String value = getCellValueAsString(cell).trim();
                if (!value.isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }
}
