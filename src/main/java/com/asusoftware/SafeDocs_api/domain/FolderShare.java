package com.asusoftware.SafeDocs_api.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "folder_shares",
        uniqueConstraints = @UniqueConstraint(name = "uq_folder_share",
                columnNames = {"folder_id","shared_with_user_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FolderShare {

    @Id @UuidGenerator
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name="folder_id", nullable=false,
            foreignKey=@ForeignKey(name="folder_shares_folder_id_fkey"))
    private Folder folder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name="shared_with_user_id", nullable=false,
            foreignKey=@ForeignKey(name="folder_shares_shared_with_user_id_fkey"))
    private User sharedWith;

    @Column(name="permission", nullable=false, length=16)
    private String permission; // READ | WRITE (poți face enum separat)

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name="created_by_user_id", nullable=false,
            foreignKey=@ForeignKey(name="folder_shares_created_by_user_id_fkey"))
    private User createdBy;

    @Column(name="created_at", nullable=false)
    private Instant createdAt = Instant.now();
}
