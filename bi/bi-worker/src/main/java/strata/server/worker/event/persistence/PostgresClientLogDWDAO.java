package strata.server.worker.event.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
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
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.yazino.bi.persistence.InsertStatementBuilder.*;

@Repository
public class PostgresClientLogDWDAO extends PostgresDWDAO<ClientLogEvent> {
    private static final Logger LOG = LoggerFactory.getLogger(PostgresClientLogDWDAO.class);
    public static final int MAX_MSG_LENGTH = 255;
    public static final int MAX_PAYLOAD_LENGTH = 65535;
    public static final int MAX_STACK_TRACE_LENGTH = 65535;
    public static final int MAX_MODEL_LENGTH = 32;

    private final ObjectMapper mapper = new ObjectMapper();

    {
        mapper.registerModule(new JodaModule());
    }

    public PostgresClientLogDWDAO() {
        //cglib
        super(null);
    }

    @Autowired
    public PostgresClientLogDWDAO(@Qualifier("externalDwJdbcTemplate") JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }


    @Override
    protected String[] getBatchUpdates(final List<ClientLogEvent> events) {
        return new String[]{createInsertStatementFor(events)};
    }

    private String createInsertStatementFor(List<ClientLogEvent> clientLogEvents) {
        InsertStatementBuilder insertBuilder = new InsertStatementBuilder("client_log",
                "log_ts", "payload", "game_type", "version", "platform", "message", "error_code",
                "stacktrace", "player_id", "table_id", "model", "api", "net");

        for (ClientLogEvent event : clientLogEvents) {
            final Map<String, Object> logData = deserialisePayload(event);
            insertBuilder = insertBuilder.withValues(
                    sqlTimestamp(new Date(event.getTimestamp().getMillis())),
                    sqlString(getStringWithMaxLength(event.getPayload(), MAX_PAYLOAD_LENGTH)),
                    sqlString(getStringOrNull(logData, "gameType")),
                    sqlString(getStringOrNull(logData, "version")),
                    sqlString(getStringOrNull(logData, "platform")),
                    sqlString(getStringWithMaxLengthOrNull(logData, "msg", MAX_MSG_LENGTH)),
                    sqlInt(getIntegerOrNull(logData, "code")),
                    sqlString(getStringWithMaxLengthOrNull(logData, "stacktrace", MAX_STACK_TRACE_LENGTH)),
                    sqlBigDecimal(getBigDecimalOrNull(logData, "playerId")),
                    sqlBigDecimal(getBigDecimalOrNull(logData, "tableId")),
                    sqlString(getStringWithMaxLengthOrNull(logData, "model", MAX_MODEL_LENGTH)),
                    sqlString(getStringOrNull(logData, "api")),
                    sqlString(getStringOrNull(logData, "net"))
            );
        }
        return insertBuilder.toSql();
    }

    private String getStringWithMaxLength(String value, int maxLength) {
        if (value != null && value.length() > maxLength) {
            value = value.substring(0, maxLength);
        }
        return value;
    }

    private BigDecimal getBigDecimalOrNull(Map<String, Object> logData, String key) {
        Object value = logData.get(key);
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer getIntegerOrNull(Map<String, Object> logData, String key) {
        Object value = logData.get(key);
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String getStringOrNull(Map<String, Object> logData, String key) {
        Object value = logData.get(key);
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    private String getStringWithMaxLengthOrNull(Map<String, Object> logData, String key, Integer maxLength) {
        String str = getStringOrNull(logData, key);
        return getStringWithMaxLength(str, maxLength);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> deserialisePayload(ClientLogEvent event) {
        try {
            return mapper.readValue(event.getPayload(), Map.class);
        } catch (IOException e) {
            LOG.error("Could not parse json {}", event.getPayload(), e);
        }
        return Collections.emptyMap();
    }
}
