package strata.server.worker.event.persistence;

import com.yazino.bi.persistence.InsertStatementBuilder;
import com.yazino.platform.event.message.GiftCollectedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import strata.server.worker.persistence.PostgresDWDAO;

import java.util.List;

import static com.yazino.bi.persistence.InsertStatementBuilder.*;

@Repository
public class PostgresGiftCollectedEventDao extends PostgresDWDAO<GiftCollectedEvent> {

    // CG LIB Constructor
    public PostgresGiftCollectedEventDao() {
        super(null);
    }

    @Autowired
    public PostgresGiftCollectedEventDao(@Qualifier("externalDwJdbcTemplate") final JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    @Override
    protected String[] getBatchUpdates(final List<GiftCollectedEvent> events) {
        return new String[]{createInsertStatementFor(events)};
    }

    private String createInsertStatementFor(final List<GiftCollectedEvent> events) {
        InsertStatementBuilder insertBuilder = new InsertStatementBuilder("GIFTS_COLLECTED", "GIFT_ID", "CHOICE", "AMOUNT", "SESSION_ID", "COLLECTED_TS");
        for (GiftCollectedEvent giftCollectedEvent : events) {
            insertBuilder = insertBuilder.withValues(
                    sqlBigDecimal(giftCollectedEvent.getGiftId()),
                    sqlString(giftCollectedEvent.getChoice().name()),
                    sqlBigDecimal(giftCollectedEvent.getGiftAmount()),
                    sqlBigDecimal(giftCollectedEvent.getSessionId()),
                    sqlTimestamp(giftCollectedEvent.getCollectTs()));
        }

        return insertBuilder.toSql();
    }
}
