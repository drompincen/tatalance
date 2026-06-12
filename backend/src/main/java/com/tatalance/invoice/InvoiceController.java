package com.tatalance.invoice;

import com.tatalance.ride.Ride;
import com.tatalance.ride.RideRepository;
import com.tatalance.ride.RideStatus;
import com.tatalance.user.AuthHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.Year;
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

    public InvoiceController(InvoiceRepository invoiceRepository, RideRepository rideRepository,
                             AuthHelper authHelper) {
        this.invoiceRepository = invoiceRepository;
        this.rideRepository = rideRepository;
        this.authHelper = authHelper;
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

        BigDecimal baseCharge = ride.getBasePrice() != null ? ride.getBasePrice() : BigDecimal.ZERO;
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
        invoice.setStatus(InvoiceStatus.OUTSTANDING);
        invoice.setCreatedAt(Instant.now());

        return invoiceRepository.save(invoice);
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

        return invoiceRepository.save(invoice);
    }

    private String generateInvoiceNumber(String userId) {
        long seq = invoiceRepository.countByUserId(userId) + 1;
        return String.format("INV-%d-%03d", Year.now().getValue(), seq);
    }
}
