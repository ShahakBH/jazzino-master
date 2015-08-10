package strata.server.worker.event.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.yazino.analytics.AnalyticsEntry;
import com.yazino.bi.persistence.InsertStatementBuilder;
import com.yazino.client.log.ClientLogEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import strata.server.worker.persistence.PostgresDWDAO;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static com.yazino.bi.persistence.InsertStatementBuilder.sqlInt;
import static com.yazino.bi.persistence.InsertStatementBuilder.sqlString;
import static com.yazino.bi.persistence.InsertStatementBuilder.sqlTimestamp;

@Repository
public class PostgresAnalyticsLogDWDAO extends PostgresDWDAO<ClientLogEvent> {
    private static final Logger LOG = LoggerFactory.getLogger(PostgresAnalyticsLogDWDAO.class);
    private final ObjectMapper mapper = new ObjectMapper();

    {
        mapper.registerModule(new JodaModule());
    }

    public PostgresAnalyticsLogDWDAO() {
        // CGLIB Constructor
        super(null);
    }

    @Autowired
    protected PostgresAnalyticsLogDWDAO(@Qualifier("externalDwJdbcTemplate") final JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    @Override
    protected String[] getBatchUpdates(final List<ClientLogEvent> events) {
        return new String[]{createInsertStatements(events)};
    }

    private String createInsertStatements(final List<ClientLogEvent> events) {
        LOG.debug("should be saving events now");

        InsertStatementBuilder statementBuilder = new InsertStatementBuilder("ANALYTICS",
                                                                             "event_id",
                                                                             "type",
                                                                             "session_id",
                                                                             "label",
                                                                             "value",
                                                                             "action",
                                                                             "analytic_ts");

        for (ClientLogEvent event : events) {
            final List<AnalyticsEntry> entryList = deserializePayload(event);
            for (AnalyticsEntry entry : entryList) {
                statementBuilder.withValues(sqlInt(entry.getEventId()),
                                            sqlString(entry.getType()),
                                            sqlString(entry.getSessionId()),
                                            sqlString(entry.getLabel()),
                                            sqlString(entry.getValue()),
                                            sqlString(entry.getAction()),
                                            sqlTimestamp(entry.getTimestampMinusDelta(event.getTimestamp()))
                );
            }
        }

        LOG.debug(statementBuilder.toSql());
        return statementBuilder.toSql();
    }

    private List<AnalyticsEntry> deserializePayload(final ClientLogEvent event) {
        final List<AnalyticsEntry> entryList;
        try {
            entryList = mapper.readValue(event.getPayload(), new TypeReference<List<AnalyticsEntry>>() {
            });
        } catch (IOException e) {
            LOG.error("Could not parse json {}", event.getPayload(), e);
            return Collections.emptyList();
        }
        return entryList;
    }
}
