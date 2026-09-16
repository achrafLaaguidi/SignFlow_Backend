package com.laaguidi.SignFlow.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "signature_fields")
@Data
@NoArgsConstructor @AllArgsConstructor
@Builder
public class SignatureField {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "signer_id", nullable = false)
    private DocumentSigner signer;

    @Column(name = "page_number", nullable = false)
    private Integer pageNumber;

    @Column(name = "x_position", nullable = false)
    private Float xPosition;

    @Column(name = "y_position", nullable = false)
    private Float yPosition;

    private Float width;
    private Float height;

    @Enumerated(EnumType.STRING)
    private FieldType type;

    public enum FieldType {
        SIGNATURE, STAMP, DATE, TEXT
    }
}