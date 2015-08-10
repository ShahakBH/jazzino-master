package com.yazino.engagement.mobile;

import com.yazino.platform.Platform;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

class MobileDeviceRowMapper implements RowMapper<MobileDevice> {

    @Override
    public MobileDevice mapRow(ResultSet row, int i) throws SQLException {
        return MobileDevice.builder()
                .withId(row.getLong("id"))
                .withPlayerId(row.getBigDecimal("player_id"))
                .withGameType(row.getString("game_type"))
                .withPlatform(Platform.valueOf(row.getString("platform")))
                .withAppId(row.getString("app_id"))
                .withDeviceId(row.getString("device_id"))
                .withPushToken(row.getString("push_token"))
                .withActive(row.getBoolean("active"))
                .build();
    }

}
