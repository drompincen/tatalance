package com.tatalance.invoice;

import com.tatalance.activity.ActivityLogger;
import com.tatalance.profile.Profile;
import com.tatalance.profile.ProfileRepository;
import com.tatalance.profile.ProfileType;
import com.tatalance.ride.Job;
import com.tatalance.ride.PricingMode;
import com.tatalance.ride.Ride;
import com.tatalance.ride.RideRepository;
import com.tatalance.ride.RideStatus;
import com.tatalance.user.AppUserRepository;
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
    private final ProfileRepository profileRepository;
    private final AppUserRepository appUserRepository;
    private final AuthHelper authHelper;
    private final ActivityLogger activityLog;

    public InvoiceController(InvoiceRepository invoiceRepository, RideRepository rideRepository,
                             ProfileRepository profileRepository, AppUserRepository appUserRepository,
                             AuthHelper authHelper, ActivityLogger activityLog) {
        this.invoiceRepository = invoiceRepository;
        this.rideRepository = rideRepository;
        this.profileRepository = profileRepository;
        this.appUserRepository = appUserRepository;
        this.authHelper = authHelper;
        this.activityLog = activityLog;
    }

    @Operation(summary = "Generate invoice from a completed ride/job (supports unified Job model post #93 refactor)")
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

        Job job = ride;
        BigDecimal baseCharge = job.getTotalAmount() != null ? job.getTotalAmount()
                : (job.getBasePrice() != null ? job.getBasePrice() : BigDecimal.ZERO);
        BigDecimal extras = BigDecimal.ZERO;
        if (job.getTolls() != null) extras = extras.add(job.getTolls());
        if (job.getParking() != null) extras = extras.add(job.getParking());
        if (job.getAdditionalCharges() != null) extras = extras.add(job.getAdditionalCharges());

        BigDecimal subtotal = baseCharge.add(extras);
        boolean noTax = isFreelanceInvoice(job);
        BigDecimal tax = noTax ? BigDecimal.ZERO
                : subtotal.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(tax);

        String venmoHandle = appUserRepository.findById(userId)
                .map(u -> u.getVenmoHandle())
                .orElse(null);

        Invoice invoice = new Invoice();
        invoice.setUserId(userId);
        invoice.setInvoiceNumber(generateInvoiceNumber(userId));
        invoice.setClientId(job.getClientId());
        invoice.setClientName(job.getClientName());
        invoice.setRideId(rideId);
        invoice.setBaseCharge(baseCharge);
        invoice.setAdditionalCharges(extras);
        invoice.setTax(tax);
        invoice.setTotal(total);
        invoice.setPricingMode(job.getPricingMode() != null ? job.getPricingMode().name() : null);
        invoice.setHourlyRate(job.getHourlyRate());
        invoice.setDurationMinutes(job.getDurationMinutes());
        invoice.setVenmoHandle(venmoHandle);
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

    @Operation(summary = "Download invoice as PDF")
    @ApiResponse(responseCode = "200", description = "PDF bytes")
    @GetMapping("/{id}/pdf")
    public void downloadPdf(@PathVariable String id, HttpServletResponse response) throws IOException {
        Invoice invoice = invoiceRepository.findByIdAndUserId(id, authHelper.getCurrentUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));
        byte[] pdf = InvoicePdfService.render(invoice);
        String filename = (invoice.getInvoiceNumber() != null ? invoice.getInvoiceNumber() : "invoice") + ".pdf";
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        response.setContentLength(pdf.length);
        response.getOutputStream().write(pdf);
        response.getOutputStream().flush();
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

    private boolean isFreelanceInvoice(Job job) {
        if (job.getPricingMode() == PricingMode.HOURLY) {
            return true;
        }
        if (job.getProfileId() == null || job.getProfileId().isBlank()) {
            return false;
        }
        return profileRepository.findById(job.getProfileId())
                .map(Profile::getType)
                .filter(t -> t == ProfileType.ENGINEER)
                .isPresent();
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