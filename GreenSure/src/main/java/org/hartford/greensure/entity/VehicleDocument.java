package org.hartford.greensure.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hartford.greensure.enums.VehicleDocumentType;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "vehicle_documents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "vehicle_id", nullable = false)
    private DeclarationVehicleData vehicle;

    @Enumerated(EnumType.STRING)
    private VehicleDocumentType documentType;

    private String documentUrl;
    private String originalFileName;
    private String mimeType;
    private Long fileSizeBytes;

    @Builder.Default
    private boolean verified = false;
    
    private String agentNote;

    @CreationTimestamp
    private LocalDateTime uploadedAt;
}
