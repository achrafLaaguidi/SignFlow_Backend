package com.laaguidi.SignFlow.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "document_signers")
@Data
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DocumentSigner {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(nullable = false)
    private String email;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "signing_order")
    private Integer signingOrder;

    @Enumerated(EnumType.STRING)
    private SignerStatus status;

    @Column(name = "access_token", unique = true)
    private String accessToken;

    @Column(name = "signed_at")
    private LocalDateTime signedAt;

    public enum SignerStatus {
        PENDING, SIGNED, REJECTED
    }
}
