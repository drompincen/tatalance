package com.tatalance.invoice;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;

public class Payment {
    @NotNull
    private Instant date;
    @NotNull
    @Positive
    private BigDecimal amount;
    @NotNull
    private PaymentMethod method;
    private String reference;

    public Instant getDate() { return date; }
    public void setDate(Instant date) { this.date = date; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public PaymentMethod getMethod() { return method; }
    public void setMethod(PaymentMethod method) { this.method = method; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
}
