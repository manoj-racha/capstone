package org.hartford.greensure.dto.response;

import lombok.Data;

@Data
public class VehicleDocumentResponseDTO {
    private Long documentId;
    private String documentType;
    private String documentUrl;
    private String originalFileName;
    private String mimeType;
    private Long fileSizeBytes;
    private boolean verified;
    private String agentNote;
    private String uploadedAt;
}
