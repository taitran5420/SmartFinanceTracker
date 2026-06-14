package com.seap.smartfinancetracker.transaction.aspect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seap.smartfinancetracker.common.constant.KafkaConstant;
import com.seap.smartfinancetracker.notification.event.TransactionCreatedEvent;
import com.seap.smartfinancetracker.transaction.dto.TransactionCreateRequest;
import com.seap.smartfinancetracker.transaction.dto.TransactionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Aspect that publishes a {@link TransactionCreatedEvent} to Kafka after a transaction is created.
 * <p>
 * The advice is bound to {@code TransactionService.createTransaction} and runs <b>after</b> the
 * method returns successfully. Because this aspect is ordered at {@link Ordered#HIGHEST_PRECEDENCE},
 * it wraps the (lower-precedence) Spring transaction interceptor; its {@code @AfterReturning} advice
 * therefore fires only once the surrounding {@code @Transactional(REQUIRES_NEW)} commit has succeeded.
 * </p>
 * <p>
 * This guarantees a downstream success notification is emitted only for a truly committed
 * transaction. If {@code createTransaction} throws (and the transaction rolls back), the advice does
 * not run and no Kafka message is sent.
 * </p>
 */
@Slf4j
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
public class TransactionEventAspect {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Publishes the {@link TransactionCreatedEvent} for a committed transaction.
     *
     * @param userId   the id of the user who created the transaction (first method argument)
     * @param request  the original creation request (unused, bound for pointcut matching)
     * @param response the persisted transaction returned by the service
     */
    @AfterReturning(
            pointcut = "execution(* com.seap.smartfinancetracker.transaction.service.TransactionService.createTransaction(..)) "
                    + "&& args(userId, request)",
            returning = "response",
            argNames = "userId,request,response")
    public void publishTransactionCreatedEvent(UUID userId, TransactionCreateRequest request, TransactionResponse response) {
        if (response == null) {
            return;
        }

        TransactionCreatedEvent transactionCreatedEvent = TransactionCreatedEvent.builder()
                .userId(userId)
                .categoryName(response.categoryName())
                .amount(response.amount())
                .transactionType(response.transactionType())
                .build();

        try {
            String jsonPayload = objectMapper.writeValueAsString(transactionCreatedEvent);
            kafkaTemplate.send(KafkaConstant.TRANSACTION_CREATED_TOPIC, jsonPayload);
            log.info("Published TransactionCreatedEvent to Kafka topic 'transaction-created-topic' for user: {}", userId);
        } catch (JsonProcessingException e) {
            log.error("Failed to convert event to JSON for user: {}", userId, e);
        }
    }
}
