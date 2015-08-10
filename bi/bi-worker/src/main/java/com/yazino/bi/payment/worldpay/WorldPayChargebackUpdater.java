package com.yazino.bi.payment.worldpay;

import com.yazino.bi.payment.persistence.JDBCPaymentChargebackDAO;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.payment.worldpay.emis.Chargeback;
import com.yazino.payment.worldpay.emis.WorldPayChargebacks;
import com.yazino.payment.worldpay.emis.WorldPayChargebacksParser;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import strata.server.worker.audit.persistence.PostgresExternalTransactionDAO;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

@Service
public class WorldPayChargebackUpdater {
    private static final Logger LOG = LoggerFactory.getLogger(WorldPayChargebackUpdater.class);

    private static final String UPDATE_ACTIVE_PROPERTY = "payment.worldpay.chargeback.update.active";
    private static final String CHARGEBACK_FILENAME_PROPERTY = "payment.worldpay.chargeback.update.filename";

    private static final String DEFAULT_CHARGEBACK_FILENAME = "'YAZOC'yyyyMMdd'.CSV'";

    private final WorldPayFileServer fileServer;
    private final WorldPayChargebacksParser parser;
    private final JDBCPaymentChargebackDAO chargebackDao;
    private final PostgresExternalTransactionDAO externalTransactionDao;
    private final PlayerChargebackHandler playerChargebackHandler;
    private final YazinoConfiguration yazinoConfiguration;

    @Autowired
    public WorldPayChargebackUpdater(final WorldPayFileServer fileServer,
                                     final WorldPayChargebacksParser parser,
                                     final JDBCPaymentChargebackDAO chargebackDao,
                                     final PostgresExternalTransactionDAO externalTransactionDao,
                                     final PlayerChargebackHandler playerChargebackHandler,
                                     final YazinoConfiguration yazinoConfiguration) {
        notNull(fileServer, "fileServer may not be null");
        notNull(parser, "parser may not be null");
        notNull(chargebackDao, "chargebackDao may not be null");
        notNull(externalTransactionDao, "externalTransactionDao may not be null");
        notNull(playerChargebackHandler, "playerChargebackHandler may not be null");
        notNull(yazinoConfiguration, "yazinoConfiguration may not be null");

        this.fileServer = fileServer;
        this.parser = parser;
        this.chargebackDao = chargebackDao;
        this.externalTransactionDao = externalTransactionDao;
        this.playerChargebackHandler = playerChargebackHandler;
        this.yazinoConfiguration = yazinoConfiguration;
    }

    @Scheduled(cron = "${payment.worldpay.chargeback.update.schedule}")
    public void updateChargebacksForYesterday() {
        updateChargebacksFor(new DateTime().minusDays(1));
    }

    public void updateChargebacksFor(final DateTime date) {
        notNull(date, "date may not be null");

        if (!yazinoConfiguration.getBoolean(UPDATE_ACTIVE_PROPERTY, true)) {
            LOG.warn("Chargeback updates are inactive; no actions performed");
            return;
        }

        try {
            LOG.debug("Processing chargebacks for {}", date);
            final int processed = process(retrieveChargebacksFor(date));

            LOG.info("Processed {} chargebacks for date {}", processed, date);

        } catch (Exception e) {
            LOG.error("Chargeback update failed", e);
        }
    }

    private int process(final WorldPayChargebacks worldPayChargebacks) {
        int chargebacksProcessed = 0;
        for (Chargeback worldPayChargeback : worldPayChargebacks.getChargebacks()) {
            LOG.debug("Processing chargeback {}", worldPayChargeback.getCardCentreRef());

            final com.yazino.bi.payment.Chargeback chargeback = chargebackFrom(worldPayChargeback);
            if (chargeback != null) {
                try {
                    chargebackDao.save(chargeback);
                    playerChargebackHandler.handleChargeback(chargeback);

                } catch (Exception e) {
                    LOG.error("Failed to handle chargeback {}", chargeback, e);
                }
            }

            ++chargebacksProcessed;
        }
        return chargebacksProcessed;
    }

    private com.yazino.bi.payment.Chargeback chargebackFrom(final Chargeback chargeback) {
        final BigDecimal playerId = externalTransactionDao.findPlayerIdFor(chargeback.getTransactionId());
        if (playerId == null) {
            LOG.error("Couldn't find player ID for internal transaction ID {}; skipping", chargeback.getTransactionId());
            return null;
        }

        return new com.yazino.bi.payment.Chargeback(chargeback.getCardCentreRef(),
                chargeback.getProcessingDate(),
                chargeback.getTransactionId(),
                chargeback.getTransactionDate(),
                playerId,
                null, chargeback.getReasonCode(),
                chargeback.getChargebackReason(),
                chargeback.getCardNumber(),
                chargeback.getChargebackAmount(),
                chargeback.getCurrency());
    }

    private WorldPayChargebacks retrieveChargebacksFor(final DateTime date) throws IOException {
        File destFilename = null;
        try {
            final String sourceFilename = date.toString(DateTimeFormat.forPattern(
                    yazinoConfiguration.getString(CHARGEBACK_FILENAME_PROPERTY, DEFAULT_CHARGEBACK_FILENAME)));
            destFilename = File.createTempFile("worldpay-chargebacks", "wp");

            LOG.debug("Fetching chargebacks from {} to {}", sourceFilename, destFilename);
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
