package com.uutic.mina.plannerservice.repository;

import com.uutic.mina.plannerservice.entity.PlanItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlanItemRepository extends JpaRepository<PlanItem, String> {
    List<PlanItem> findAllByPlanId(String planId);
}
