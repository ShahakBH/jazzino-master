package com.yazino.web.domain.social;

import java.math.BigDecimal;
import java.util.HashMap;

public final class PlayerInformation extends HashMap<String, Object> {
    private static final long serialVersionUID = 2823896110334415941L;

    private PlayerInformation() {
    }

    private PlayerInformation(final HashMap<String, Object> values) {
        super(values);
    }

    public static class Builder {
        private final HashMap<String, Object> values = new HashMap<String, Object>();

        public Builder(final BigDecimal playerId) {
            values.put("playerId", playerId);
        }

        public PlayerInformation build() {
            return new PlayerInformation(values);
        }

        public Builder withField(final PlayerInformationType type, final Object value) {
            if (value != null) {
                values.put(type.name().toLowerCase(), value);
            }
            return this;
        }
    }
}
