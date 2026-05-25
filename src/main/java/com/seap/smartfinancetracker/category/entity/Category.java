package com.seap.smartfinancetracker.category.entity;

import com.seap.smartfinancetracker.transaction.enums.TransactionType;
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
@Table(name = Category.CATEGORY_TABLE_NAME, uniqueConstraints = {
        @UniqueConstraint( name = Category.USER_CATEGORY_NAME_INDEX,
                columnNames = { Category.USER_ID_COLUMN, Category.CATEGORY_NAME_COLUMN } )
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Category {

    public final static String CATEGORY_TABLE_NAME = "categories";

    public final static String CATEGORY_NAME_COLUMN = "category_name";
    public final static String TRANSACTION_TYPE_COLUMN = "transaction_type";
    public final static String USER_ID_COLUMN = "user_id";
    public final static String CREATED_AT_COLUMN = "created_at";
    public final static String UPDATED_AT_COLUMN_NAME = "updated_at";

    public final static String USER_CATEGORY_NAME_INDEX = "uk_user_category_name";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = CATEGORY_NAME_COLUMN, nullable = false)
    private String categoryName;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = TRANSACTION_TYPE_COLUMN, nullable = false)
    private TransactionType transactionType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = USER_ID_COLUMN)
    private User user;

    @Column(nullable = false)
    private boolean active = true;

    @Column(unique = true, updatable = false)
    private String code;

    @CreatedDate
    @Column(name = CREATED_AT_COLUMN, nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @LastModifiedDate
    @Column(name = UPDATED_AT_COLUMN_NAME, nullable = false)
    private Instant updatedAt;
}
