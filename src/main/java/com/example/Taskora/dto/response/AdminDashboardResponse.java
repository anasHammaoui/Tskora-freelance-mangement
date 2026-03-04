package com.example.Taskora.dto.response;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminDashboardResponse {
    private long totalFreelancers;
    private long activeFreelancers;
    private long bannedFreelancers;
    private long totalProjects;
    private long activeProjects;
    private long completedProjects;
    private BigDecimal totalPlatformRevenue;
    // Monthly revenue across platform
    private List<FreelancerDashboardResponse.MonthlyRevenueEntry> monthlyPlatformRevenue;
    // Freelancer status breakdown
    private UserStatusBreakdown freelancerStatusBreakdown;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserStatusBreakdown {
        private long active;
        private long banned;
    }
}
