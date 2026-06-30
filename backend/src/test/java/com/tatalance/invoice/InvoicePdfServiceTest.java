package com.tatalance.invoice;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertTrue;

class InvoicePdfServiceTest {

    @Test
    void render_includesVenmoLineWhenHandleSet() throws Exception {
        Invoice invoice = sampleInvoice();
        invoice.setVenmoHandle("luchi");

        String text = extractText(InvoicePdfService.render(invoice));

        assertTrue(text.contains("Pay via Venmo: @luchi"));
    }

    @Test
    void render_preservesAtPrefixOnVenmoHandle() throws Exception {
        Invoice invoice = sampleInvoice();
        invoice.setVenmoHandle("@luchi");

        String text = extractText(InvoicePdfService.render(invoice));

        assertTrue(text.contains("Pay via Venmo: @luchi"));
    }

    @Test
    void render_showsFallbackWhenVenmoMissing() throws Exception {
        Invoice invoice = sampleInvoice();
        invoice.setVenmoHandle(null);

        String text = extractText(InvoicePdfService.render(invoice));

        assertTrue(text.contains("contact sender for payment details"));
    }

    private static Invoice sampleInvoice() {
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber("INV-001");
        invoice.setClientName("Test Client");
        invoice.setCreatedAt(Instant.parse("2026-06-29T12:00:00Z"));
        invoice.setBaseCharge(new BigDecimal("200.00"));
        invoice.setTax(new BigDecimal("16.00"));
        invoice.setTotal(new BigDecimal("216.00"));
        invoice.setStatus(InvoiceStatus.OUTSTANDING);
        invoice.setPricingMode("FLAT");
        return invoice;
    }

    private static String extractText(byte[] pdf) throws Exception {
        PdfReader reader = new PdfReader(pdf);
        try {
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            StringBuilder sb = new StringBuilder();
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                sb.append(extractor.getTextFromPage(page));
            }
            return sb.toString();
        } finally {
            reader.close();
        }
    }
}