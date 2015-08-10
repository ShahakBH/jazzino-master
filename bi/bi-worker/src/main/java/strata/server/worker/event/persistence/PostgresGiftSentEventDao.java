package strata.server.worker.event.persistence;

import com.yazino.bi.persistence.InsertStatementBuilder;
import com.yazino.platform.event.message.GiftSentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import strata.server.worker.persistence.PostgresDWDAO;

import java.util.List;

import static com.yazino.bi.persistence.InsertStatementBuilder.sqlBigDecimal;
import static com.yazino.bi.persistence.InsertStatementBuilder.sqlTimestamp;

@Repository
public class PostgresGiftSentEventDao extends PostgresDWDAO<GiftSentEvent> {


    private static final Logger LOG = LoggerFactory.getLogger(PostgresGiftSentEventDao.class);

    //CG Lib
    public PostgresGiftSentEventDao() {
        super(null);
    }

    @Autowired
    public PostgresGiftSentEventDao(@Qualifier("externalDwJdbcTemplate") final JdbcTemplate dwTemplate) {
        super(dwTemplate);
    }

    @Override
    protected String[] getBatchUpdates(final List<GiftSentEvent> events) {
        return new String[]{createInsertStatementFor(events)};
    }

    private String createInsertStatementFor(final List<GiftSentEvent> events) {
        InsertStatementBuilder insertBuilder = new InsertStatementBuilder(
                "GIFTS_SENT", "GIFT_ID", "SENDER_ID", "RECEIVER_ID", "EXPIRY_TS", "SENT_TS", "SESSION_ID");
        for (GiftSentEvent giftSentEvent : events) {
            LOG.debug("sending gift:{}", giftSentEvent.toString());
            insertBuilder = insertBuilder.withValues(
                    sqlBigDecimal(giftSentEvent.getGiftId()),
                    sqlBigDecimal(giftSentEvent.getSender()),
                    sqlBigDecimal(giftSentEvent.getReceiver()),
                    sqlTimestamp(giftSentEvent.getExpiry()),
                    sqlTimestamp(giftSentEvent.getNow()),
                    sqlBigDecimal(giftSentEvent.getSessionId()));
        }

        return insertBuilder.toSql();
    }
}
