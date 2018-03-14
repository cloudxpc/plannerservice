package com.uutic.mina.plannerservice.controller;

import com.uutic.mina.plannerservice.entity.PlanItem;
import com.uutic.mina.plannerservice.repository.PlanItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plan")
public class PlanController {
    @Autowired
    private PlanItemRepository planItemRepository;

    @RequestMapping(value = "", method = RequestMethod.GET)
    public List<PlanItem> getItems(@RequestAttribute("plan_id") String planId) {
        return planItemRepository.findAllByPlanId(planId);
    }
}
