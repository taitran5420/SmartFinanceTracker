package com.seap.smartfinancetracker.transaction.controller;

import com.seap.smartfinancetracker.security.annotation.CurrentUserId;
import com.seap.smartfinancetracker.transaction.dto.RecurringTransactionCreateRequest;
import com.seap.smartfinancetracker.transaction.dto.RecurringTransactionResponse;
import com.seap.smartfinancetracker.transaction.dto.RecurringTransactionUpdateRequest;
import com.seap.smartfinancetracker.transaction.dto.UpcomingRecurringResponse;
import com.seap.smartfinancetracker.transaction.service.RecurringTransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/recurring-transactions")
@RequiredArgsConstructor
public class RecurringTransactionController {

    private final RecurringTransactionService recurringTransactionService;

    @PostMapping
    public ResponseEntity<RecurringTransactionResponse> createRecurringTransaction(
            @CurrentUserId UUID userId,
            @Valid @RequestBody RecurringTransactionCreateRequest request) {
        RecurringTransactionResponse response = recurringTransactionService.createRecurring(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecurringTransactionResponse> getRecurringTransactionById(
            @CurrentUserId UUID userId,
            @PathVariable UUID id) {
        return ResponseEntity.ok(recurringTransactionService.getRecurringById(userId, id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RecurringTransactionResponse> updateRecurringTransaction(
            @CurrentUserId UUID userId,
            @PathVariable UUID id,
            @Valid @RequestBody RecurringTransactionUpdateRequest request
            ) {
        return ResponseEntity.ok(recurringTransactionService.updateRecurring(userId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecurring(
            @CurrentUserId UUID userId,
            @PathVariable UUID id) {
        recurringTransactionService.deleteRecurring(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<RecurringTransactionResponse> toggleActiveStatus(
            @CurrentUserId UUID userId,
            @PathVariable UUID id) {
        return ResponseEntity.ok(recurringTransactionService.toggleActiveStatus(userId, id));
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<UpcomingRecurringResponse>> getUpcomingTransactions(@CurrentUserId UUID userId) {
        return ResponseEntity.ok(recurringTransactionService.getUpcomingTransactions(userId));
    }
}
