package com.example.Taskora.controller;

import com.example.Taskora.dto.response.AdminDashboardResponse;
import com.example.Taskora.dto.response.FreelancerDashboardResponse;
import com.example.Taskora.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Dashboard metrics for freelancers and admins")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/freelancer/{userId}")
    @Operation(summary = "Get freelancer dashboard metrics: clients, active projects, pending invoices, monthly revenue (last 6 months)")
    public ResponseEntity<FreelancerDashboardResponse> getFreelancerDashboard(@PathVariable Long userId) {
        return ResponseEntity.ok(dashboardService.getFreelancerDashboard(userId));
    }

    @GetMapping("/admin")
    @Operation(summary = "Get admin dashboard metrics: freelancer stats, project stats, platform revenue (last 12 months)")
    public ResponseEntity<AdminDashboardResponse> getAdminDashboard() {
        return ResponseEntity.ok(dashboardService.getAdminDashboard());
    }
}
