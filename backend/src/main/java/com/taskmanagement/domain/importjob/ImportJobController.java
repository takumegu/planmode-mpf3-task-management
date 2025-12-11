package com.taskmanagement.domain.importjob;

import com.taskmanagement.dto.response.ApiResponse;
import com.taskmanagement.dto.response.ImportJobResponse;
import com.taskmanagement.util.ErrorCsvGenerator;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;

/**
 * REST controller for import job operations
 */
@RestController
@RequestMapping("/api/import-jobs")
@CrossOrigin(origins = "*")
public class ImportJobController {

    private final ImportJobService importJobService;
    private final ImportJobRepository importJobRepository;
    private final ErrorCsvGenerator errorCsvGenerator;

    public ImportJobController(ImportJobService importJobService,
                               ImportJobRepository importJobRepository,
                               ErrorCsvGenerator errorCsvGenerator) {
        this.importJobService = importJobService;
        this.importJobRepository = importJobRepository;
        this.errorCsvGenerator = errorCsvGenerator;
    }

    /**
     * POST /api/import-jobs?projectId={projectId}&dryRun={true|false} - Upload and import CSV/Excel file
     *
     * @param file      CSV or Excel file to import
     * @param projectId Target project ID
     * @param dryRun    If true, only validate without committing (default: false)
     * @return Import job response with validation results and summary
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ImportJobResponse> createImportJob(
            @RequestParam("file") MultipartFile file,
            @RequestParam("projectId") Long projectId,
            @RequestParam(value = "dryRun", defaultValue = "false") boolean dryRun
    ) throws IOException {

        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }

        ImportJobResponse response = importJobService.executeImport(file, projectId, dryRun);
        return ApiResponse.success(response);
    }

    /**
     * GET /api/import-jobs/{id} - Get import job status and summary
     *
     * @param id Import job ID
     * @return Import job response
     */
    @GetMapping("/{id}")
    public ApiResponse<ImportJobResponse> getImportJob(@PathVariable Long id) {
        ImportJobResponse response = importJobService.getImportJob(id);
        return ApiResponse.success(response);
    }

    /**
     * GET /api/import-jobs/{id}/errors - Download error report CSV
     *
     * @param id Import job ID
     * @return CSV file with validation errors
     */
    @GetMapping("/{id}/errors")
    public ResponseEntity<Resource> downloadErrorReport(@PathVariable Long id) {
        // Get import job
        ImportJob job = importJobRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Import job not found with id: " + id));

        // Check if error report exists
        if (job.getErrorReportPath() == null) {
            throw new EntityNotFoundException("No error report available for import job: " + id);
        }

        Path errorReportPath = errorCsvGenerator.getErrorReportPath(id);
        if (errorReportPath == null) {
            throw new EntityNotFoundException("Error report file not found for import job: " + id);
        }

        // Create file resource
        Resource resource = new FileSystemResource(errorReportPath);

        // Set headers for file download
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION,
                String.format("attachment; filename=\"import-errors-%d.csv\"", id));
        headers.add(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8");

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(errorReportPath.toFile().length())
                .body(resource);
    }
}
