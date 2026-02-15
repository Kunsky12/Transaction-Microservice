package com.mekheainteractive.Transaction_Microservice.Repository;

import com.mekheainteractive.Transaction_Microservice.Entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<TransactionEntity, String> {
}
