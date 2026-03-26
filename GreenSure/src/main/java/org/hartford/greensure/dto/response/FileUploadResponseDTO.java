package org.hartford.greensure.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileUploadResponseDTO {
    private String fileUrl;
    private String originalFileName;
    private String mimeType;
    private Long fileSizeBytes;
}
