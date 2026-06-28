package com.tatalance.invoice;

import com.tatalance.profile.Profile;
import com.tatalance.profile.ProfileType;
import com.tatalance.ride.Job;
import com.tatalance.ride.PricingMode;
import com.tatalance.user.AppUser;

import java.math.BigDecimal;

/**
 * Resolves the tax rate (decimal fraction, e.g. 0.08 = 8%) for invoice generation.
 * Priority: profile override → user setting → legacy defaults for unset accounts.
 */
public final class TaxRateResolver {

    static final BigDecimal LEGACY_CHAUFFEUR_RATE = new BigDecimal("0.08");

    private TaxRateResolver() {}

    public static BigDecimal resolve(Job job, AppUser user, Profile profile) {
        if (profile != null && profile.getTaxRate() != null) {
            return clamp(profile.getTaxRate());
        }
        if (user != null && user.getDefaultTaxRate() != null) {
            return clamp(user.getDefaultTaxRate());
        }
        if (job.getPricingMode() == PricingMode.HOURLY) {
            return BigDecimal.ZERO;
        }
        if (profile != null && profile.getType() == ProfileType.ENGINEER) {
            return BigDecimal.ZERO;
        }
        return LEGACY_CHAUFFEUR_RATE;
    }

    public static BigDecimal fromPercent(Number percent) {
        if (percent == null) {
            return null;
        }
        double p = percent.doubleValue();
        if (p < 0 || p > 100) {
            throw new IllegalArgumentException("Tax rate percent must be between 0 and 100");
        }
        return BigDecimal.valueOf(p).movePointLeft(2);
    }

    public static BigDecimal clamp(BigDecimal rate) {
        if (rate.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        if (rate.compareTo(BigDecimal.ONE) > 0) {
            return BigDecimal.ONE;
        }
        return rate;
    }
}