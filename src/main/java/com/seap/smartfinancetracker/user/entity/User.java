package com.seap.smartfinancetracker.user.entity;

import com.seap.smartfinancetracker.user.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents an application user stored in the database.
 */
@Entity
@Table(name = User.USER_TABLE_NAME)
@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class User {

    public static final String USER_TABLE_NAME = "users";

    public static final String FULL_NAME_COLUMN_NAME = "full_name";
    public static final String CREATED_AT_COLUMN_NAME = "created_at";
    public static final String UPDATED_AT_COLUMN_NAME = "updated_at";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = FULL_NAME_COLUMN_NAME,nullable = false)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.USER;

    @CreatedDate
    @Column(name = CREATED_AT_COLUMN_NAME, nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = UPDATED_AT_COLUMN_NAME, nullable = false)
    private Instant updatedAt;
}
