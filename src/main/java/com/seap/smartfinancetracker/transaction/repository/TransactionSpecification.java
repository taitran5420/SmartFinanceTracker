package com.seap.smartfinancetracker.transaction.repository;

import com.seap.smartfinancetracker.transaction.dto.TransactionFilterRequest;
import com.seap.smartfinancetracker.transaction.entity.Transaction;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TransactionSpecification {
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
