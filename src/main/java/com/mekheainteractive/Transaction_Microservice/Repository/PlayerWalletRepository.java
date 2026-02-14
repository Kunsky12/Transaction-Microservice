package com.mekheainteractive.Transaction_Microservice.Repository;

import com.mekheainteractive.Transaction_Microservice.Entity.PlayerWallet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerWalletRepository extends JpaRepository <PlayerWallet, String>{
}
