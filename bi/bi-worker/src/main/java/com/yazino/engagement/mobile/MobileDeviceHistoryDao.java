package com.yazino.engagement.mobile;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;

import static com.yazino.engagement.mobile.JdbcParams.buildParams;

@Repository
public class MobileDeviceHistoryDao {

    private final NamedParameterJdbcTemplate externalDwNamedJdbcTemplate;

    @Autowired
    public MobileDeviceHistoryDao(NamedParameterJdbcTemplate externalDwNamedJdbcTemplate) {
        this.externalDwNamedJdbcTemplate = externalDwNamedJdbcTemplate;
    }

    public void recordEvent(final Long rowId, final MobileDeviceEvent event, final String detail) {

        final Map<String, Object> params = buildParams("id", rowId, "event", event.name(), "detail", detail);

        externalDwNamedJdbcTemplate.update("insert into MOBILE_DEVICE_HISTORY (id,event,detail) values(:id, :event, :detail)", params);
    }

}
