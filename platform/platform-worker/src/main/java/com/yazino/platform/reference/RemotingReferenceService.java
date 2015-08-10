package com.yazino.platform.reference;

import com.yazino.configuration.YazinoConfiguration;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.apache.commons.lang3.Validate.notNull;

@Service("referenceService")
public class RemotingReferenceService implements ReferenceService {
    private static final Logger LOG = LoggerFactory.getLogger(RemotingReferenceService.class);

    private static final int ONE_MINUTE = 60000;
    private static final String DEFAULT_CURRENCY_PROPERTY = "payments.currency.default";
    private static final Currency DEFAULT_CURRENCY_FALLBACK_VALUE = Currency.USD;

    private final ReadWriteLock countryLock = new ReentrantReadWriteLock();
    private final Set<Country> countries = new HashSet<Country>();

    private final JDBCCountryRepository countryRepository;

    private YazinoConfiguration yazinoConfiguration;
    private int cacheTimeInMillis = ONE_MINUTE;
    private DateTime updatedTime;

    @Autowired
    public RemotingReferenceService(final JDBCCountryRepository countryRepository,
                                    final YazinoConfiguration yazinoConfiguration) {
        notNull(countryRepository, "countryRepository may not be null");
        notNull(yazinoConfiguration, "yazinoConfiguration may not be null");

        this.countryRepository = countryRepository;
        this.yazinoConfiguration = yazinoConfiguration;
    }

    @Override
    public Set<Country> getCountries() {
        return countries();
    }

    @Override
    public Currency getPreferredCurrency(final String countryNameOrISOCode) {
        if (countryNameOrISOCode == null) {
            return defaultCurrency();
        }

        for (Country country : countries()) {
            if (country.getIso3166CountryCode().equals(countryNameOrISOCode)
                    || country.getName().equals(countryNameOrISOCode)) {
                try {
                    return Currency.valueOf(country.getIso4217CurrencyCode());

                } catch (IllegalArgumentException e) {
                    LOG.error("Currency for country {} is malformed or unknown: {}",
                            country.getIso3166CountryCode(), country.getIso4217CurrencyCode());
                }
            }
        }

        return defaultCurrency();
    }

    private Currency defaultCurrency() {
        try {
            final String defaultCurrency = yazinoConfiguration.getString(DEFAULT_CURRENCY_PROPERTY);
            if (defaultCurrency != null) {
                return Currency.valueOf(defaultCurrency);
            }
        } catch (NoSuchElementException e) {
            LOG.error("Couldn't load default currency from property " + DEFAULT_CURRENCY_PROPERTY, e);
        }

        return DEFAULT_CURRENCY_FALLBACK_VALUE;
    }

    void setCacheTimeInMillis(final int cacheTimeInMillis) {
        this.cacheTimeInMillis = cacheTimeInMillis;
    }

    private Set<Country> countries() {
        countryLock.readLock().lock();
        try {
            if (updatedTime == null
                    || updatedTime.plusMillis(cacheTimeInMillis).isBefore(new DateTime())) {
                countryLock.readLock().unlock();
                updateCountriesFromRepository();
                countryLock.readLock().lock();
            }
            return countries;
        } finally {
            countryLock.readLock().unlock();
        }
    }

    private void updateCountriesFromRepository() {
        LOG.debug("Reading countries from repository");

        countryLock.writeLock().lock();
        try {
            countries.clear();
            countries.addAll(countryRepository.getCountries());
            updatedTime = new DateTime();
        } finally {
            countryLock.writeLock().unlock();
        }
    }
}
