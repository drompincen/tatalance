package com.tatalance.invoice;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Renders a simple invoice PDF for freelance mobile share/download (#115 G2).
 */
public final class InvoicePdfService {

    private static final Font TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
    private static final Font BODY = FontFactory.getFont(FontFactory.HELVETICA, 11);
    private static final Font MUTED = FontFactory.getFont(FontFactory.HELVETICA, 10);

    private InvoicePdfService() {}

    public static byte[] render(Invoice invoice) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document();
            PdfWriter.getInstance(doc, out);
            doc.open();

            doc.add(new Paragraph("Tatalance Invoice", TITLE));
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Invoice #: " + nullSafe(invoice.getInvoiceNumber()), BODY));
            doc.add(new Paragraph("Client: " + nullSafe(invoice.getClientName()), BODY));
            if (invoice.getCreatedAt() != null) {
                String when = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                        .withZone(ZoneId.systemDefault())
                        .format(invoice.getCreatedAt());
                doc.add(new Paragraph("Date: " + when, BODY));
            }
            doc.add(new Paragraph(" "));
            doc.add(lineItem(invoice));
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Subtotal: $" + money(invoice.getBaseCharge()), BODY));
            BigDecimal extras = invoice.getAdditionalCharges() != null ? invoice.getAdditionalCharges() : BigDecimal.ZERO;
            if (extras.compareTo(BigDecimal.ZERO) > 0) {
                doc.add(new Paragraph("Additional charges: $" + money(extras), BODY));
            }
            doc.add(new Paragraph("Tax: $" + money(invoice.getTax()), BODY));
            doc.add(new Paragraph("Total: $" + money(invoice.getTotal()), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
            doc.add(new Paragraph("Status: " + (invoice.getStatus() != null ? invoice.getStatus() : ""), BODY));
            doc.add(new Paragraph(" "));
            if (invoice.getVenmoHandle() != null && !invoice.getVenmoHandle().isBlank()) {
                doc.add(new Paragraph("Pay via Venmo: " + formatVenmo(invoice.getVenmoHandle()), BODY));
            } else {
                doc.add(new Paragraph("Payment: contact sender for payment details.", MUTED));
            }

            doc.close();
            return out.toByteArray();
        } catch (DocumentException | IOException e) {
            throw new IllegalStateException("Failed to render invoice PDF", e);
        }
    }

    private static Paragraph lineItem(Invoice invoice) {
        if ("HOURLY".equalsIgnoreCase(invoice.getPricingMode())
                && invoice.getHourlyRate() != null
                && invoice.getDurationMinutes() != null
                && invoice.getDurationMinutes() > 0) {
            BigDecimal hours = BigDecimal.valueOf(invoice.getDurationMinutes())
                    .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
            return new Paragraph(
                    hours.stripTrailingZeros().toPlainString() + "h × $"
                            + money(invoice.getHourlyRate()) + "/hr = $" + money(invoice.getBaseCharge()),
                    BODY);
        }
        return new Paragraph("Service charge: $" + money(invoice.getBaseCharge()), BODY);
    }

    private static String formatVenmo(String handle) {
        String h = handle.trim();
        return h.startsWith("@") ? h : "@" + h;
    }

    private static String money(BigDecimal amount) {
        if (amount == null) return "0.00";
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String nullSafe(String value) {
        return value != null ? value : "";
    }
}