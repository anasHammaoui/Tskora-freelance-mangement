package com.example.Taskora.service.impl;

import com.example.Taskora.dto.response.AdminDashboardResponse;
import com.example.Taskora.dto.response.FreelancerDashboardResponse;
import com.example.Taskora.entity.InvoiceStatus;
import com.example.Taskora.entity.ProjectStatus;
import com.example.Taskora.entity.Role;
import com.example.Taskora.entity.UserStatus;
import com.example.Taskora.repository.InvoiceRepository;
import com.example.Taskora.repository.ClientRepository;
import com.example.Taskora.repository.ProjectRepository;
import com.example.Taskora.repository.UserRepository;
import com.example.Taskora.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final ProjectRepository projectRepository;
    private final InvoiceRepository invoiceRepository;

    @Override
    public FreelancerDashboardResponse getFreelancerDashboard(Long userId) {
        long totalClients = clientRepository.findByUserId(userId, Pageable.unpaged()).getTotalElements();
        long activeProjects = projectRepository.countByUserIdAndStatus(userId, ProjectStatus.EN_COURS);
        long pendingInvoices = invoiceRepository.countByUserIdAndStatus(userId, InvoiceStatus.EN_ATTENTE);

        // Revenue for current month
        LocalDate now = LocalDate.now();
        LocalDate startOfMonth = now.withDayOfMonth(1);
        LocalDate endOfMonth = now.withDayOfMonth(now.lengthOfMonth());
        BigDecimal currentMonthRevenue = invoiceRepository.sumPaidRevenueByUserAndPeriod(userId, startOfMonth, endOfMonth);

        // Last 6 months monthly revenue
        LocalDate sixMonthsAgo = now.minusMonths(5).withDayOfMonth(1);
        List<Object[]> rawData = invoiceRepository.findMonthlyRevenueByUser(userId, sixMonthsAgo);
        List<FreelancerDashboardResponse.MonthlyRevenueEntry> monthlyRevenue = buildMonthlyRevenue(rawData, 6);

        FreelancerDashboardResponse response = new FreelancerDashboardResponse();
        response.setTotalClients(totalClients);
        response.setActiveProjects(activeProjects);
        response.setPendingInvoices(pendingInvoices);
        response.setCurrentMonthRevenue(currentMonthRevenue != null ? currentMonthRevenue : BigDecimal.ZERO);
        response.setLast6MonthsRevenue(monthlyRevenue);
        return response;
    }

    @Override
    public AdminDashboardResponse getAdminDashboard() {
        long totalFreelancers = userRepository.countByRole(Role.ROLE_FREELANCER);
        long activeFreelancers = userRepository.countByRoleAndStatus(Role.ROLE_FREELANCER, UserStatus.ACTIVE);
        long bannedFreelancers = userRepository.countByRoleAndStatus(Role.ROLE_FREELANCER, UserStatus.BANNED);
        long totalProjects = projectRepository.count();
        long activeProjects = projectRepository.countByStatus(ProjectStatus.EN_COURS);
        long completedProjects = projectRepository.countByStatus(ProjectStatus.TERMINE);

        // Total platform revenue
        LocalDate firstDay = LocalDate.of(2000, 1, 1);
        LocalDate today = LocalDate.now();
        BigDecimal totalPlatformRevenue = invoiceRepository.sumTotalPaidRevenueByPeriod(firstDay, today);

        // Monthly revenue for last 12 months
        LocalDate twelveMonthsAgo = today.minusMonths(11).withDayOfMonth(1);
        List<Object[]> rawData = invoiceRepository.findMonthlyRevenueGlobal(twelveMonthsAgo);
        List<FreelancerDashboardResponse.MonthlyRevenueEntry> monthlyRevenue = buildMonthlyRevenue(rawData, 12);

        AdminDashboardResponse.UserStatusBreakdown breakdown =
                new AdminDashboardResponse.UserStatusBreakdown(activeFreelancers, bannedFreelancers);

        AdminDashboardResponse response = new AdminDashboardResponse();
        response.setTotalFreelancers(totalFreelancers);
        response.setActiveFreelancers(activeFreelancers);
        response.setBannedFreelancers(bannedFreelancers);
        response.setTotalProjects(totalProjects);
        response.setActiveProjects(activeProjects);
        response.setCompletedProjects(completedProjects);
        response.setTotalPlatformRevenue(totalPlatformRevenue != null ? totalPlatformRevenue : BigDecimal.ZERO);
        response.setMonthlyPlatformRevenue(monthlyRevenue);
        response.setFreelancerStatusBreakdown(breakdown);
        return response;
    }

    /**
     * Builds a complete list of monthly revenue entries for the last N months,
     * filling zeros for months without data.
     */
    private List<FreelancerDashboardResponse.MonthlyRevenueEntry> buildMonthlyRevenue(
            List<Object[]> rawData, int months) {
        // rawData: [0] = LocalDate (issue date), [1] = BigDecimal (sum)
        Map<String, BigDecimal> revenueMap = rawData.stream().collect(Collectors.toMap(
                row -> {
                    LocalDate d = (LocalDate) row[0];
                    return YearMonth.of(d.getYear(), d.getMonthValue())
                            .format(DateTimeFormatter.ofPattern("yyyy-MM"));
                },
                row -> (BigDecimal) row[1],
                BigDecimal::add
        ));

        List<FreelancerDashboardResponse.MonthlyRevenueEntry> result = new ArrayList<>();
        YearMonth current = YearMonth.now();
        for (int i = months - 1; i >= 0; i--) {
            YearMonth ym = current.minusMonths(i);
            String key = ym.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            BigDecimal revenue = revenueMap.getOrDefault(key, BigDecimal.ZERO);
            result.add(new FreelancerDashboardResponse.MonthlyRevenueEntry(key, revenue));
        }
        return result;
    }
}
