package com.example.Taskora.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MonthlyRevenueDetailResponse {

    private String month;          // "YYYY-MM"
    private BigDecimal totalRevenue;
    private List<DailyRevenueEntry> dailyRevenue;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DailyRevenueEntry {
        private String date;        // "YYYY-MM-DD"
        private BigDecimal revenue;
    }
}
