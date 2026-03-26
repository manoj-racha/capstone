package org.hartford.greensure.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.hartford.greensure.enums.VehicleDocumentType;

@Data
public class UploadVehicleDocumentRequestDTO {
    @NotNull private Long vehicleId;
    @NotNull private VehicleDocumentType documentType;
    @NotBlank private String documentUrl;
    private String originalFileName;
    private String mimeType;
    private Long fileSizeBytes;
}
