package com.seap.smartfinancetracker.transaction.mapper;

import com.seap.smartfinancetracker.category.entity.Category;
import com.seap.smartfinancetracker.transaction.dto.RecurringTransactionCreateRequest;
import com.seap.smartfinancetracker.transaction.dto.RecurringTransactionResponse;
import com.seap.smartfinancetracker.transaction.dto.TransactionCreateRequest;
import com.seap.smartfinancetracker.transaction.dto.UpcomingRecurringResponse;
import com.seap.smartfinancetracker.transaction.entity.RecurringTransaction;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.instancio.Select.field;
import static org.junit.jupiter.api.Assertions.*;

class RecurringTransactionMapperTest {

    private RecurringTransactionMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new RecurringTransactionMapper();
    }

    //<editor-fold desc="Test toRecurringTransactionResponse">
    @Test
    @DisplayName("Should correctly map Entity to Response DTO")
    void toRecurringTransactionResponse_ShouldMapCorrectly() {
        // Arrange
        RecurringTransaction entity = Instancio.create(RecurringTransaction.class);

        // Act
        RecurringTransactionResponse response = mapper.toRecurringTransactionResponse(entity);

        // Assert
        assertNotNull(response);
        assertEquals(entity.getId(), response.id());
        assertEquals(entity.getCategory().getId(), response.categoryId());
        assertEquals(entity.getAmount(), response.amount());
        assertEquals(entity.getFrequency(), response.frequency());
        assertEquals(entity.isActive(), response.active());
    }

    @Test
    @DisplayName("Should return null when Entity is null")
    void toRecurringTransactionResponse_ShouldReturnNull_WhenEntityIsNull() {
        assertNull(mapper.toRecurringTransactionResponse(null));
    }
    //</editor-fold>

    //<editor-fold desc="Test toEntity">
    @Test
    @DisplayName("Should correctly map Create Request and Category to Entity")
    void toEntity_ShouldMapCorrectly_AndSetInitialValues() {
        // Arrange
        UUID userId = UUID.randomUUID();
        Category category = Instancio.create(Category.class);
        RecurringTransactionCreateRequest request = Instancio.create(RecurringTransactionCreateRequest.class);

        // Act
        RecurringTransaction entity = mapper.toEntity(userId, request, category);

        // Assert
        assertNotNull(entity);
        assertEquals(userId, entity.getUser().getId());
        assertEquals(category, entity.getCategory());
        assertEquals(category.getTransactionType(), entity.getTransactionType());
        assertEquals(request.amount(), entity.getAmount());
        assertTrue(entity.isActive(), "New recurring transaction should be active by default");
        assertEquals(request.startDate(), entity.getNextOccurrenceDate(), "Next occurrence date should match start date initially");
    }
    //</editor-fold>

    //<editor-fold desc="Test toUpcomingRecurringResponse">
    @Test
    @DisplayName("Should correctly map Entity to Upcoming Response and calculate days until due")
    void toUpcomingRecurringResponse_ShouldCalculateDaysUntilDue() {
        // Arrange
        LocalDate nextDate = LocalDate.now().plusDays(5);
        RecurringTransaction entity = Instancio.of(RecurringTransaction.class)
                .set(field(RecurringTransaction::getNextOccurrenceDate), nextDate)
                .create();

        // Act
        UpcomingRecurringResponse response = mapper.toUpcomingRecurringResponse(entity);

        // Assert
        assertNotNull(response);
        assertEquals(entity.getId(), response.id());
        assertEquals(5L, response.daysUntilDue(), "Should dynamically calculate exactly 5 days until due");
    }
    //</editor-fold>

    //<editor-fold desc="Test toTransactionCreateRequest">
    @Test
    @DisplayName("Should map Entity to Transaction Create Request with a newly generated idempotency key")
    void toTransactionCreateRequest_ShouldGenerateNewIdempotencyKey() {
        // Arrange
        RecurringTransaction entity = Instancio.create(RecurringTransaction.class);

        // Act
        TransactionCreateRequest request = mapper.toTransactionCreateRequest(entity);

        // Assert
        assertNotNull(request);
        assertEquals(entity.getCategory().getId(), request.categoryId());
        assertEquals(entity.getAmount(), request.amount());
        assertNotNull(request.idempotencyKey(), "Idempotency key must be generated");
    }
    //</editor-fold>
}