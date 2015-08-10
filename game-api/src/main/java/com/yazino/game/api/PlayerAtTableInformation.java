package com.yazino.game.api;

import com.yazino.game.api.document.DocumentBuilder;
import com.yazino.game.api.document.Documentable;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Map;

import static com.yazino.game.api.document.DocumentAccessors.bigDecimalFor;
import static com.yazino.game.api.document.DocumentAccessors.documentFor;
import static org.apache.commons.lang3.Validate.notNull;

public class PlayerAtTableInformation implements Serializable, Documentable {
    private static final long serialVersionUID = -1770695530737124322L;

    private final GamePlayer player;
    private final BigDecimal investedChips;
    private final Map<String, String> properties;

    public PlayerAtTableInformation(final GamePlayer player,
                                    final Map<String, String> properties) {
        this(player, BigDecimal.ZERO, properties);
    }

    public PlayerAtTableInformation(final GamePlayer player,
                                    final BigDecimal investedChips,
                                    final Map<String, String> properties) {
        this.player = player;
        this.properties = properties;
        if (investedChips != null) {
            this.investedChips = investedChips;
        } else {
            this.investedChips = BigDecimal.ZERO;
        }
    }

    @SuppressWarnings("unchecked")
    public PlayerAtTableInformation(final Map<String, Object> document) {
        notNull(document, "document may not be null");

        player = GamePlayer.fromDocument(documentFor(document, "player"));
        investedChips = bigDecimalFor(document, "investedChips");
        properties = (Map<String, String>) document.get("properties");
    }

    public static PlayerAtTableInformation fromDocument(final Map<String, Object> document) {
        if (document == null || document.isEmpty()) {
            return null;
        }
        return new PlayerAtTableInformation(document);
    }

    @Override
    public Map<String, Object> toDocument() {
        return new DocumentBuilder()
                .withDocument("player", player)
                .withBigDecimal("investedChips", investedChips)
                .withPrimitiveMapOf("properties", properties)
                .toDocument();
    }

    public GamePlayer getPlayer() {
        return player;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public BigDecimal getInvestedChips() {
        return investedChips;
    }

    public boolean booleanPropertyValue(final String key) {
        return properties.containsKey(key) && Boolean.parseBoolean(properties.get(key));
    }

    public int intPropertyValue(final String key) {
        if (properties.containsKey(key)) {
            return Integer.parseInt(properties.get(key));
        }
        return 0;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        final PlayerAtTableInformation rhs = (PlayerAtTableInformation) obj;
        return new EqualsBuilder()
                .append(player, rhs.player)
                .append(investedChips, rhs.investedChips)
                .append(properties, rhs.properties)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(player)
                .append(investedChips)
                .append(properties)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(player)
                .append(investedChips)
                .append(properties)
                .toString();
    }

}
