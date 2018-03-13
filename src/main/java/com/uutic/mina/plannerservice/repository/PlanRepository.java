package com.uutic.mina.plannerservice.repository;

import com.uutic.mina.plannerservice.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanRepository extends JpaRepository<Plan, String> {
}
