package com.uutic.mina.plannerservice.repository;

import com.uutic.mina.plannerservice.entity.PlanItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanItemRepository extends JpaRepository<PlanItem, String> {
}
