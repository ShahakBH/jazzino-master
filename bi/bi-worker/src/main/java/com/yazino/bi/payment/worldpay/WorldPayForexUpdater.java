package com.yazino.bi.payment.worldpay;

import com.google.common.base.Optional;
import com.yazino.bi.payment.persistence.JDBCPaymentFXDAO;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.payment.worldpay.fx.CompanyExchangeRates;
import com.yazino.payment.worldpay.fx.ExchangeRate;
import com.yazino.payment.worldpay.fx.WorldPayExchangeRates;
import com.yazino.payment.worldpay.fx.WorldPayExchangeRatesParser;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Currency;

import static java.lang.Long.parseLong;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.Validate.notNull;

@Service
public class WorldPayForexUpdater {
    private static final Logger LOG = LoggerFactory.getLogger(WorldPayForexUpdater.class);

    private static final String UPDATE_ACTIVE_PROPERTY = "payment.worldpay.fx.update.active";
    private static final String FOREX_FILENAME_PROPERTY = "payment.worldpay.fx.update.filename";
    private static final String COMPANIES_PROPERTY = "payment.worldpay.fx.update.companies";
    private static final String COMPANY_PROPERTY = "payment.worldpay.fx.update.companies.%s";

    private static final String DEFAULT_FOREX_FILENAME = "MA.PISCESSW.#D.XRATE.YAZO.TRANSMIT";

    private final WorldPayFileServer fileServer;
    private final WorldPayExchangeRatesParser parser;
    private final JDBCPaymentFXDAO paymentFXDao;
    private final YazinoConfiguration yazinoConfiguration;

    @Autowired
    public WorldPayForexUpdater(final WorldPayFileServer fileServer,
                                final WorldPayExchangeRatesParser parser,
                                final JDBCPaymentFXDAO paymentFXDao,
                                final YazinoConfiguration yazinoConfiguration) {
        notNull(fileServer, "fileServer may not be null");
        notNull(parser, "parser may not be null");
        notNull(paymentFXDao, "paymentFXDao may not be null");
        notNull(yazinoConfiguration, "yazinoConfiguration may not be null");

        this.fileServer = fileServer;
        this.parser = parser;
        this.paymentFXDao = paymentFXDao;
        this.yazinoConfiguration = yazinoConfiguration;
    }

    @Scheduled(cron = "${payment.worldpay.fx.update.schedule}")
    public void updateExchangeRates() {
        if (!yazinoConfiguration.getBoolean(UPDATE_ACTIVE_PROPERTY, true)) {
            LOG.warn("Exchange rate updates are inactive; no actions performed");
            return;
        }

        try {
            final Optional<WorldPayExchangeRates> parsedRates = retrieveExchangeRates();
            if (parsedRates.isPresent()) {
                LOG.debug("Processing rates");
                process(parsedRates.get());

            } else {
                LOG.debug("No rates found to process");
            }

        } catch (Exception e) {
            LOG.error("Forex update failed", e);
        }
    }

    private void process(final WorldPayExchangeRates worldPayExchangeRates) {
        for (Object companyId : yazinoConfiguration.getList(COMPANIES_PROPERTY, emptyList())) {
            try {
                LOG.debug("Looking up rates for company {}", companyId);

                final Optional<CompanyExchangeRates> exchangeRates = worldPayExchangeRates.exchangeRatesFor(parseLong(companyId.toString().trim()));
                if (exchangeRates.isPresent()) {
                    processRatesFor(companyId.toString(), exchangeRates.get());

                } else {
                    LOG.warn("No exchange rates are present for company {}", companyId);
                }

            } catch (NumberFormatException e) {
                LOG.error("Failed to update rates for company {}", companyId, e);
            }
        }
    }

    private void processRatesFor(final String companyId,
                                 final CompanyExchangeRates exchangeRates) {
        final Optional<String> baseCurrency = exchangeRates.baseCurrency();
        if (!baseCurrency.isPresent()) {
            LOG.error("No base currency is present for company {}", companyId);
            return;
        }

        for (Object currencyCode : yazinoConfiguration.getList(String.format(COMPANY_PROPERTY, companyId), emptyList())) {
            final Optional<ExchangeRate> rateForCurrency = exchangeRates.exchangeRateFor(currencyCode.toString().trim());
            if (rateForCurrency.isPresent()) {
                final com.yazino.bi.payment.ExchangeRate exchangeRate = exchangeRateFor(rateForCurrency.get(), baseCurrency.get(), exchangeRates.getAgreementDate());
                LOG.debug("Saving exchange rate {} from company {}", exchangeRate, companyId);
                paymentFXDao.save(exchangeRate);

            } else {
                LOG.warn("No rate is present for company {} and currency {}", companyId, currencyCode);
            }
        }
    }

    private com.yazino.bi.payment.ExchangeRate exchangeRateFor(final ExchangeRate rateForCurrency,
                                                               final String baseCurrency,
                                                               final DateTime agreementDate) {
        return new com.yazino.bi.payment.ExchangeRate(
                Currency.getInstance(rateForCurrency.getCurrencyCode()),
                Currency.getInstance(baseCurrency),
                rateForCurrency.getRate(),
                agreementDate);
    }

    private Optional<WorldPayExchangeRates> retrieveExchangeRates() throws IOException {
        File destFilename = null;
        try {
            final String sourceFilename = yazinoConfiguration.getString(FOREX_FILENAME_PROPERTY, DEFAULT_FOREX_FILENAME);
            destFilename = File.createTempFile("worldpay-forex", "wp");

            LOG.debug("Fetching rates from {} to {}", sourceFilename, destFilename);
            fileServer.fetchTo(sourceFilename, destFilename.getAbsolutePath());

            return parser.parse(new BufferedInputStream(new FileInputStream(destFilename)));

        } finally {
            if (destFilename != null && destFilename.exists()) {
                //noinspection ResultOfMethodCallIgnored
                destFilename.delete();
            }
        }
    }
}
