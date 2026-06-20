package com.tatalance.stats;

import com.tatalance.client.ClientRepository;
import com.tatalance.driver.DriverRepository;
import com.tatalance.invoice.InvoiceRepository;
import com.tatalance.invoice.InvoiceStatus;
import com.tatalance.ride.RideRepository;
import com.tatalance.ride.RideStatus;
import com.tatalance.user.AuthHelper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.*;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final ClientRepository clientRepo;
    private final DriverRepository driverRepo;
    private final RideRepository rideRepo;
    private final InvoiceRepository invoiceRepo;
    private final AuthHelper authHelper;

    public StatsController(ClientRepository clientRepo, DriverRepository driverRepo,
                           RideRepository rideRepo, InvoiceRepository invoiceRepo,
                           AuthHelper authHelper) {
        this.clientRepo = clientRepo;
        this.driverRepo = driverRepo;
        this.rideRepo = rideRepo;
        this.invoiceRepo = invoiceRepo;
        this.authHelper = authHelper;
    }

    @GetMapping
    public Map<String, Object> stats() {
        String userId = authHelper.getCurrentUserId();
        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);

        Instant dayStart = today.atStartOfDay(zone).toInstant();
        Instant dayEnd = today.plusDays(1).atStartOfDay(zone).toInstant();

        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        Instant weekInstant = weekStart.atStartOfDay(zone).toInstant();

        LocalDate monthStart = today.withDayOfMonth(1);
        Instant monthInstant = monthStart.atStartOfDay(zone).toInstant();

        long totalClients = clientRepo.countByUserId(userId);
        long totalDrivers = driverRepo.countByUserId(userId);
        long totalRides = rideRepo.countByUserId(userId);

        long ridesToday = rideRepo.countByUserIdAndPickupDateTimeBetween(userId, dayStart, dayEnd);
        long ridesThisWeek = rideRepo.countByUserIdAndPickupDateTimeBetween(userId, weekInstant, dayEnd);
        long ridesThisMonth = rideRepo.countByUserIdAndPickupDateTimeBetween(userId, monthInstant, dayEnd);

        // Rides by status
        Map<String, Long> ridesByStatus = new LinkedHashMap<>();
        for (RideStatus s : RideStatus.values()) {
            long count = rideRepo.countByUserIdAndStatus(userId, s);
            ridesByStatus.put(s.name(), count);
        }

        // Revenue (paid invoices) and outstanding
        var paidInvoices = invoiceRepo.findByUserIdAndStatus(userId, InvoiceStatus.PAID);
        BigDecimal revenueTotal = paidInvoices.stream()
                .map(i -> i.getTotal() != null ? i.getTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Revenue this month only
        BigDecimal revenueThisMonth = paidInvoices.stream()
                .filter(i -> i.getCreatedAt() != null && !i.getCreatedAt().isBefore(monthInstant))
                .map(i -> i.getTotal() != null ? i.getTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var outstandingInvoices = invoiceRepo.findByUserIdAndStatus(userId, InvoiceStatus.OUTSTANDING);
        BigDecimal outstandingAmount = outstandingInvoices.stream()
                .map(i -> i.getTotal() != null ? i.getTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalClients", totalClients);
        result.put("totalDrivers", totalDrivers);
        result.put("totalRides", totalRides);
        result.put("ridesToday", ridesToday);
        result.put("ridesThisWeek", ridesThisWeek);
        result.put("ridesThisMonth", ridesThisMonth);
        result.put("ridesByStatus", ridesByStatus);
        result.put("revenueTotal", revenueTotal);
        result.put("revenueThisMonth", revenueThisMonth);
        result.put("outstandingAmount", outstandingAmount);
        result.put("outstandingCount", outstandingInvoices.size());
        return result;
    }
}
