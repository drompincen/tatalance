package com.tatalance.stats;

import com.tatalance.client.ClientRepository;
import com.tatalance.driver.DriverRepository;
import com.tatalance.invoice.Invoice;
import com.tatalance.invoice.InvoiceRepository;
import com.tatalance.invoice.InvoiceStatus;
import com.tatalance.ride.Ride;
import com.tatalance.ride.RideRepository;
import com.tatalance.ride.RideStatus;
import com.tatalance.user.AuthHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StatsController.class)
@AutoConfigureMockMvc(addFilters = false)
class StatsControllerTest {

    private static final String TEST_USER_ID = "user123";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ClientRepository clientRepo;

    @MockBean
    private DriverRepository driverRepo;

    @MockBean
    private RideRepository rideRepo;

    @MockBean
    private InvoiceRepository invoiceRepo;

    @MockBean
    private AuthHelper authHelper;

    @BeforeEach
    void setUp() {
        when(authHelper.getCurrentUserId()).thenReturn(TEST_USER_ID);
        when(clientRepo.countByUserId(eq(TEST_USER_ID))).thenReturn(5L);
        when(driverRepo.countByUserId(eq(TEST_USER_ID))).thenReturn(2L);
        when(rideRepo.countByUserId(eq(TEST_USER_ID))).thenReturn(10L);
        when(rideRepo.countByUserIdAndPickupDateTimeBetween(any(), any(), any())).thenReturn(3L);
        when(rideRepo.countByUserIdAndStatus(any(), any())).thenReturn(1L);

        // Rich data to cover stream branches (null totals, month filter, payouts >0 / ==0 )
        Instant now = Instant.now();
        Instant monthStart = LocalDate.now(ZoneId.systemDefault()).withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        Invoice paid1 = new Invoice();
        paid1.setTotal(new BigDecimal("100.00"));
        paid1.setCreatedAt(now);
        Invoice paid2 = new Invoice();
        paid2.setTotal(null);
        paid2.setCreatedAt(monthStart.minusSeconds(3600)); // before month
        when(invoiceRepo.findByUserIdAndStatus(eq(TEST_USER_ID), eq(InvoiceStatus.PAID)))
                .thenReturn(List.of(paid1, paid2));

        Invoice out1 = new Invoice();
        out1.setTotal(new BigDecimal("50.00"));
        when(invoiceRepo.findByUserIdAndStatus(eq(TEST_USER_ID), eq(InvoiceStatus.OUTSTANDING)))
                .thenReturn(List.of(out1));

        Ride r1 = new Ride();
        r1.setDriverPayout(new BigDecimal("25.00"));
        Ride r2 = new Ride();
        r2.setDriverPayout(BigDecimal.ZERO);
        Ride r3 = new Ride();
        r3.setDriverPayout(null);
        when(rideRepo.findByUserIdAndStatusAndPayoutPaid(eq(TEST_USER_ID), eq(RideStatus.COMPLETED), eq(false)))
                .thenReturn(List.of(r1, r2, r3));
    }

    @Test
    void stats_shouldReturnData() throws Exception {
        mockMvc.perform(get("/api/stats"))
                .andExpect(status().isOk());
    }

    @Test
    void stats_withRichData_coversStreamBranches() throws Exception {
        // Extra call exercises the lambdas/filters in revenue + payouts
        mockMvc.perform(get("/api/stats"))
                .andExpect(status().isOk());
    }
}
