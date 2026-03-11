package com.example.Taskora.service.unit;

import com.example.Taskora.dto.response.AdminDashboardResponse;
import com.example.Taskora.dto.response.FreelancerDashboardResponse;
import com.example.Taskora.dto.response.MonthlyRevenueDetailResponse;
import com.example.Taskora.entity.InvoiceStatus;
import com.example.Taskora.entity.ProjectStatus;
import com.example.Taskora.entity.Role;
import com.example.Taskora.entity.UserStatus;
import com.example.Taskora.repository.ClientRepository;
import com.example.Taskora.repository.InvoiceRepository;
import com.example.Taskora.repository.ProjectRepository;
import com.example.Taskora.repository.UserRepository;
import com.example.Taskora.service.impl.DashboardServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardServiceImpl Unit Tests")
class DashboardServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private InvoiceRepository invoiceRepository;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    // ---- getFreelancerDashboard ----

    @Test
    @DisplayName("getFreelancerDashboard: returns correct stats for existing user")
    void getFreelancerDashboard_returnsCorrectStats() {
        Long userId = 1L;
        LocalDate now = LocalDate.now();
        LocalDate start = now.withDayOfMonth(1);
        LocalDate end = now.withDayOfMonth(now.lengthOfMonth());

        when(clientRepository.findByUserId(eq(userId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList(), Pageable.unpaged(), 5L));
        when(projectRepository.countByUserIdAndStatus(userId, ProjectStatus.EN_COURS)).thenReturn(3L);
        when(invoiceRepository.countByUserIdAndStatus(userId, InvoiceStatus.EN_ATTENTE)).thenReturn(2L);
        when(invoiceRepository.sumPaidRevenueByUserAndPeriod(eq(userId), any(), any(), eq(InvoiceStatus.PAYEE)))
                .thenReturn(new BigDecimal("1500.00"));
        when(invoiceRepository.findMonthlyRevenueByUser(eq(userId), any(), eq(InvoiceStatus.PAYEE)))
                .thenReturn(Collections.emptyList());

        FreelancerDashboardResponse response = dashboardService.getFreelancerDashboard(userId);

        assertThat(response).isNotNull();
        assertThat(response.getTotalClients()).isEqualTo(5L);
        assertThat(response.getActiveProjects()).isEqualTo(3L);
        assertThat(response.getPendingInvoices()).isEqualTo(2L);
        assertThat(response.getCurrentMonthRevenue()).isEqualByComparingTo("1500.00");
        assertThat(response.getLast6MonthsRevenue()).hasSize(6);
    }

    @Test
    @DisplayName("getFreelancerDashboard: handles null revenue (no paid invoices) as ZERO")
    void getFreelancerDashboard_nullRevenue_returnsZero() {
        Long userId = 2L;

        when(clientRepository.findByUserId(eq(userId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList(), Pageable.unpaged(), 0L));
        when(projectRepository.countByUserIdAndStatus(userId, ProjectStatus.EN_COURS)).thenReturn(0L);
        when(invoiceRepository.countByUserIdAndStatus(userId, InvoiceStatus.EN_ATTENTE)).thenReturn(0L);
        when(invoiceRepository.sumPaidRevenueByUserAndPeriod(eq(userId), any(), any(), eq(InvoiceStatus.PAYEE)))
                .thenReturn(null);
        when(invoiceRepository.findMonthlyRevenueByUser(eq(userId), any(), eq(InvoiceStatus.PAYEE)))
                .thenReturn(Collections.emptyList());

        FreelancerDashboardResponse response = dashboardService.getFreelancerDashboard(userId);

        assertThat(response.getCurrentMonthRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("getFreelancerDashboard: fills zero-revenue months for missing months")
    void getFreelancerDashboard_missingMonths_filledWithZero() {
        Long userId = 3L;

        when(clientRepository.findByUserId(eq(userId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList(), Pageable.unpaged(), 0L));
        when(projectRepository.countByUserIdAndStatus(any(), any())).thenReturn(0L);
        when(invoiceRepository.countByUserIdAndStatus(any(), any())).thenReturn(0L);
        when(invoiceRepository.sumPaidRevenueByUserAndPeriod(any(), any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(invoiceRepository.findMonthlyRevenueByUser(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        FreelancerDashboardResponse response = dashboardService.getFreelancerDashboard(userId);

        assertThat(response.getLast6MonthsRevenue()).hasSize(6);
        response.getLast6MonthsRevenue().forEach(entry ->
                assertThat(entry.getRevenue()).isEqualByComparingTo(BigDecimal.ZERO));
    }

    // ---- getAdminDashboard ----

    @Test
    @DisplayName("getAdminDashboard: returns aggregated platform stats")
    void getAdminDashboard_returnsCorrectStats() {
        when(userRepository.countByRole(Role.ROLE_FREELANCER)).thenReturn(50L);
        when(userRepository.countByRoleAndStatus(Role.ROLE_FREELANCER, UserStatus.ACTIVE)).thenReturn(40L);
        when(userRepository.countByRoleAndStatus(Role.ROLE_FREELANCER, UserStatus.BANNED)).thenReturn(10L);
        when(projectRepository.count()).thenReturn(200L);
        when(projectRepository.countByStatus(ProjectStatus.EN_COURS)).thenReturn(80L);
        when(projectRepository.countByStatus(ProjectStatus.TERMINE)).thenReturn(120L);
        when(invoiceRepository.sumTotalPaidRevenueByPeriod(any(), any(), eq(InvoiceStatus.PAYEE)))
                .thenReturn(new BigDecimal("50000.00"));
        when(invoiceRepository.findMonthlyRevenueGlobal(any(), eq(InvoiceStatus.PAYEE)))
                .thenReturn(Collections.emptyList());

        AdminDashboardResponse response = dashboardService.getAdminDashboard();

        assertThat(response).isNotNull();
        assertThat(response.getTotalFreelancers()).isEqualTo(50L);
        assertThat(response.getActiveFreelancers()).isEqualTo(40L);
        assertThat(response.getBannedFreelancers()).isEqualTo(10L);
        assertThat(response.getTotalProjects()).isEqualTo(200L);
        assertThat(response.getActiveProjects()).isEqualTo(80L);
        assertThat(response.getCompletedProjects()).isEqualTo(120L);
        assertThat(response.getTotalPlatformRevenue()).isEqualByComparingTo("50000.00");
        assertThat(response.getMonthlyPlatformRevenue()).hasSize(12);
        assertThat(response.getFreelancerStatusBreakdown()).isNotNull();
        assertThat(response.getFreelancerStatusBreakdown().getActive()).isEqualTo(40L);
        assertThat(response.getFreelancerStatusBreakdown().getBanned()).isEqualTo(10L);
    }

    @Test
    @DisplayName("getAdminDashboard: handles null total revenue as ZERO")
    void getAdminDashboard_nullRevenue_returnsZero() {
        when(userRepository.countByRole(any())).thenReturn(0L);
        when(userRepository.countByRoleAndStatus(any(), any())).thenReturn(0L);
        when(projectRepository.count()).thenReturn(0L);
        when(projectRepository.countByStatus(any())).thenReturn(0L);
        when(invoiceRepository.sumTotalPaidRevenueByPeriod(any(), any(), any())).thenReturn(null);
        when(invoiceRepository.findMonthlyRevenueGlobal(any(), any())).thenReturn(Collections.emptyList());

        AdminDashboardResponse response = dashboardService.getAdminDashboard();

        assertThat(response.getTotalPlatformRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ---- getFreelancerMonthlyRevenueDetail ----

    @Test
    @DisplayName("getFreelancerMonthlyRevenueDetail: builds correct daily breakdown")
    void getFreelancerMonthlyRevenueDetail_buildsDailyBreakdown() {
        Long userId = 1L;
        int year = 2026;
        int month = 3;

        when(invoiceRepository.sumRevenueByUserAndMonth(userId, year, month, InvoiceStatus.PAYEE))
                .thenReturn(new BigDecimal("3000.00"));
        when(invoiceRepository.findDailyRevenueByUserAndMonth(userId, year, month, InvoiceStatus.PAYEE))
                .thenReturn(List.of(new Object[]{5, new BigDecimal("1500.00")},
                        new Object[]{15, new BigDecimal("1500.00")}));

        MonthlyRevenueDetailResponse response =
                dashboardService.getFreelancerMonthlyRevenueDetail(userId, year, month);

        assertThat(response).isNotNull();
        assertThat(response.getMonth()).isEqualTo("2026-03");
        assertThat(response.getTotalRevenue()).isEqualByComparingTo("3000.00");
        // March has 31 days
        assertThat(response.getDailyRevenue()).hasSize(31);
        assertThat(response.getDailyRevenue().get(4).getRevenue()).isEqualByComparingTo("1500.00"); // day 5
        assertThat(response.getDailyRevenue().get(14).getRevenue()).isEqualByComparingTo("1500.00"); // day 15
    }

    // ---- getAdminMonthlyRevenueDetail ----

    @Test
    @DisplayName("getAdminMonthlyRevenueDetail: builds correct daily breakdown for admin")
    void getAdminMonthlyRevenueDetail_buildsDailyBreakdown() {
        int year = 2026;
        int month = 2;

        when(invoiceRepository.sumRevenueByMonth(year, month, InvoiceStatus.PAYEE))
                .thenReturn(new BigDecimal("8000.00"));
        when(invoiceRepository.findDailyRevenueByMonth(year, month, InvoiceStatus.PAYEE))
                .thenReturn(Collections.emptyList());

        MonthlyRevenueDetailResponse response = dashboardService.getAdminMonthlyRevenueDetail(year, month);

        assertThat(response.getMonth()).isEqualTo("2026-02");
        assertThat(response.getTotalRevenue()).isEqualByComparingTo("8000.00");
        // February 2026 has 28 days
        assertThat(response.getDailyRevenue()).hasSize(28);
        response.getDailyRevenue().forEach(entry ->
                assertThat(entry.getRevenue()).isEqualByComparingTo(BigDecimal.ZERO));
    }
}
