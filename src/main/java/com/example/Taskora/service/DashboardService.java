package com.example.Taskora.service;

import com.example.Taskora.dto.response.AdminDashboardResponse;
import com.example.Taskora.dto.response.FreelancerDashboardResponse;

public interface DashboardService {
    FreelancerDashboardResponse getFreelancerDashboard(Long userId);
    AdminDashboardResponse getAdminDashboard();
}
