package org.hartford.greensure.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hartford.greensure.dto.response.ApiResponse;
import org.hartford.greensure.dto.response.FileUploadResponseDTO;
import org.hartford.greensure.exception.FileProcessingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/uploads")
@RequiredArgsConstructor
@Slf4j
public class FileUploadController {

    @Value("${app.upload.dir}")
    private String uploadDir;

    @PostMapping(value = "/file", produces = "application/json")
    public ResponseEntity<ApiResponse<FileUploadResponseDTO>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "category", defaultValue = "general") String category) {

        log.info("Receiving file upload request for category: {}", category);

        // STEP 1 — Validate the file is not empty
        if (file == null || file.isEmpty()) {
            throw new FileProcessingException("No file provided or file is empty");
        }

        // STEP 2 — Validate file type
        String contentType = file.getContentType();
        List<String> allowedTypes = List.of("image/jpeg", "image/png", "image/jpg", "application/pdf");
        if (contentType == null || !allowedTypes.contains(contentType)) {
            throw new FileProcessingException("Invalid file type. Only PDF, JPG, PNG allowed.");
        }

        // STEP 3 — Validate file size (10MB)
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new FileProcessingException("File too large. Maximum size is 10MB.");
        }

        try {
            // STEP 4 — Create upload directory if missing
            Path uploadPath = Paths.get(uploadDir, category);
            Files.createDirectories(uploadPath);

            // STEP 5 — Generate safe unique filename
            String originalName = file.getOriginalFilename();
            String extension = ".bin";
            if (originalName != null && originalName.contains(".")) {
                extension = originalName.substring(originalName.lastIndexOf("."));
            }
            String safeFileName = UUID.randomUUID().toString() + extension;

            // STEP 6 — Save file to disk
            Path filePath = uploadPath.resolve(safeFileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // STEP 7 — Build relative URL (Frontend will prepend API base if needed)
            String fileUrl = "/uploads/" + category + "/" + safeFileName;

            // STEP 8 — Return clean JSON response
            FileUploadResponseDTO dto = new FileUploadResponseDTO(
                    fileUrl,
                    file.getOriginalFilename(),
                    contentType,
                    file.getSize()
            );

            log.info("File uploaded successfully: {}", safeFileName);
            return ResponseEntity.ok(ApiResponse.success("File uploaded successfully", dto));

        } catch (IOException e) {
            // STEP 9 — Wrap entire method in try-catch to avoid 500 XML errors
            log.error("File upload failed: {}", e.getMessage());
            throw new FileProcessingException("File could not be saved. Please try again.");
        }
    }
}
