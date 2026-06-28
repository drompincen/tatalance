package com.tatalance.invoice;

import com.tatalance.profile.Profile;
import com.tatalance.profile.ProfileType;
import com.tatalance.ride.Job;
import com.tatalance.ride.PricingMode;
import com.tatalance.ride.Ride;
import com.tatalance.user.AppUser;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaxRateResolverTest {

    @Test
    void usesProfileOverrideWhenSet() {
        Profile profile = engineerProfile();
        profile.setTaxRate(new BigDecimal("0.05"));
        assertThat(TaxRateResolver.resolve(flatJob(), null, profile))
                .isEqualByComparingTo("0.05");
    }

    @Test
    void usesUserDefaultWhenProfileHasNoOverride() {
        AppUser user = new AppUser();
        user.setDefaultTaxRate(new BigDecimal("0.10"));
        assertThat(TaxRateResolver.resolve(flatJob(), user, null))
                .isEqualByComparingTo("0.10");
    }

    @Test
    void legacyHourlyWithoutSettings_isZero() {
        assertThat(TaxRateResolver.resolve(hourlyJob(), null, null))
                .isEqualByComparingTo("0");
    }

    @Test
    void legacyFlatWithoutSettings_isEightPercent() {
        assertThat(TaxRateResolver.resolve(flatJob(), null, null))
                .isEqualByComparingTo("0.08");
    }

    @Test
    void legacyEngineerProfileWithoutSettings_isZero() {
        assertThat(TaxRateResolver.resolve(flatJob(), null, engineerProfile()))
                .isEqualByComparingTo("0");
    }

    @Test
    void userSettingOverridesLegacyHourlyZero() {
        AppUser user = new AppUser();
        user.setDefaultTaxRate(new BigDecimal("0.08"));
        assertThat(TaxRateResolver.resolve(hourlyJob(), user, null))
                .isEqualByComparingTo("0.08");
    }

    @Test
    void fromPercent_convertsAndValidates() {
        assertThat(TaxRateResolver.fromPercent(8)).isEqualByComparingTo("0.08");
        assertThatThrownBy(() -> TaxRateResolver.fromPercent(101))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static Job flatJob() {
        Ride ride = new Ride();
        ride.setPricingMode(PricingMode.FLAT);
        return ride;
    }

    private static Job hourlyJob() {
        Ride ride = new Ride();
        ride.setPricingMode(PricingMode.HOURLY);
        return ride;
    }

    private static Profile engineerProfile() {
        Profile p = new Profile();
        p.setType(ProfileType.ENGINEER);
        return p;
    }
}