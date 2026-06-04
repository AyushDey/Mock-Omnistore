package com.omnistore.mockui.repository;

import com.omnistore.mockui.model.Transaction;
import com.omnistore.mockui.model.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    Optional<Transaction> findFirstByStatusOrderByCreatedAtDesc(TransactionStatus status);
    List<Transaction> findByStatusOrderByCreatedAtDesc(TransactionStatus status);
    List<Transaction> findAllByOrderByCreatedAtDesc();
}
