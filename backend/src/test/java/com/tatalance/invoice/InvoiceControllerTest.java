package com.tatalance.invoice;

import com.tatalance.SecurityConfig;
import com.tatalance.ride.Ride;
import com.tatalance.ride.RideRepository;
import com.tatalance.ride.RideStatus;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InvoiceController.class)
@Import(SecurityConfig.class)
@WithMockUser
class InvoiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InvoiceRepository invoiceRepository;

    @MockBean
    private RideRepository rideRepository;

    @MockBean
    private UserDetailsService userDetailsService;

    private Ride completedRide() {
        var ride = new Ride();
        ride.setId("ride001");
        ride.setClientId("cli001");
        ride.setClientName("Ana Torres");
        ride.setPickupDateTime(Instant.parse("2026-06-01T14:00:00Z"));
        ride.setPickupLocation("MIA");
        ride.setDropoffLocation("FLL");
        ride.setBasePrice(new BigDecimal("100.00"));
        ride.setTolls(new BigDecimal("5.00"));
        ride.setParking(new BigDecimal("10.00"));
        ride.setAdditionalCharges(new BigDecimal("20.00"));
        ride.setStatus(RideStatus.COMPLETED);
        ride.setTotalAmount(new BigDecimal("135.00"));
        return ride;
    }

    private Invoice sampleInvoice() {
        var inv = new Invoice();
        inv.setId("inv001");
        inv.setInvoiceNumber("INV-2026-001");
        inv.setClientId("cli001");
        inv.setClientName("Ana Torres");
        inv.setRideId("ride001");
        inv.setBaseCharge(new BigDecimal("100.00"));
        inv.setAdditionalCharges(new BigDecimal("35.00"));
        inv.setTax(new BigDecimal("10.80"));
        inv.setTotal(new BigDecimal("145.80"));
        inv.setStatus(InvoiceStatus.OUTSTANDING);
        inv.setCreatedAt(Instant.now());
        return inv;
    }

    @Test
    void should_createInvoice_fromCompletedRide() throws Exception {
        when(rideRepository.findById("ride001")).thenReturn(Optional.of(completedRide()));
        when(invoiceRepository.count()).thenReturn(0L);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> {
            Invoice i = inv.getArgument(0);
            i.setId("inv001");
            return i;
        });

        mockMvc.perform(post("/api/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rideId":"ride001"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invoiceNumber").value("INV-2026-001"))
                .andExpect(jsonPath("$.clientName").value("Ana Torres"))
                .andExpect(jsonPath("$.baseCharge").value(100.00))
                .andExpect(jsonPath("$.additionalCharges").value(35.00))
                .andExpect(jsonPath("$.tax").value(10.80))
                .andExpect(jsonPath("$.total").value(145.80))
                .andExpect(jsonPath("$.status").value("OUTSTANDING"));
    }

    @Test
    void should_return400_when_rideNotCompleted() throws Exception {
        var ride = completedRide();
        ride.setStatus(RideStatus.ASSIGNED);
        when(rideRepository.findById("ride001")).thenReturn(Optional.of(ride));

        mockMvc.perform(post("/api/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rideId":"ride001"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return400_when_rideNotFound() throws Exception {
        when(rideRepository.findById("unknown")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rideId":"unknown"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return400_when_rideIdMissing() throws Exception {
        mockMvc.perform(post("/api/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_listInvoices() throws Exception {
        when(invoiceRepository.findAll()).thenReturn(List.of(sampleInvoice()));

        mockMvc.perform(get("/api/invoices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].invoiceNumber").value("INV-2026-001"));
    }

    @Test
    void should_getInvoiceById() throws Exception {
        when(invoiceRepository.findById("inv001")).thenReturn(Optional.of(sampleInvoice()));

        mockMvc.perform(get("/api/invoices/inv001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientName").value("Ana Torres"));
    }

    @Test
    void should_return404_when_invoiceNotFound() throws Exception {
        when(invoiceRepository.findById("unknown")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/invoices/unknown"))
                .andExpect(status().isNotFound());
    }

    @Test
    void should_createInvoice_withNoExtras() throws Exception {
        var ride = completedRide();
        ride.setTolls(null);
        ride.setParking(null);
        ride.setAdditionalCharges(null);
        when(rideRepository.findById("ride001")).thenReturn(Optional.of(ride));
        when(invoiceRepository.count()).thenReturn(2L);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> {
            Invoice i = inv.getArgument(0);
            i.setId("inv003");
            return i;
        });

        mockMvc.perform(post("/api/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rideId":"ride001"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invoiceNumber").value("INV-2026-003"))
                .andExpect(jsonPath("$.baseCharge").value(100.00))
                .andExpect(jsonPath("$.additionalCharges").value(0))
                .andExpect(jsonPath("$.tax").value(8.00))
                .andExpect(jsonPath("$.total").value(108.00));
    }

    @Test
    void should_markOutstandingAsPaid() throws Exception {
        var invoice = sampleInvoice();
        when(invoiceRepository.findById("inv001")).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/api/invoices/inv001/mark-paid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    void should_togglePaidBackToOutstanding() throws Exception {
        var invoice = sampleInvoice();
        invoice.setStatus(InvoiceStatus.PAID);
        when(invoiceRepository.findById("inv001")).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/api/invoices/inv001/mark-paid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OUTSTANDING"));
    }

    @Test
    void should_return404_when_markingNonexistentInvoice() throws Exception {
        when(invoiceRepository.findById("unknown")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/invoices/unknown/mark-paid"))
                .andExpect(status().isNotFound());
    }
}
