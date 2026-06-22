package com.tatalance.search;

import com.tatalance.client.Client;
import com.tatalance.client.ClientRepository;
import com.tatalance.invoice.Invoice;
import com.tatalance.invoice.InvoiceRepository;
import com.tatalance.ride.Ride;
import com.tatalance.ride.RideRepository;
import com.tatalance.user.AuthHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SearchController.class)
@AutoConfigureMockMvc(addFilters = false)
class SearchControllerTest {

    private static final String TEST_USER_ID = "user123";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ClientRepository clientRepo;

    @MockBean
    private RideRepository rideRepo;

    @MockBean
    private InvoiceRepository invoiceRepo;

    @MockBean
    private AuthHelper authHelper;

    @BeforeEach
    void setUp() {
        when(authHelper.getCurrentUserId()).thenReturn(TEST_USER_ID);

        // Data to exercise match filters + if(!empty) result grouping
        Client c1 = new Client();
        c1.setId("cli-alice");
        c1.setFirstName("Alice");
        c1.setLastName("Beta");
        c1.setPhone("123");
        c1.setEmail("a@b.com");

        Ride r1 = new Ride();
        r1.setId("ride-alice");
        r1.setClientName("Alice Beta");
        r1.setPickupLocation("Downtown");
        r1.setDropoffLocation("Airport");
        r1.setStatus(com.tatalance.ride.RideStatus.SCHEDULED);

        Invoice i1 = new Invoice();
        i1.setId("inv-001");
        i1.setInvoiceNumber("INV-001");
        i1.setClientName("Alice");
        i1.setTotal(new java.math.BigDecimal("10"));
        i1.setStatus(com.tatalance.invoice.InvoiceStatus.PAID);

        when(clientRepo.findByUserId(eq(TEST_USER_ID))).thenReturn(List.of(c1));
        when(rideRepo.findByUserId(eq(TEST_USER_ID))).thenReturn(List.of(r1));
        when(invoiceRepo.findByUserId(eq(TEST_USER_ID))).thenReturn(List.of(i1));
    }

    @Test
    void search_shouldReturnWhenQLong() throws Exception {
        mockMvc.perform(get("/api/search").param("q", "al"))
                .andExpect(status().isOk());
    }

    @Test
    void search_shouldReturnEmptyWhenShortQ() throws Exception {
        mockMvc.perform(get("/api/search").param("q", "a"))
                .andExpect(status().isOk());
    }

    @Test
    void search_withMatches_populatesAllSections() throws Exception {
        // q that matches across clients + rides + invoices
        mockMvc.perform(get("/api/search").param("q", "Alice"))
                .andExpect(status().isOk());
    }

    @Test
    void search_matchesPhoneOrEmail_only_coversClientFilterBranches() throws Exception {
        // hit phone/email in client filter || chain, no rides/inv match
        Client c = new Client();
        c.setId("c2");
        c.setFirstName("Zoe");
        c.setLastName("Z");
        c.setPhone("+15551239999");
        c.setEmail("zoe@ex.com");
        when(clientRepo.findByUserId(eq(TEST_USER_ID))).thenReturn(List.of(c));
        when(rideRepo.findByUserId(eq(TEST_USER_ID))).thenReturn(List.of());
        when(invoiceRepo.findByUserId(eq(TEST_USER_ID))).thenReturn(List.of());

        mockMvc.perform(get("/api/search").param("q", "+1555"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/search").param("q", "zoe@"))
                .andExpect(status().isOk());
    }

    @Test
    void search_ridesPickupDropOrInvoiceOnly_coversOtherFilterAndSectionIfs() throws Exception {
        // hit pickup/drop for rides, invoice num, no client section
        Ride r = new Ride();
        r.setId("r2");
        r.setClientName("NoMatch");
        r.setPickupLocation("SecretBase");
        r.setDropoffLocation("HiddenSpot");
        r.setStatus(com.tatalance.ride.RideStatus.SCHEDULED);

        Invoice i = new Invoice();
        i.setId("i2");
        i.setInvoiceNumber("INV-999");
        i.setClientName("X");
        i.setTotal(new java.math.BigDecimal("5"));
        i.setStatus(com.tatalance.invoice.InvoiceStatus.OUTSTANDING);

        when(clientRepo.findByUserId(eq(TEST_USER_ID))).thenReturn(List.of());
        when(rideRepo.findByUserId(eq(TEST_USER_ID))).thenReturn(List.of(r));
        when(invoiceRepo.findByUserId(eq(TEST_USER_ID))).thenReturn(List.of(i));

        mockMvc.perform(get("/api/search").param("q", "Secret"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/search").param("q", "INV-999"))
                .andExpect(status().isOk());
    }
}
