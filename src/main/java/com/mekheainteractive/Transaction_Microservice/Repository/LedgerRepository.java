package com.mekheainteractive.Transaction_Microservice.Repository;

import com.mekheainteractive.Transaction_Microservice.Entity.LedgerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LedgerRepository extends JpaRepository<LedgerEntity, String> {

    List<LedgerEntity> findByIdempotencyKey(String idempotencyKey); // ✅ unique or 0/1
}