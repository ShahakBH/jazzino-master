package com.yazino.bi.messaging;

import com.yazino.engagement.campaign.domain.NotificationCustomField;
import com.yazino.yaps.JsonHelper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isBlank;

public final class SegmentSelectionCustomDataHelper {

    private SegmentSelectionCustomDataHelper() {
    }

    public static Map<String, String> getCustomData(final ResultSet rs) throws SQLException {
        Map<String, String> customData = new HashMap<>();
        addField(rs, customData);
        return customData;
    }

    @SuppressWarnings("unchecked")
    private static void addField(final ResultSet rs, final Map<String, String> customData) throws SQLException {
        final String progressiveBonus = rs.getString("PROGRESSIVE_BONUS");
        final String displayName = rs.getString("DISPLAY_NAME");
        final String content = rs.getString("CONTENT");
        if (!isBlank(content)) {
            final Map<String, Object> deserialized = new JsonHelper().deserialize(Map.class, content);
            for (String key : deserialized.keySet()) {
                if (deserialized.get(key) == null) {
                    customData.put(key, null);
                } else {
                    customData.put(key, deserialized.get(key).toString());
                }
            }
        }
        if (progressiveBonus != null && !isBlank(progressiveBonus)) {
            customData.put(NotificationCustomField.PROGRESSIVE.name(), progressiveBonus);
        }
        if (displayName != null && !isBlank(displayName)) {
            customData.put(NotificationCustomField.DISPLAY_NAME.name(), displayName);
        }
    }
}
