package com.seap.smartfinancetracker.transaction.aspect;

import com.seap.smartfinancetracker.common.constant.KafkaConstant;
import com.seap.smartfinancetracker.common.exception.BusinessException;
import com.seap.smartfinancetracker.common.messaging.KafkaEventPublisher;
import com.seap.smartfinancetracker.notification.event.OverdraftAlertEvent;
import com.seap.smartfinancetracker.notification.event.TransactionCreatedEvent;
import com.seap.smartfinancetracker.transaction.constant.TransactionConstant;
import com.seap.smartfinancetracker.transaction.dto.TransactionCreateRequest;
import com.seap.smartfinancetracker.transaction.dto.TransactionResponse;
import com.seap.smartfinancetracker.transaction.exception.TransactionErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Aspect that publishes transaction lifecycle events to Kafka around {@code TransactionService.createTransaction}.
 * <p>
 * Because this aspect is ordered at {@link Ordered#HIGHEST_PRECEDENCE}, it wraps the (lower-precedence)
 * Spring transaction interceptor. Its advice therefore runs only once the surrounding
 * {@code @Transactional(REQUIRES_NEW)} boundary has resolved:
 * </p>
 * <ul>
 *   <li>{@code @AfterReturning} — the create committed, so a {@link TransactionCreatedEvent} (success
 *       notification) is published. A rolled-back transaction never reaches this advice.</li>
 *   <li>{@code @AfterThrowing} — the create rolled back; if it failed specifically because the overdraft
 *       limit was exceeded, an {@link OverdraftAlertEvent} is published. This fires for every overdraft
 *       rejection, whether the call originated from the API or the recurring scheduler.</li>
 * </ul>
 */
@Slf4j
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
public class TransactionEventAspect {
    private final KafkaEventPublisher kafkaEventPublisher;

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

        kafkaEventPublisher.publish(KafkaConstant.TRANSACTION_CREATED_TOPIC, transactionCreatedEvent);
    }

    /**
     * Publishes an {@link OverdraftAlertEvent} when a transaction creation is rejected for exceeding the
     * overdraft limit. Other {@link BusinessException}s (and any other failure) are ignored.
     *
     * @param userId    the id of the user whose transaction was rejected (first method argument)
     * @param request   the original creation request (unused, bound for pointcut matching)
     * @param exception the exception thrown by the service
     */
    @AfterThrowing(
            pointcut = "execution(* com.seap.smartfinancetracker.transaction.service.TransactionService.createTransaction(..)) "
                    + "&& args(userId, request)",
            throwing = "exception",
            argNames = "userId,request,exception")
    public void publishOverdraftAlertEvent(UUID userId, TransactionCreateRequest request, Throwable exception) {
        if (!(exception instanceof BusinessException businessException)
                || businessException.getErrorCode() != TransactionErrorCode.OVERDRAFT_LIMIT_EXCEEDED) {
            return;
        }

        Object categoryName = businessException.getContext().get(TransactionConstant.OVERDRAFT_CATEGORY_NAME_KEY);

        OverdraftAlertEvent overdraftAlertEvent = OverdraftAlertEvent.builder()
                .userId(userId)
                .categoryName(categoryName == null ? null : categoryName.toString())
                .errorMessage(businessException.getMessage())
                .build();

        kafkaEventPublisher.publish(KafkaConstant.OVERDRAFT_ALERT_TOPIC, overdraftAlertEvent);
    }
}
