package com.suvidha.billing.repository;

import com.suvidha.billing.entity.Transaction;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {

    List<Transaction> findAllByRazorpayOrderId(String razorpayOrderId);

    Optional<Transaction> findByRazorpayOrderId(String razorpayOrderId);

    Optional<Transaction> findByRazorpayPaymentId(String razorpayPaymentId);

    Optional<Transaction> findByTransactionId(String transactionId);

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Transaction t WHERE t.razorpayOrderId = :orderId")
    List<Transaction> findAllByRazorpayOrderIdForUpdate(@Param("orderId") String orderId);

    Page<Transaction> findByAccountIdOrderByCreatedAtDesc(String accountId, Pageable pageable);

    long countByAccountId(String accountId);
}
