package com.example.Taskora.service.unit;

import com.example.Taskora.dto.request.CreateInvoiceLineRequest;
import com.example.Taskora.dto.request.CreateInvoiceRequest;
import com.example.Taskora.dto.response.InvoiceResponse;
import com.example.Taskora.entity.*;
import com.example.Taskora.exception.ResourceNotFoundException;
import com.example.Taskora.mapper.InvoiceLineMapper;
import com.example.Taskora.mapper.InvoiceMapper;
import com.example.Taskora.repository.InvoiceRepository;
import com.example.Taskora.repository.ProjectRepository;
import com.example.Taskora.repository.UserRepository;
import com.example.Taskora.service.impl.InvoiceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InvoiceServiceImpl Unit Tests")
class InvoiceServiceImplTest {

    @Mock private InvoiceRepository invoiceRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private UserRepository userRepository;
    @Mock private InvoiceMapper invoiceMapper;
    @Mock private InvoiceLineMapper invoiceLineMapper;

    @InjectMocks
    private InvoiceServiceImpl invoiceService;

    private User user;
    private Project project;
    private Invoice invoice;
    private InvoiceResponse invoiceResponse;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L).fullName("Alice").email("alice@example.com")
                .role(Role.ROLE_FREELANCER).status(UserStatus.ACTIVE).build();

        Client client = Client.builder().id(10L).name("Acme Corp").user(user).build();

        project = Project.builder()
                .id(100L).title("Website Redesign")
                .status(ProjectStatus.EN_COURS).client(client).user(user).build();

        invoice = Invoice.builder()
                .id(500L)
                .invoiceNumber("INV-202603-0001")
                .issueDate(LocalDate.now())
                .status(InvoiceStatus.EN_ATTENTE)
                .taxRate(new BigDecimal("20.00"))
                .project(project).user(user).build();

        invoiceResponse = new InvoiceResponse();
        invoiceResponse.setId(500L);
        invoiceResponse.setInvoiceNumber("INV-202603-0001");
        invoiceResponse.setStatus(InvoiceStatus.EN_ATTENTE);
        invoiceResponse.setProjectId(100L);
        invoiceResponse.setUserId(1L);
        invoiceResponse.setTaxRate(new BigDecimal("20.00"));
        invoiceResponse.setTotalHT(new BigDecimal("1000.00"));
        invoiceResponse.setTotalTVA(new BigDecimal("200.00"));
        invoiceResponse.setTotalTTC(new BigDecimal("1200.00"));
    }

    // ---- createInvoice ----

    @Test
    @DisplayName("createInvoice: computes totals and saves invoice with EN_ATTENTE status")
    void createInvoice_validRequest_computesTotalsAndSaves() {
        CreateInvoiceLineRequest lineReq = new CreateInvoiceLineRequest();
        lineReq.setDescription("Design services");
        lineReq.setQuantity(10);
        lineReq.setUnitPrice(new BigDecimal("100.00"));

        CreateInvoiceRequest request = new CreateInvoiceRequest();
        request.setProjectId(100L);
        request.setUserId(1L);
        request.setIssueDate(LocalDate.now());
        request.setTaxRate(new BigDecimal("20.00"));
        request.setLines(List.of(lineReq));

        InvoiceLine line = InvoiceLine.builder()
                .description("Design services").quantity(10)
                .unitPrice(new BigDecimal("100.00")).build();

        when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(invoiceMapper.toEntity(request)).thenReturn(invoice);
        when(invoiceLineMapper.toEntity(lineReq)).thenReturn(line);
        when(invoiceRepository.countByYearAndMonth(anyInt(), anyInt())).thenReturn(0L);
        when(invoiceRepository.existsByInvoiceNumber(anyString())).thenReturn(false);
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);
        when(invoiceMapper.toResponse(invoice)).thenReturn(invoiceResponse);

        InvoiceResponse result = invoiceService.createInvoice(request);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(InvoiceStatus.EN_ATTENTE);
        // totalHT = 10 * 100 = 1000, TVA = 200, TTC = 1200
        assertThat(invoice.getTotalHT()).isEqualByComparingTo("1000.00");
        assertThat(invoice.getTotalTVA()).isEqualByComparingTo("200.00");
        assertThat(invoice.getTotalTTC()).isEqualByComparingTo("1200.00");
        verify(invoiceRepository).save(invoice);
    }

    @Test
    @DisplayName("createInvoice: throws ResourceNotFoundException when project not found")
    void createInvoice_projectNotFound_throwsException() {
        CreateInvoiceRequest request = new CreateInvoiceRequest();
        request.setProjectId(999L);
        request.setUserId(1L);

        when(projectRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.createInvoice(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("createInvoice: throws ResourceNotFoundException when user not found")
    void createInvoice_userNotFound_throwsException() {
        CreateInvoiceRequest request = new CreateInvoiceRequest();
        request.setProjectId(100L);
        request.setUserId(999L);

        when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.createInvoice(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- getInvoiceById ----

    @Test
    @DisplayName("getInvoiceById: returns invoice when it exists")
    void getInvoiceById_exists_returnsInvoice() {
        when(invoiceRepository.findById(500L)).thenReturn(Optional.of(invoice));
        when(invoiceMapper.toResponse(invoice)).thenReturn(invoiceResponse);

        InvoiceResponse result = invoiceService.getInvoiceById(500L);

        assertThat(result.getId()).isEqualTo(500L);
    }

    @Test
    @DisplayName("getInvoiceById: throws ResourceNotFoundException when not found")
    void getInvoiceById_notFound_throwsException() {
        when(invoiceRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.getInvoiceById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- getAllInvoices ----

    @Test
    @DisplayName("getAllInvoices: returns paginated invoices")
    void getAllInvoices_returnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        when(invoiceRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(invoice)));
        when(invoiceMapper.toResponse(invoice)).thenReturn(invoiceResponse);

        Page<InvoiceResponse> result = invoiceService.getAllInvoices(pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    // ---- getInvoicesByUser ----

    @Test
    @DisplayName("getInvoicesByUser: returns invoices belonging to user")
    void getInvoicesByUser_returnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        when(invoiceRepository.findByUserId(1L, pageable)).thenReturn(new PageImpl<>(List.of(invoice)));
        when(invoiceMapper.toResponse(invoice)).thenReturn(invoiceResponse);

        Page<InvoiceResponse> result = invoiceService.getInvoicesByUser(1L, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    // ---- markAsPaid ----

    @Test
    @DisplayName("markAsPaid: sets invoice status to PAYEE")
    void markAsPaid_existingInvoice_setsPaidStatus() {
        InvoiceResponse paidResponse = new InvoiceResponse();
        paidResponse.setStatus(InvoiceStatus.PAYEE);

        when(invoiceRepository.findById(500L)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(invoice)).thenReturn(invoice);
        when(invoiceMapper.toResponse(invoice)).thenReturn(paidResponse);

        InvoiceResponse result = invoiceService.markAsPaid(500L);

        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAYEE);
        assertThat(result.getStatus()).isEqualTo(InvoiceStatus.PAYEE);
    }

    @Test
    @DisplayName("markAsPaid: throws ResourceNotFoundException when invoice not found")
    void markAsPaid_notFound_throwsException() {
        when(invoiceRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.markAsPaid(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- cancelInvoice ----

    @Test
    @DisplayName("cancelInvoice: sets invoice status to ANNULEE")
    void cancelInvoice_existingInvoice_setsCancelledStatus() {
        InvoiceResponse cancelledResponse = new InvoiceResponse();
        cancelledResponse.setStatus(InvoiceStatus.ANNULEE);

        when(invoiceRepository.findById(500L)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(invoice)).thenReturn(invoice);
        when(invoiceMapper.toResponse(invoice)).thenReturn(cancelledResponse);

        InvoiceResponse result = invoiceService.cancelInvoice(500L);

        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.ANNULEE);
        assertThat(result.getStatus()).isEqualTo(InvoiceStatus.ANNULEE);
    }

    // ---- deleteInvoice ----

    @Test
    @DisplayName("deleteInvoice: deletes existing invoice")
    void deleteInvoice_existingId_deletesInvoice() {
        when(invoiceRepository.existsById(500L)).thenReturn(true);

        invoiceService.deleteInvoice(500L);

        verify(invoiceRepository).deleteById(500L);
    }

    @Test
    @DisplayName("deleteInvoice: throws ResourceNotFoundException when invoice not found")
    void deleteInvoice_notFound_throwsException() {
        when(invoiceRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> invoiceService.deleteInvoice(999L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(invoiceRepository, never()).deleteById(any());
    }
}
