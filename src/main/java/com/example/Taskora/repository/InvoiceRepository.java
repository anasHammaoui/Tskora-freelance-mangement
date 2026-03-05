package com.example.Taskora.repository;

import com.example.Taskora.entity.Invoice;
import com.example.Taskora.entity.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Page<Invoice> findByUserId(Long userId, Pageable pageable);
    Page<Invoice> findByProjectId(Long projectId, Pageable pageable);
    Page<Invoice> findByUserIdAndStatus(Long userId, InvoiceStatus status, Pageable pageable);
    Page<Invoice> findByStatus(InvoiceStatus status, Pageable pageable);
    long countByUserIdAndStatus(Long userId, InvoiceStatus status);

    @Query("SELECT COALESCE(SUM(i.totalTTC), 0) FROM Invoice i WHERE i.user.id = :userId AND i.status = :status AND i.issueDate >= :startDate AND i.issueDate <= :endDate")
    BigDecimal sumPaidRevenueByUserAndPeriod(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, @Param("status") InvoiceStatus status);

    @Query("SELECT COALESCE(SUM(i.totalTTC), 0) FROM Invoice i WHERE i.status = :status AND i.issueDate >= :startDate AND i.issueDate <= :endDate")
    BigDecimal sumTotalPaidRevenueByPeriod(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, @Param("status") InvoiceStatus status);

    @Query("SELECT FUNCTION('YEAR', i.issueDate), FUNCTION('MONTH', i.issueDate), COALESCE(SUM(i.totalTTC), 0) FROM Invoice i WHERE i.user.id = :userId AND i.status = :status AND i.issueDate >= :startDate GROUP BY FUNCTION('YEAR', i.issueDate), FUNCTION('MONTH', i.issueDate) ORDER BY FUNCTION('YEAR', i.issueDate) ASC, FUNCTION('MONTH', i.issueDate) ASC")
    List<Object[]> findMonthlyRevenueByUser(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("status") InvoiceStatus status);

    @Query("SELECT FUNCTION('YEAR', i.issueDate), FUNCTION('MONTH', i.issueDate), COALESCE(SUM(i.totalTTC), 0) FROM Invoice i WHERE i.status = :status AND i.issueDate >= :startDate GROUP BY FUNCTION('YEAR', i.issueDate), FUNCTION('MONTH', i.issueDate) ORDER BY FUNCTION('YEAR', i.issueDate) ASC, FUNCTION('MONTH', i.issueDate) ASC")
    List<Object[]> findMonthlyRevenueGlobal(@Param("startDate") LocalDate startDate, @Param("status") InvoiceStatus status);

    // Daily revenue breakdown for a specific month
    @Query("SELECT FUNCTION('DAY', i.issueDate), COALESCE(SUM(i.totalTTC), 0) FROM Invoice i WHERE i.user.id = :userId AND i.status = :status AND FUNCTION('YEAR', i.issueDate) = :year AND FUNCTION('MONTH', i.issueDate) = :month GROUP BY FUNCTION('DAY', i.issueDate) ORDER BY FUNCTION('DAY', i.issueDate) ASC")
    List<Object[]> findDailyRevenueByUserAndMonth(@Param("userId") Long userId, @Param("year") int year, @Param("month") int month, @Param("status") InvoiceStatus status);

    @Query("SELECT FUNCTION('DAY', i.issueDate), COALESCE(SUM(i.totalTTC), 0) FROM Invoice i WHERE i.status = :status AND FUNCTION('YEAR', i.issueDate) = :year AND FUNCTION('MONTH', i.issueDate) = :month GROUP BY FUNCTION('DAY', i.issueDate) ORDER BY FUNCTION('DAY', i.issueDate) ASC")
    List<Object[]> findDailyRevenueByMonth(@Param("year") int year, @Param("month") int month, @Param("status") InvoiceStatus status);

    // Total revenue for a specific month
    @Query("SELECT COALESCE(SUM(i.totalTTC), 0) FROM Invoice i WHERE i.user.id = :userId AND i.status = :status AND FUNCTION('YEAR', i.issueDate) = :year AND FUNCTION('MONTH', i.issueDate) = :month")
    BigDecimal sumRevenueByUserAndMonth(@Param("userId") Long userId, @Param("year") int year, @Param("month") int month, @Param("status") InvoiceStatus status);

    @Query("SELECT COALESCE(SUM(i.totalTTC), 0) FROM Invoice i WHERE i.status = :status AND FUNCTION('YEAR', i.issueDate) = :year AND FUNCTION('MONTH', i.issueDate) = :month")
    BigDecimal sumRevenueByMonth(@Param("year") int year, @Param("month") int month, @Param("status") InvoiceStatus status);

    boolean existsByInvoiceNumber(String invoiceNumber);

    @Query("SELECT COUNT(i) FROM Invoice i WHERE FUNCTION('YEAR', i.issueDate) = :year AND FUNCTION('MONTH', i.issueDate) = :month")
    long countByYearAndMonth(int year, int month);
}
