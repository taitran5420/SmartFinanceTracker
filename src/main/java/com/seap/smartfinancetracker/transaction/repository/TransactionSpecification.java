package com.seap.smartfinancetracker.transaction.repository;

import com.seap.smartfinancetracker.transaction.dto.TransactionFilterRequest;
import com.seap.smartfinancetracker.transaction.entity.Transaction;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Utility class for constructing dynamic JPA {@link Specification} for the {@link Transaction} entity.
 * <p>
 * This class leverages the JPA Criteria API to build complex, type-safe queries
 * based on varying client-provided filtering criteria.
 * </p>
 */
public class TransactionSpecification {

    /**
     * Builds a {@link Specification} to filter transactions based on the given criteria.
     * <p>
     * <b>Security Note:</b> The query is strictly scoped to the provided {@code userId}.
     * This is a mandatory predicate to ensure users can only query their own data.
     * All other criteria provided in the {@link TransactionFilterRequest} are optional
     * and will be appended using a logical {@code AND} operator if present.
     * </p>
     *
     * @param userId                   the unique identifier of the user (mandatory for data isolation)
     * @param transactionFilterRequest the payload containing optional filtering criteria
     *                                 (e.g., date ranges, category ID, transaction type)
     * @return a constructed {@link Specification} that can be executed by a {@link org.springframework.data.jpa.repository.JpaSpecificationExecutor}
     */
    public static Specification<Transaction> getFilterTransaction(UUID userId, TransactionFilterRequest transactionFilterRequest) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.equal(root.get("user").get("id"), userId));

            if (transactionFilterRequest.startDate() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), transactionFilterRequest.startDate()));
            }

            if (transactionFilterRequest.endDate() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), transactionFilterRequest.endDate()));
            }

            if (transactionFilterRequest.categoryId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("category").get("id"), transactionFilterRequest.categoryId()));
            }

            if (transactionFilterRequest.transactionType() != null) {
                predicates.add(criteriaBuilder.equal(root.get("transactionType"), transactionFilterRequest.transactionType()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
