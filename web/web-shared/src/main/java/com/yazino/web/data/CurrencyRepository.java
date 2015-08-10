package com.yazino.web.data;

import com.googlecode.ehcache.annotations.Cacheable;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.reference.Currency;
import com.yazino.platform.reference.ReferenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.Set;

import static com.google.common.collect.Sets.newLinkedHashSet;
import static org.apache.commons.lang3.Validate.notNull;

@Repository
public class CurrencyRepository {
    private static final Logger LOG = LoggerFactory.getLogger(CurrencyRepository.class);

    private static final String ACCEPTED_CURRENCIES = "payments.currency.accepted";
    private static final String DEFAULT_CURRENCY = "USD";

    private final ReferenceService referenceService;
    private final YazinoConfiguration yazinoConfiguration;

    CurrencyRepository() {
        // cglib constructor

        this.referenceService = null;
        this.yazinoConfiguration = null;
    }

    @Autowired
    public CurrencyRepository(final ReferenceService referenceService,
                              final YazinoConfiguration yazinoConfiguration) {
        notNull(referenceService, "referenceService may not be null");
        notNull(yazinoConfiguration, "yazinoConfiguration may not be null");

        this.referenceService = referenceService;
        this.yazinoConfiguration = yazinoConfiguration;
    }

    private void verifyInitialisation() {
        if (referenceService == null
                || yazinoConfiguration == null) {
            throw new IllegalStateException("Class was created with CGLib constructor");
        }
    }

    @Cacheable(cacheName = "preferredCurrencyCache")
    public Currency getPreferredCurrencyFor(final String countryCodeOrName) {
        verifyInitialisation();

        return referenceService.getPreferredCurrency(countryCodeOrName);
    }

    @Cacheable(cacheName = "acceptedCurrencyCache")
    public Set<Currency> getAcceptedCurrencies() {
        verifyInitialisation();

        final Set<Currency> acceptedCurrencies = newLinkedHashSet();
        for (Object currencyCode : yazinoConfiguration.getList(ACCEPTED_CURRENCIES, Collections.emptyList())) {
            try {
                acceptedCurrencies.add(Currency.valueOf(currencyCode.toString()));

            } catch (IllegalArgumentException e) {
                LOG.error("Accepted currency code {} is not a valid currency, ignoring.", currencyCode);
            }
        }

        if (acceptedCurrencies.isEmpty()) {
            LOG.debug("No accepted currencies found, falling back to {}", DEFAULT_CURRENCY);
            acceptedCurrencies.add(Currency.valueOf(DEFAULT_CURRENCY));

        } else {
            LOG.debug("Accepted currencies are {}", acceptedCurrencies);
        }

        return acceptedCurrencies;
    }
}
