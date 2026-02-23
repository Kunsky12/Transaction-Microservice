package com.mekheainteractive.Transaction_Microservice.Repository;

import com.mekheainteractive.Transaction_Microservice.Entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LedgerRepository extends JpaRepository<TransactionEntity, String> {

    List<TransactionEntity> findBySenderIdAndIdempotencyKey(String senderId, String idempotencyKey);
}

