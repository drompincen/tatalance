package com.tatalance.search;

import com.tatalance.client.ClientRepository;
import com.tatalance.invoice.InvoiceRepository;
import com.tatalance.ride.JobRepository;
import com.tatalance.ride.RideRepository;
import com.tatalance.user.AuthHelper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final ClientRepository clientRepo;
    private final RideRepository rideRepo;
    private final InvoiceRepository invoiceRepo;
    private final AuthHelper authHelper;

    public SearchController(ClientRepository clientRepo, RideRepository rideRepo,
                            InvoiceRepository invoiceRepo, AuthHelper authHelper) {
        this.clientRepo = clientRepo;
        this.rideRepo = rideRepo;
        this.invoiceRepo = invoiceRepo;
        this.authHelper = authHelper;
    }

    // #93: Search on rides continues to use ride-specific fields (locations); jobs use scheduled/client only.

    @GetMapping
    public Map<String, Object> search(@RequestParam String q) {
        String userId = authHelper.getCurrentUserId();
        String term = q.trim().toLowerCase();
        Map<String, Object> results = new LinkedHashMap<>();

        if (term.length() < 2) {
            return results;
        }

        var clients = clientRepo.findByUserId(userId).stream()
                .filter(c -> matches(c.getFirstName(), term) || matches(c.getLastName(), term)
                        || matches(c.getPhone(), term) || matches(c.getEmail(), term))
                .limit(5)
                .map(c -> Map.of("id", c.getId(),
                        "text", c.getFirstName() + " " + c.getLastName(),
                        "detail", c.getPhone() != null ? c.getPhone() : ""))
                .toList();

        var rides = rideRepo.findByUserId(userId).stream()
                .filter(r -> matches(r.getClientName(), term) || matches(r.getPickupLocation(), term)
                        || matches(r.getDropoffLocation(), term))
                .limit(5)
                .map(r -> Map.of("id", r.getId(),
                        "text", r.getClientName() + " — " + r.getStatus(),
                        "detail", (r.getPickupLocation() != null ? r.getPickupLocation() : "") + " → "
                                + (r.getDropoffLocation() != null ? r.getDropoffLocation() : "")))
                .toList();

        var invoices = invoiceRepo.findByUserId(userId).stream()
                .filter(i -> matches(i.getInvoiceNumber(), term) || matches(i.getClientName(), term))
                .limit(5)
                .map(i -> Map.of("id", i.getId(),
                        "text", i.getInvoiceNumber() + " — " + i.getClientName(),
                        "detail", "$" + i.getTotal() + " " + i.getStatus()))
                .toList();

        if (!clients.isEmpty()) results.put("clients", clients);
        if (!rides.isEmpty()) results.put("rides", rides);
        if (!invoices.isEmpty()) results.put("invoices", invoices);
        return results;
    }

    private static boolean matches(String value, String term) {
        return value != null && value.toLowerCase().contains(term);
    }
}
