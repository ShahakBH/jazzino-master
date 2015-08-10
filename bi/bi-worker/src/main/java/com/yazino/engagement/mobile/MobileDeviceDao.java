package com.yazino.engagement.mobile;

import com.yazino.platform.Platform;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static com.yazino.engagement.mobile.JdbcParams.buildParams;

@Repository
public class MobileDeviceDao {

    private static final Logger LOG = LoggerFactory.getLogger(MobileDeviceDao.class);
    private static final RowMapper<MobileDevice> ROW_MAPPER = new MobileDeviceRowMapper();

    private static final String INSERT_NEW_DEVICE = "INSERT INTO mobile_device (player_id, platform , app_id, game_type, device_id, push_token, active) "
            + "VALUES (:playerId, :platform, :appId, :gameType, :deviceId, :pushToken, true)";

    private final NamedParameterJdbcTemplate externalDwNamedJdbcTemplate;

    @Autowired
    public MobileDeviceDao(NamedParameterJdbcTemplate externalDwNamedJdbcTemplate) {
        this.externalDwNamedJdbcTemplate = externalDwNamedJdbcTemplate;
    }

    public MobileDevice create(MobileDevice device) {
        MapSqlParameterSource params = new MapSqlParameterSource(toParams(device));
        KeyHolder keyHolder = new GeneratedKeyHolder();
        externalDwNamedJdbcTemplate.update(INSERT_NEW_DEVICE, params, keyHolder);
        Long rowId = (Long) keyHolder.getKeys().get("id");
        return MobileDevice.builder(device).withId(rowId).build();
    }

    public List<MobileDevice> find(BigDecimal playerId, String gameType, Platform platform) {
        return selectActive("player_id = :playerId AND game_type = :gameType AND platform = :platform",
                buildParams("playerId", playerId, "gameType", gameType, "platform", platform.name()));
    }

    public List<MobileDevice> findByDeviceIdExcludingPlayerId(String gameType, Platform platform, String deviceId, BigDecimal excludedPlayerId) {
        Validate.notBlank(deviceId, "deviceId was blank");
        return selectActive("game_type = :gameType AND platform = :platform AND device_id = :deviceId AND player_id <> :playerId",
                buildParams("gameType", gameType, "platform", platform.name(), "deviceId", deviceId, "playerId", excludedPlayerId));
    }

    public List<MobileDevice> findByPushToken(Platform platform, String pushToken) {
        return selectActive("platform = :platform AND push_token = :pushToken",
                buildParams("platform", platform.name(), "pushToken", pushToken));
    }

    public List<MobileDevice> findByPushToken(BigDecimal playerId, Platform platform, String pushToken) {
        return selectActive("player_id = :playerId AND platform = :platform AND push_token = :pushToken",
                buildParams("playerId", playerId, "platform", platform.name(), "pushToken", pushToken));
    }

    public boolean update(MobileDevice device) {
        // NOTE: playerId, gameType and platform are not updateable
        int rows = externalDwNamedJdbcTemplate.update("UPDATE mobile_device "
                + "SET app_id = :appId, device_id = :deviceId, push_token = :pushToken, active = :active "
                + "WHERE id = :id", toParams(device));
        return rows > 0;
    }

    private List<MobileDevice> selectActive(String whereClause, Map<String, Object> params) {
        return externalDwNamedJdbcTemplate.query("SELECT * FROM mobile_device WHERE " + whereClause + " AND active = true", params, ROW_MAPPER);
    }

    private Map<String, Object> toParams(MobileDevice device) {
        return buildParams(
                "id", device.getId(),
                "playerId", device.getPlayerId(),
                "gameType", device.getGameType(),
                "platform", device.getPlatform().name(),
                "appId", device.getAppId(),
                "deviceId", device.getDeviceId(),
                "pushToken", device.getPushToken(),
                "active", device.isActive()
        );
    }

}
