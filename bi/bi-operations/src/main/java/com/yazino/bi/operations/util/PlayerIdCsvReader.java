package com.yazino.bi.operations.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import com.yazino.bi.operations.persistence.PlayerInformationDao;
import com.yazino.bi.operations.persistence.TooManyPlayersMatchedToExternalIdException;
import com.yazino.bi.operations.model.PlayerReaderResult;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

@Service("playerIdReader")
public class PlayerIdCsvReader implements PlayerIdReader {
    private static final Logger LOG = LoggerFactory.getLogger(PlayerIdCsvReader.class);
    private final PlayerInformationDao dao;

    /**
     * Creates the reader
     *
     * @param dao DAO used to access the player information
     */
    @Autowired(required = true)
    public PlayerIdCsvReader(final PlayerInformationDao dao) {
        super();
        this.dao = dao;
    }


    @Override
    public PlayerReaderResult readPlayerIds(final InputStream stream) throws IOException {
        notNull(stream, "stream is null");
        final PlayerReaderResult playerReaderResult = new PlayerReaderResult();

        final CsvReader inputReader = new MaximilesSourceCsvReader();
        inputReader.setInputStream(stream);
        for (;;) {
            final List<String> record = inputReader.readNextRecord();
            if (record.size() == 0) {
                break;
            }
            if (record.size() == 1) {
                try {
                    playerReaderResult.addMatched(BigDecimal.valueOf(Long.parseLong(record.get(0).trim())));
                } catch (final NumberFormatException x) {
                    playerReaderResult.addInvalidInputLine(record);
                }
            } else if (record.size() == 2) {
                final String rpx = record.get(0).trim();
                final String xid = record.get(1).trim();
                try {
                    final BigDecimal playerId = dao.getPlayerId(rpx, xid);
                    if (playerId == null) {
                        if (LOG.isWarnEnabled()) {
                            LOG.warn("No existing player in the database for " + rpx + "/" + xid);
                        }
                        playerReaderResult.addNotMatched(rpx, xid);
                    } else {
                        playerReaderResult.addMatched(playerId);
                    }
                } catch (TooManyPlayersMatchedToExternalIdException e) {
                    LOG.error(String.format("Multiple players matched for provider[%s], externalId[%s]. "
                            + "Player Ids matched[%s]", rpx, xid, e.getPlayerIdsMatched()));
                    playerReaderResult.addMultipleMatch(rpx, xid, e.getPlayerIdsMatched());
                }
            } else {
                playerReaderResult.addInvalidInputLine(record);
            }
        }
        return playerReaderResult;
    }
}
