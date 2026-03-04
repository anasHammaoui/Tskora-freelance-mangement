package com.example.Taskora.dto.response;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FreelancerDashboardResponse {
    private long totalClients;
    private long activeProjects;
    private long pendingInvoices;
    private BigDecimal currentMonthRevenue;
    // Monthly revenue for last 6 months: key=YYYY-MM, value=amount
    private List<MonthlyRevenueEntry> last6MonthsRevenue;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MonthlyRevenueEntry {
        private String month; // "YYYY-MM"
        private BigDecimal revenue;
    }
}
