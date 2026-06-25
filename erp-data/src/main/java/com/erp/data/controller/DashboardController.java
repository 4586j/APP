package com.erp.data.controller;
import com.erp.common.model.R; import com.erp.data.dto.DashboardStatsVO; import com.erp.data.service.DashboardService;
import lombok.RequiredArgsConstructor; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/dashboard") @RequiredArgsConstructor
public class DashboardController {
    final DashboardService service;
    @GetMapping("/stats") public R<DashboardStatsVO> stats(){return R.ok(service.getStats());}
}