package com.yazino.platform.service.community;

import com.yazino.configuration.YazinoConfiguration;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static org.apache.commons.lang3.Validate.notNull;

@Service
public class GiftProperties {
    private static final String PROPERTY_REGIFT_COOLDOWN_PERIOD = "strata.gifting.regiftcooldownperiod";
    private static final String PROPERTY_EXPIRY_TIME = "strata.gifting.expiryOfGiftInHours";
    private static final String PROPERTY_MAX_GIFT_COLLECTIONS = "strata.gifting.maxGiftsPerDay";
    private static final String PROPERTY_TIME_ZONE = "strata.gifting.timeZone";
    private static final String PROPERTY_GIFT_RETENTION = "gifting.retention-hours";
    private static final int DEFAULT_REGIFT_COOLDOWN_PERIOD = 86400; // 24 hours
    private static final int DEFAULT_EXPIRY_TIME = 96;
    private static final int DEFAULT_MAX_GIFT_COLLECTIONS = 25;
    private static final String DEFAULT_TIME_ZONE = "America/New_York";
    private static final int DEFAULT_GIFT_RETENTION = 168;

    private final YazinoConfiguration yazinoConfiguration;

    @Autowired
    public GiftProperties(final YazinoConfiguration yazinoConfiguration) {
        notNull(yazinoConfiguration, "yazinoConfiguration may not be null");

        this.yazinoConfiguration = yazinoConfiguration;
    }

    public int regiftCooldownPeriodInSeconds() {
        return yazinoConfiguration.getInt(PROPERTY_REGIFT_COOLDOWN_PERIOD, DEFAULT_REGIFT_COOLDOWN_PERIOD);
    }

    public int expiryTimeInHours() {
        return yazinoConfiguration.getInt(PROPERTY_EXPIRY_TIME, DEFAULT_EXPIRY_TIME);
    }

    public int maxGiftCollectionsPerDay() {
        return yazinoConfiguration.getInt(PROPERTY_MAX_GIFT_COLLECTIONS, DEFAULT_MAX_GIFT_COLLECTIONS);
    }

    public int remainingGiftCollections(final int collectedToday) {
        return Math.max(maxGiftCollectionsPerDay() - collectedToday, 0);
    }

    public int retentionInHours() {
        return yazinoConfiguration.getInt(PROPERTY_GIFT_RETENTION, DEFAULT_GIFT_RETENTION);
    }

    public DateTime endOfGiftPeriod() {
        final DateTime nextDateOfMidnight = new DateTime(timeZone()).withTimeAtStartOfDay().plusDays(1);
        return nextDateOfMidnight.toDateTime(DateTimeZone.getDefault());
    }

    public DateTime startOfGiftPeriod() {
        final DateTime nextDateOfMidnight = new DateTime(timeZone()).withTimeAtStartOfDay();
        return nextDateOfMidnight.toDateTime(DateTimeZone.getDefault());
    }

    private DateTimeZone timeZone() {
        return DateTimeZone.forID(yazinoConfiguration.getString(PROPERTY_TIME_ZONE, DEFAULT_TIME_ZONE));
    }
}
