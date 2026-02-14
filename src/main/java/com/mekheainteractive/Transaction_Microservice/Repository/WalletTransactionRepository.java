package com.mekheainteractive.Transaction_Microservice.Repository;

import com.mekheainteractive.Transaction_Microservice.Entity.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, String> {
}
