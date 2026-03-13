package com.mekheainteractive.Transaction_Microservice.TopUp_Service.Repository;

import com.mekheainteractive.Transaction_Microservice.TopUp_Service.Entity.TopUpOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TopUpOrderRepository extends JpaRepository<TopUpOrder, Long> {

    Optional<TopUpOrder> findByCode(String code);

}