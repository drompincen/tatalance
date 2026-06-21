package com.tatalance.demo;

import com.tatalance.client.Client;
import com.tatalance.client.ClientRepository;
import com.tatalance.customtable.*;
import com.tatalance.driver.*;
import com.tatalance.invoice.*;
import com.tatalance.profile.Profile;
import com.tatalance.profile.ProfileRepository;
import com.tatalance.profile.ProfileType;
import com.tatalance.ride.*;
import com.tatalance.user.AppUser;
import com.tatalance.user.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Component
@Order(3)
public class DemoDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final AppUserRepository userRepo;
    private final ClientRepository clientRepo;
    private final DriverRepository driverRepo;
    private final RideRepository rideRepo;
    private final InvoiceRepository invoiceRepo;
    private final CustomTableRepository tableRepo;
    private final CustomTableRowRepository rowRepo;
    private final ProfileRepository profileRepo;

    public DemoDataSeeder(AppUserRepository userRepo,
                          ClientRepository clientRepo,
                          DriverRepository driverRepo,
                          RideRepository rideRepo,
                          InvoiceRepository invoiceRepo,
                          CustomTableRepository tableRepo,
                          CustomTableRowRepository rowRepo,
                          ProfileRepository profileRepo) {
        this.userRepo = userRepo;
        this.clientRepo = clientRepo;
        this.driverRepo = driverRepo;
        this.rideRepo = rideRepo;
        this.invoiceRepo = invoiceRepo;
        this.tableRepo = tableRepo;
        this.rowRepo = rowRepo;
        this.profileRepo = profileRepo;
    }

    @Override
    public void run(String... args) {
        try {
            if (clientRepo.count() > 0 && profileRepo.count() > 0) {
                log.info("Demo data + profiles already present - skipping");
                return;
            }

            Optional<AppUser> adminOpt = userRepo.findByUsername("admin");
            if (adminOpt.isEmpty()) {
                log.warn("No admin user found for demo seeding");
                return;
            }
            String uid = adminOpt.get().getId();
            Instant now = Instant.now();

            // --- Clients ---
            Client c1 = new Client();
            c1.setUserId(uid);
            c1.setFirstName("Alice");
            c1.setLastName("Johnson");
            c1.setPhone("+15551234567");
            c1.setEmail("alice.j@example.com");
            c1.setNotes("Prefers airport pickups");
            c1.setCreatedAt(now.minus(10, ChronoUnit.DAYS));
            c1 = clientRepo.save(c1);

            Client c2 = new Client();
            c2.setUserId(uid);
            c2.setFirstName("Bob");
            c2.setLastName("Smith");
            c2.setPhone("+15559876543");
            c2.setEmail("bob.smith@example.com");
            c2.setCreatedAt(now.minus(7, ChronoUnit.DAYS));
            c2 = clientRepo.save(c2);

            Client c3 = new Client();
            c3.setUserId(uid);
            c3.setFirstName("Carol");
            c3.setLastName("Lee");
            c3.setPhone("+15553332211");
            c3.setNotes("VIP - early pickup");
            c3.setCreatedAt(now.minus(3, ChronoUnit.DAYS));
            c3 = clientRepo.save(c3);

            // --- Drivers ---
            Driver d1 = new Driver();
            d1.setUserId(uid);
            d1.setFirstName("Mike");
            d1.setLastName("Rivers");
            d1.setPhone("+15554443322");
            d1.setVehicle("Toyota Camry - ABC123");
            d1.setPayoutType(PayoutType.PERCENTAGE);
            d1.setPayoutRate(new BigDecimal("70"));
            d1.setAvailability(Availability.AVAILABLE);
            d1.setCreatedAt(now.minus(9, ChronoUnit.DAYS));
            d1 = driverRepo.save(d1);

            Driver d2 = new Driver();
            d2.setUserId(uid);
            d2.setFirstName("Sara");
            d2.setLastName("Kim");
            d2.setPhone("+15556667788");
            d2.setVehicle("Honda Accord - XYZ789");
            d2.setPayoutType(PayoutType.FLAT);
            d2.setPayoutRate(new BigDecimal("45"));
            d2.setAvailability(Availability.ON_TRIP);
            d2.setCreatedAt(now.minus(5, ChronoUnit.DAYS));
            d2 = driverRepo.save(d2);

            // --- Profiles for multi-profile business owner demo (clients shared, jobs/rides scoped) ---
            Profile pDriver = new Profile();
            pDriver.setUserId(uid);
            pDriver.setType(ProfileType.DRIVER);
            pDriver.setName("Main Taxi / Driver");
            pDriver.setCreatedAt(now.minus(20, ChronoUnit.DAYS));
            pDriver = profileRepo.save(pDriver);

            Profile pFreelance = new Profile();
            pFreelance.setUserId(uid);
            pFreelance.setType(ProfileType.ENGINEER);
            pFreelance.setName("Freelance Engineer");
            pFreelance.setCreatedAt(now.minus(15, ChronoUnit.DAYS));
            pFreelance = profileRepo.save(pFreelance);

            String pidDriver = pDriver.getId();
            String pidFreelance = pFreelance.getId();

            // --- Rides (stored as jobs with type RIDE) ---
            // 1. Scheduled (today-ish)
            Ride r1 = new Ride();
            r1.setUserId(uid);
            r1.setProfileId(pidDriver);
            r1.setClientId(c1.getId());
            r1.setClientName(c1.getFirstName() + " " + c1.getLastName());
            r1.setPickupLocation("123 Main St, City");
            r1.setDropoffLocation("Airport Terminal 2");
            r1.setScheduledTime(now.plus(4, ChronoUnit.HOURS));
            r1.setPickupDateTime(r1.getScheduledTime());
            r1.setBasePrice(new BigDecimal("65.00"));
            r1.setStatus(RideStatus.SCHEDULED);
            r1.setNotes("2 passengers, luggage");
            r1.setCreatedAt(now.minus(1, ChronoUnit.DAYS));
            r1 = rideRepo.save(r1);

            // 2. Assigned
            Ride r2 = new Ride();
            r2.setUserId(uid);
            r2.setProfileId(pidDriver);
            r2.setClientId(c2.getId());
            r2.setClientName(c2.getFirstName() + " " + c2.getLastName());
            r2.setPickupLocation("456 Oak Ave");
            r2.setDropoffLocation("Downtown Convention Center");
            r2.setScheduledTime(now.plus(1, ChronoUnit.HOURS));
            r2.setPickupDateTime(r2.getScheduledTime());
            r2.setBasePrice(new BigDecimal("42.50"));
            r2.setAssignedDriverId(d1.getId());
            r2.setAssignedDriverName(d1.getFirstName() + " " + d1.getLastName());
            r2.setStatus(RideStatus.ASSIGNED);
            r2.setCreatedAt(now.minus(2, ChronoUnit.HOURS));
            r2 = rideRepo.save(r2);

            // 3. In progress
            Ride r3 = new Ride();
            r3.setUserId(uid);
            r3.setClientId(c3.getId());
            r3.setClientName(c3.getFirstName() + " " + c3.getLastName());
            r3.setPickupLocation("789 Pine Rd, Suburb");
            r3.setDropoffLocation("Harbor View Hotel");
            r3.setScheduledTime(now.minus(30, ChronoUnit.MINUTES));
            r3.setPickupDateTime(r3.getScheduledTime());
            r3.setBasePrice(new BigDecimal("55.00"));
            r3.setAssignedDriverId(d2.getId());
            r3.setAssignedDriverName(d2.getFirstName() + " " + d2.getLastName());
            r3.setStatus(RideStatus.IN_PROGRESS);
            r3.setActualStart(now.minus(20, ChronoUnit.MINUTES));
            r3.setNotes("Hotel to event");
            r3.setCreatedAt(now.minus(1, ChronoUnit.HOURS));
            r3 = rideRepo.save(r3);

            // 4. Completed (revenue + payout)
            Ride r4 = new Ride();
            r4.setUserId(uid);
            r4.setProfileId(pidFreelance);
            r4.setClientId(c1.getId());
            r4.setClientName(c1.getFirstName() + " " + c1.getLastName());
            r4.setPickupLocation("Downtown Plaza");
            r4.setDropoffLocation("Suburban Mall");
            r4.setScheduledTime(now.minus(2, ChronoUnit.DAYS));
            r4.setPickupDateTime(r4.getScheduledTime());
            r4.setBasePrice(new BigDecimal("38.00"));
            r4.setAdditionalCharges(new BigDecimal("12.50"));
            r4.setTolls(new BigDecimal("8.00"));
            r4.setTotalAmount(new BigDecimal("58.50"));
            r4.setBillableAmount(new BigDecimal("58.50"));
            r4.setAssignedDriverId(d1.getId());
            r4.setAssignedDriverName(d1.getFirstName() + " " + d1.getLastName());
            r4.setDriverPayout(new BigDecimal("40.95")); // ~70%
            r4.setPayoutPaid(false);
            r4.setStatus(RideStatus.COMPLETED);
            r4.setActualStart(now.minus(2, ChronoUnit.DAYS).plusSeconds(3600));
            r4.setActualEnd(now.minus(2, ChronoUnit.DAYS).plusSeconds(7200));
            r4.setDurationMinutes(60L);
            r4.setCreatedAt(now.minus(3, ChronoUnit.DAYS));
            r4 = rideRepo.save(r4);

            // 5. Completed + paid out (for revenue)
            Ride r5 = new Ride();
            r5.setUserId(uid);
            r5.setClientId(c2.getId());
            r5.setClientName(c2.getFirstName() + " " + c2.getLastName());
            r5.setPickupLocation("Train Station");
            r5.setDropoffLocation("Riverside Park");
            r5.setScheduledTime(now.minus(5, ChronoUnit.DAYS));
            r5.setPickupDateTime(r5.getScheduledTime());
            r5.setBasePrice(new BigDecimal("72.00"));
            r5.setAdditionalCharges(new BigDecimal("5.00"));
            r5.setTolls(new BigDecimal("4.50"));
            r5.setTotalAmount(new BigDecimal("81.50"));
            r5.setBillableAmount(new BigDecimal("81.50"));
            r5.setAssignedDriverId(d2.getId());
            r5.setAssignedDriverName(d2.getFirstName() + " " + d2.getLastName());
            r5.setDriverPayout(new BigDecimal("45.00"));
            r5.setPayoutPaid(true);
            r5.setStatus(RideStatus.COMPLETED);
            r5.setActualStart(now.minus(5, ChronoUnit.DAYS).plusSeconds(1800));
            r5.setActualEnd(now.minus(5, ChronoUnit.DAYS).plusSeconds(5400));
            r5.setDurationMinutes(60L);
            r5.setCreatedAt(now.minus(6, ChronoUnit.DAYS));
            r5 = rideRepo.save(r5);

            // --- Invoices (to populate revenue/outstanding in dashboard) ---
            Invoice inv1 = new Invoice();
            inv1.setUserId(uid);
            inv1.setInvoiceNumber("INV-1001");
            inv1.setClientId(c1.getId());
            inv1.setClientName(c1.getFirstName() + " " + c1.getLastName());
            inv1.setRideId(r4.getId());
            inv1.setBaseCharge(new BigDecimal("38.00"));
            inv1.setAdditionalCharges(new BigDecimal("20.50"));
            inv1.setTax(new BigDecimal("5.80"));
            inv1.setTotal(new BigDecimal("64.30"));
            inv1.setStatus(InvoiceStatus.PAID);
            inv1.setCreatedAt(r4.getActualEnd() != null ? r4.getActualEnd() : now.minus(2, ChronoUnit.DAYS));
            invoiceRepo.save(inv1);

            Invoice inv2 = new Invoice();
            inv2.setUserId(uid);
            inv2.setInvoiceNumber("INV-1002");
            inv2.setClientId(c2.getId());
            inv2.setClientName(c2.getFirstName() + " " + c2.getLastName());
            inv2.setRideId(r5.getId());
            inv2.setBaseCharge(new BigDecimal("72.00"));
            inv2.setAdditionalCharges(new BigDecimal("9.50"));
            inv2.setTax(new BigDecimal("8.15"));
            inv2.setTotal(new BigDecimal("89.65"));
            inv2.setStatus(InvoiceStatus.OUTSTANDING);
            inv2.setCreatedAt(r5.getActualEnd() != null ? r5.getActualEnd() : now.minus(5, ChronoUnit.DAYS));
            invoiceRepo.save(inv2);

            // --- Custom table demo ---
            CustomTable exp = new CustomTable();
            exp.setUserId(uid);
            exp.setName("Expenses");
            exp.setCreatedAt(now.minus(4, ChronoUnit.DAYS));

            ColumnDef col1 = new ColumnDef();
            col1.setName("Date");
            col1.setType(ColumnType.DATE);

            ColumnDef col2 = new ColumnDef();
            col2.setName("Category");
            col2.setType(ColumnType.STRING);

            ColumnDef col3 = new ColumnDef();
            col3.setName("Amount");
            col3.setType(ColumnType.INT);

            ColumnDef col4 = new ColumnDef();
            col4.setName("Notes");
            col4.setType(ColumnType.STRING);

            exp.setColumns(Arrays.asList(col1, col2, col3, col4));
            exp = tableRepo.save(exp);

            // Rows for the table
            Map<String, Object> rowData1 = new LinkedHashMap<>();
            rowData1.put("Date", "2026-06-18");
            rowData1.put("Category", "Fuel");
            rowData1.put("Amount", 42);
            rowData1.put("Notes", "Fill up after airport run");

            CustomTableRow row1 = new CustomTableRow();
            row1.setUserId(uid);
            row1.setTableId(exp.getId());
            row1.setData(rowData1);
            row1.setCreatedAt(now.minus(3, ChronoUnit.DAYS));
            rowRepo.save(row1);

            Map<String, Object> rowData2 = new LinkedHashMap<>();
            rowData2.put("Date", "2026-06-19");
            rowData2.put("Category", "Tolls");
            rowData2.put("Amount", 18);
            rowData2.put("Notes", "Bridge");

            CustomTableRow row2 = new CustomTableRow();
            row2.setUserId(uid);
            row2.setTableId(exp.getId());
            row2.setData(rowData2);
            row2.setCreatedAt(now.minus(2, ChronoUnit.DAYS));
            rowRepo.save(row2);

            log.info("Seeded demo data (clients, drivers, rides, invoices, 1 custom table) for local/mobile testing");

        } catch (Exception e) {
            log.warn("DemoDataSeeder skipped or partial: {}", e.getMessage());
        }
    }
}
