package com.example.Taskora.service;

import com.example.Taskora.dto.response.AdminDashboardResponse;
import com.example.Taskora.dto.response.FreelancerDashboardResponse;
import com.example.Taskora.dto.response.MonthlyRevenueDetailResponse;

public interface DashboardService {
    FreelancerDashboardResponse getFreelancerDashboard(Long userId);
    AdminDashboardResponse getAdminDashboard();
    MonthlyRevenueDetailResponse getFreelancerMonthlyRevenueDetail(Long userId, int year, int month);
    MonthlyRevenueDetailResponse getAdminMonthlyRevenueDetail(int year, int month);
}
