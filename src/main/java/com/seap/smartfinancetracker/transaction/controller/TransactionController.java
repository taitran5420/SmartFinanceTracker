package com.seap.smartfinancetracker.transaction.controller;

import com.seap.smartfinancetracker.security.annotation.CurrentUserId;
import com.seap.smartfinancetracker.transaction.dto.TransactionCreateRequest;
import com.seap.smartfinancetracker.transaction.dto.TransactionFilterRequest;
import com.seap.smartfinancetracker.transaction.dto.TransactionResponse;
import com.seap.smartfinancetracker.transaction.dto.TransactionUpdateRequest;
import com.seap.smartfinancetracker.transaction.service.TransactionService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/transactions")
@AllArgsConstructor
public class TransactionController {
    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(
            @CurrentUserId UUID userId,
            @Valid @RequestBody TransactionCreateRequest transactionCreateRequest
    ) {
            TransactionResponse transactionResponse = transactionService.createTransaction(userId, transactionCreateRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(transactionResponse);
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> getTransactionById(
            @CurrentUserId UUID userId,
            @PathVariable UUID transactionId) {
        TransactionResponse transactionResponse = transactionService.getTransactionById(userId, transactionId);
        return ResponseEntity.ok(transactionResponse);
    }

    @GetMapping
    public ResponseEntity<Slice<TransactionResponse>> getAllTransactions(
            @CurrentUserId UUID userId,
            @ModelAttribute TransactionFilterRequest transactionFilterRequest,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Slice<TransactionResponse> transactionResponses = transactionService.getTransactions(
                userId,
                transactionFilterRequest,
                pageable);

        return ResponseEntity.ok(transactionResponses);
    }

    @PutMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> updateTransaction(
            @CurrentUserId UUID userId,
            @PathVariable UUID transactionId,
            @Valid @RequestBody TransactionUpdateRequest transactionUpdateRequest
    ) {
        TransactionResponse transactionUpdatedResponse = transactionService.updateTransaction(userId, transactionId, transactionUpdateRequest);

        return ResponseEntity.ok(transactionUpdatedResponse);
    }

    @DeleteMapping("/{transactionId}")
    public ResponseEntity<Void> deleteTransaction(
            @CurrentUserId UUID userId,
            @PathVariable UUID transactionId
    ) {
        transactionService.deleteTransaction(userId, transactionId);
        return ResponseEntity.noContent().build();
    }
}
