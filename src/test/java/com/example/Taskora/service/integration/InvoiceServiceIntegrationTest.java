package com.example.Taskora.service.integration;

import com.example.Taskora.dto.request.*;
import com.example.Taskora.dto.response.InvoiceResponse;
import com.example.Taskora.entity.InvoiceStatus;
import com.example.Taskora.entity.ProjectStatus;
import com.example.Taskora.exception.ResourceNotFoundException;
import com.example.Taskora.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
@DisplayName("InvoiceService Integration Tests")
class InvoiceServiceIntegrationTest {

    @Autowired private InvoiceService invoiceService;
    @Autowired private UserService userService;
    @Autowired private ClientService clientService;
    @Autowired private ProjectService projectService;

    private Long userId;
    private Long projectId;

    @BeforeEach
    void setUpPrerequisites() {
        CreateUserRequest userReq = new CreateUserRequest();
        userReq.setFullName("Invoice Tester");
        userReq.setEmail("invoice-tester@example.com");
        userReq.setPassword("password");
        userId = userService.createUser(userReq).getId();

        CreateClientRequest clientReq = new CreateClientRequest();
        clientReq.setName("Invoice Client");
        clientReq.setEmail("invoice-client@example.com");
        clientReq.setUserId(userId);
        Long clientId = clientService.createClient(clientReq).getId();

        CreateProjectRequest projectReq = new CreateProjectRequest();
        projectReq.setTitle("Invoice Project");
        projectReq.setStatus(ProjectStatus.EN_COURS);
        projectReq.setBudget(new BigDecimal("20000.00"));
        projectReq.setStartDate(LocalDate.now());
        projectReq.setClientId(clientId);
        projectReq.setUserId(userId);
        projectId = projectService.createProject(projectReq).getId();
    }

    private CreateInvoiceRequest buildInvoiceRequest(BigDecimal taxRate) {
        CreateInvoiceLineRequest line1 = new CreateInvoiceLineRequest();
        line1.setDescription("Development services");
        line1.setQuantity(10);
        line1.setUnitPrice(new BigDecimal("100.00"));

        CreateInvoiceLineRequest line2 = new CreateInvoiceLineRequest();
        line2.setDescription("Design services");
        line2.setQuantity(5);
        line2.setUnitPrice(new BigDecimal("80.00"));

        CreateInvoiceRequest req = new CreateInvoiceRequest();
        req.setIssueDate(LocalDate.now());
        req.setDueDate(LocalDate.now().plusDays(30));
        req.setTaxRate(taxRate);
        req.setProjectId(projectId);
        req.setUserId(userId);
        req.setLines(List.of(line1, line2));
        return req;
    }

    // ---- createInvoice ----

    @Test
    @DisplayName("createInvoice: persists invoice with correct tax calculations")
    void createInvoice_correctTaxCalculations() {
        // line1: 10 * 100 = 1000 HT; line2: 5 * 80 = 400 HT -> totalHT = 1400
        // TVA = 1400 * 20% = 280; TTC = 1680
        InvoiceResponse result = invoiceService.createInvoice(buildInvoiceRequest(new BigDecimal("20.00")));

        assertThat(result.getId()).isNotNull();
        assertThat(result.getInvoiceNumber()).isNotBlank();
        assertThat(result.getStatus()).isEqualTo(InvoiceStatus.EN_ATTENTE);
        assertThat(result.getTaxRate()).isEqualByComparingTo("20.00");
        assertThat(result.getTotalHT()).isEqualByComparingTo("1400.00");
        assertThat(result.getTotalTVA()).isEqualByComparingTo("280.00");
        assertThat(result.getTotalTTC()).isEqualByComparingTo("1680.00");
        assertThat(result.getProjectId()).isEqualTo(projectId);
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getLines()).hasSize(2);
    }

    @Test
    @DisplayName("createInvoice: correctly computes totals with 0% tax rate")
    void createInvoice_zeroTaxRate_noTVA() {
        // 0% tax: TVA = 0, TTC = HT = 1400
        InvoiceResponse result = invoiceService.createInvoice(buildInvoiceRequest(BigDecimal.ZERO));

        assertThat(result.getTaxRate()).isEqualByComparingTo("0");
        assertThat(result.getTotalHT()).isEqualByComparingTo("1400.00");
        assertThat(result.getTotalTVA()).isEqualByComparingTo("0.00");
        assertThat(result.getTotalTTC()).isEqualByComparingTo("1400.00");
    }

    @Test
    @DisplayName("createInvoice: generates unique invoice number")
    void createInvoice_generatesUniqueInvoiceNumber() {
        InvoiceResponse inv1 = invoiceService.createInvoice(buildInvoiceRequest(new BigDecimal("20.00")));
        InvoiceResponse inv2 = invoiceService.createInvoice(buildInvoiceRequest(new BigDecimal("20.00")));

        assertThat(inv1.getInvoiceNumber()).isNotEqualTo(inv2.getInvoiceNumber());
    }

    @Test
    @DisplayName("createInvoice: throws ResourceNotFoundException when project does not exist")
    void createInvoice_projectNotFound_throwsException() {
        CreateInvoiceRequest req = buildInvoiceRequest(new BigDecimal("20.00"));
        req.setProjectId(Long.MAX_VALUE);

        assertThatThrownBy(() -> invoiceService.createInvoice(req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("createInvoice: throws ResourceNotFoundException when user does not exist")
    void createInvoice_userNotFound_throwsException() {
        CreateInvoiceRequest req = buildInvoiceRequest(new BigDecimal("20.00"));
        req.setUserId(Long.MAX_VALUE);

        assertThatThrownBy(() -> invoiceService.createInvoice(req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- getInvoiceById ----

    @Test
    @DisplayName("getInvoiceById: retrieves persisted invoice with lines")
    void getInvoiceById_returnsInvoiceWithLines() {
        InvoiceResponse created = invoiceService.createInvoice(buildInvoiceRequest(new BigDecimal("20.00")));

        InvoiceResponse found = invoiceService.getInvoiceById(created.getId());

        assertThat(found.getId()).isEqualTo(created.getId());
        assertThat(found.getLines()).hasSize(2);
    }

    @Test
    @DisplayName("getInvoiceById: throws ResourceNotFoundException for unknown id")
    void getInvoiceById_notFound_throwsException() {
        assertThatThrownBy(() -> invoiceService.getInvoiceById(Long.MAX_VALUE))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- getAllInvoices ----

    @Test
    @DisplayName("getAllInvoices: returns paginated list of all invoices")
    void getAllInvoices_returnsPage() {
        invoiceService.createInvoice(buildInvoiceRequest(new BigDecimal("20.00")));
        invoiceService.createInvoice(buildInvoiceRequest(new BigDecimal("10.00")));

        Page<InvoiceResponse> page = invoiceService.getAllInvoices(PageRequest.of(0, 50));

        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(2);
    }

    // ---- getInvoicesByUser ----

    @Test
    @DisplayName("getInvoicesByUser: returns only invoices belonging to the user")
    void getInvoicesByUser_returnsUserInvoices() {
        invoiceService.createInvoice(buildInvoiceRequest(new BigDecimal("20.00")));

        Page<InvoiceResponse> page = invoiceService.getInvoicesByUser(userId, PageRequest.of(0, 10));

        assertThat(page.getContent()).allMatch(inv -> inv.getUserId().equals(userId));
    }

    // ---- getInvoicesByProject ----

    @Test
    @DisplayName("getInvoicesByProject: returns invoices for the given project")
    void getInvoicesByProject_returnsProjectInvoices() {
        invoiceService.createInvoice(buildInvoiceRequest(new BigDecimal("20.00")));

        Page<InvoiceResponse> page = invoiceService.getInvoicesByProject(projectId, PageRequest.of(0, 10));

        assertThat(page.getContent()).allMatch(inv -> inv.getProjectId().equals(projectId));
    }

    // ---- getInvoicesByUserAndStatus ----

    @Test
    @DisplayName("getInvoicesByUserAndStatus: filters invoices by user and status")
    void getInvoicesByUserAndStatus_filtersCorrectly() {
        InvoiceResponse inv1 = invoiceService.createInvoice(buildInvoiceRequest(new BigDecimal("20.00")));
        InvoiceResponse inv2 = invoiceService.createInvoice(buildInvoiceRequest(new BigDecimal("20.00")));
        invoiceService.markAsPaid(inv1.getId());

        Page<InvoiceResponse> pending = invoiceService.getInvoicesByUserAndStatus(
                userId, InvoiceStatus.EN_ATTENTE, PageRequest.of(0, 10));
        Page<InvoiceResponse> paid = invoiceService.getInvoicesByUserAndStatus(
                userId, InvoiceStatus.PAYEE, PageRequest.of(0, 10));

        assertThat(pending.getContent()).anyMatch(inv -> inv.getId().equals(inv2.getId()));
        assertThat(paid.getContent()).anyMatch(inv -> inv.getId().equals(inv1.getId()));
    }

    // ---- markAsPaid ----

    @Test
    @DisplayName("markAsPaid: changes invoice status to PAYEE")
    void markAsPaid_setsStatusToPaid() {
        InvoiceResponse created = invoiceService.createInvoice(buildInvoiceRequest(new BigDecimal("20.00")));

        InvoiceResponse paid = invoiceService.markAsPaid(created.getId());

        assertThat(paid.getStatus()).isEqualTo(InvoiceStatus.PAYEE);
    }

    @Test
    @DisplayName("markAsPaid: throws ResourceNotFoundException for unknown id")
    void markAsPaid_notFound_throwsException() {
        assertThatThrownBy(() -> invoiceService.markAsPaid(Long.MAX_VALUE))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- cancelInvoice ----

    @Test
    @DisplayName("cancelInvoice: changes invoice status to ANNULEE")
    void cancelInvoice_setsStatusToCancelled() {
        InvoiceResponse created = invoiceService.createInvoice(buildInvoiceRequest(new BigDecimal("20.00")));

        InvoiceResponse cancelled = invoiceService.cancelInvoice(created.getId());

        assertThat(cancelled.getStatus()).isEqualTo(InvoiceStatus.ANNULEE);
    }

    // ---- deleteInvoice ----

    @Test
    @DisplayName("deleteInvoice: removes invoice and its lines from database")
    void deleteInvoice_removesInvoice() {
        InvoiceResponse created = invoiceService.createInvoice(buildInvoiceRequest(new BigDecimal("20.00")));

        invoiceService.deleteInvoice(created.getId());

        assertThatThrownBy(() -> invoiceService.getInvoiceById(created.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("deleteInvoice: throws ResourceNotFoundException for unknown id")
    void deleteInvoice_notFound_throwsException() {
        assertThatThrownBy(() -> invoiceService.deleteInvoice(Long.MAX_VALUE))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
