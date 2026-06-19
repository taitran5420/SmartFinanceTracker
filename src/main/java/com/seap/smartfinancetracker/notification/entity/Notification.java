package com.seap.smartfinancetracker.notification.entity;

import com.seap.smartfinancetracker.notification.enums.NotificationType;
import com.seap.smartfinancetracker.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = Notification.NOTIFICATION_TABLE_NAME, indexes = {
        @Index(name = Notification.NOTIFICATION_USER_UNREAD_INDEX,
                columnList = Notification.USER_ID_COLUMN_NAME + ", " +
                Notification.IS_READ_COLUMN_NAME)
})
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Notification {

    public static final String NOTIFICATION_TABLE_NAME = "notifications";

    public static final String USER_ID_COLUMN_NAME = "user_id";
    public static final String Notification_TYPE_COLUMN_NAME = "notification_type";
    public static final String IS_READ_COLUMN_NAME = "is_read";
    public static final String CREATED_AT_COLUMN_NAME = "created_at";
    public static final String UPDATED_AT_COLUMN_NAME = "updated_at";

    public static final String NOTIFICATION_USER_UNREAD_INDEX = "idx_notifications_user_unread";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = USER_ID_COLUMN_NAME, nullable = false)
    private User user;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = Notification_TYPE_COLUMN_NAME, nullable = false)
    private NotificationType notificationType;

    @Builder.Default
    @Column(name = IS_READ_COLUMN_NAME, nullable = false)
    private boolean isRead = false;

    @CreatedDate
    @Column(name = CREATED_AT_COLUMN_NAME, nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = UPDATED_AT_COLUMN_NAME)
    private Instant updatedAt;
}
