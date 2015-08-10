package com.yazino.bi.operations.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.yazino.bi.operations.engagement.AppRequestTarget;
import com.yazino.bi.operations.engagement.AppRequestTargetBuilder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * taken from iosPushNotificationAdminController and made more generic
 * iosPushnotificationcontroller should then be deprecated or deleted
 * also tried to refactor and write extra unit tests ... god i tried
 */
@Service("targetReader")
public class TargetCsvReader implements TargetReader {
    private static final Logger LOG = LoggerFactory.getLogger(TargetCsvReader.class);


    @Override
    public Set<AppRequestTarget> readTargets(final InputStream stream) throws IOException {
        notNull(stream, "stream is null");
        final Set<AppRequestTarget> targets = new HashSet<AppRequestTarget>();

        final CsvReader inputReader = new MaximilesSourceCsvReader();
        inputReader.setInputStream(stream);
        List<String> record = inputReader.readNextRecord();

        while (record.size() != 0) {
            if (record.size() >= 2) {
                final String gameType = record.get(0).trim();
                final String playerId = record.get(1).trim();
                String externalId = null;
                if (record.size() == 3) {
                    externalId = record.get(2).trim();
                }
                try {
                    final AppRequestTarget target = new AppRequestTargetBuilder()
                            .withGameType(gameType).withPlayerId(new BigDecimal(playerId))
                            .withExternalId(externalId).build();
                    targets.add(target);
                } catch (NumberFormatException e) {
                    LOG.debug("Failed to upload target {}, playerId must be an integer", record.toString());
                }
            } else {
                LOG.debug("Failed to upload target {}", record.toString());
            }
            record = inputReader.readNextRecord();
        }
        return targets;
    }


}
