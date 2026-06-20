package com.tatalance.invoice;

import com.tatalance.activity.ActivityLogger;
import com.tatalance.ride.Ride;
import com.tatalance.ride.RideRepository;
import com.tatalance.ride.RideStatus;
import com.tatalance.user.AuthHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.Year;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Tag(name = "Invoices", description = "Invoice generation and management")
@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private static final BigDecimal TAX_RATE = new BigDecimal("0.08");

    private final InvoiceRepository invoiceRepository;
    private final RideRepository rideRepository;
    private final AuthHelper authHelper;
    private final ActivityLogger activityLog;

    public InvoiceController(InvoiceRepository invoiceRepository, RideRepository rideRepository,
                             AuthHelper authHelper, ActivityLogger activityLog) {
        this.invoiceRepository = invoiceRepository;
        this.rideRepository = rideRepository;
        this.authHelper = authHelper;
        this.activityLog = activityLog;
    }

    @Operation(summary = "Generate invoice from a completed ride")
    @ApiResponse(responseCode = "201", description = "Invoice created")
    @ApiResponse(responseCode = "400", description = "Ride not completed or not found")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Invoice create(@RequestBody Map<String, String> body) {
        String userId = authHelper.getCurrentUserId();
        String rideId = body.get("rideId");
        if (rideId == null || rideId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "rideId is required");
        }

        Ride ride = rideRepository.findByIdAndUserId(rideId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ride not found"));

        if (ride.getStatus() != RideStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ride must be COMPLETED to generate invoice");
        }

        BigDecimal baseCharge = ride.getTotalAmount() != null ? ride.getTotalAmount()
                : (ride.getBasePrice() != null ? ride.getBasePrice() : BigDecimal.ZERO);
        BigDecimal extras = BigDecimal.ZERO;
        if (ride.getTolls() != null) extras = extras.add(ride.getTolls());
        if (ride.getParking() != null) extras = extras.add(ride.getParking());
        if (ride.getAdditionalCharges() != null) extras = extras.add(ride.getAdditionalCharges());

        BigDecimal subtotal = baseCharge.add(extras);
        BigDecimal tax = subtotal.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(tax);

        Invoice invoice = new Invoice();
        invoice.setUserId(userId);
        invoice.setInvoiceNumber(generateInvoiceNumber(userId));
        invoice.setClientId(ride.getClientId());
        invoice.setClientName(ride.getClientName());
        invoice.setRideId(rideId);
        invoice.setBaseCharge(baseCharge);
        invoice.setAdditionalCharges(extras);
        invoice.setTax(tax);
        invoice.setTotal(total);
        invoice.setPricingMode(ride.getPricingMode() != null ? ride.getPricingMode().name() : null);
        invoice.setHourlyRate(ride.getHourlyRate());
        invoice.setDurationMinutes(ride.getDurationMinutes());
        invoice.setStatus(InvoiceStatus.OUTSTANDING);
        invoice.setCreatedAt(Instant.now());

        Invoice saved = invoiceRepository.save(invoice);
        activityLog.log(userId, "CREATE", "Invoice", saved.getId(),
                "Created invoice " + saved.getInvoiceNumber() + " for " + saved.getClientName() + " — $" + saved.getTotal());
        return saved;
    }

    @Operation(summary = "List all invoices")
    @ApiResponse(responseCode = "200", description = "Invoice list")
    @GetMapping
    public Page<Invoice> list(@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return invoiceRepository.findByUserId(authHelper.getCurrentUserId(), pageable);
    }

    @Operation(summary = "Get invoice by id")
    @ApiResponse(responseCode = "200", description = "Invoice found")
    @ApiResponse(responseCode = "404", description = "Invoice not found")
    @GetMapping("/{id}")
    public Invoice getById(@PathVariable String id) {
        return invoiceRepository.findByIdAndUserId(id, authHelper.getCurrentUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));
    }

    @Operation(summary = "Toggle invoice paid/outstanding status")
    @ApiResponse(responseCode = "200", description = "Status toggled")
    @ApiResponse(responseCode = "404", description = "Invoice not found")
    @PostMapping("/{id}/mark-paid")
    public Invoice markPaid(@PathVariable String id) {
        Invoice invoice = invoiceRepository.findByIdAndUserId(id, authHelper.getCurrentUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));

        if (invoice.getStatus() == InvoiceStatus.OUTSTANDING) {
            invoice.setStatus(InvoiceStatus.PAID);
        } else {
            invoice.setStatus(InvoiceStatus.OUTSTANDING);
        }

        Invoice saved = invoiceRepository.save(invoice);
        activityLog.log(authHelper.getCurrentUserId(), "UPDATE", "Invoice", id,
                "Marked " + saved.getInvoiceNumber() + " as " + saved.getStatus());
        return saved;
    }

    @Operation(summary = "Export all invoices as CSV")
    @GetMapping("/export/csv")
    public void exportCsv(HttpServletResponse response) throws IOException {
        String userId = authHelper.getCurrentUserId();
        List<Invoice> invoices = invoiceRepository.findByUserId(userId);

        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=invoices.csv");

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                .withZone(ZoneId.systemDefault());

        PrintWriter w = response.getWriter();
        w.println("Invoice #,Client,Status,Base Charge,Additional,Tax,Total,Pricing Mode,Date");
        for (Invoice inv : invoices) {
            w.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                    csvSafe(inv.getInvoiceNumber()),
                    csvSafe(inv.getClientName()),
                    inv.getStatus(),
                    inv.getBaseCharge(),
                    inv.getAdditionalCharges(),
                    inv.getTax(),
                    inv.getTotal(),
                    inv.getPricingMode() != null ? inv.getPricingMode() : "FLAT",
                    inv.getCreatedAt() != null ? fmt.format(inv.getCreatedAt()) : "");
        }
        w.flush();
    }

    private static String csvSafe(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String generateInvoiceNumber(String userId) {
        long seq = invoiceRepository.countByUserId(userId) + 1;
        return String.format("INV-%d-%03d", Year.now().getValue(), seq);
    }
}
