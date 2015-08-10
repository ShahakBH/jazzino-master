package com.yazino.platform.persistence.table;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.yazino.platform.table.GameVariation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static org.apache.commons.lang3.Validate.notNull;

@Repository
public class JDBCGameVariationDAO {
    private static final String SQL_SELECT_ALL
            = "SELECT T.GAME_VARIATION_TEMPLATE_ID,T.NAME AS TEMPLATE_NAME,T.GAME_TYPE,P.NAME,P.VALUE"
            + " FROM GAME_VARIATION_TEMPLATE T, GAME_VARIATION_TEMPLATE_PROPERTY P"
            + " WHERE T.GAME_VARIATION_TEMPLATE_ID = P.GAME_VARIATION_TEMPLATE_ID;";

    private final GameVariationExtractor gameVariationExtractor;
    private final JdbcTemplate template;

    @Autowired
    public JDBCGameVariationDAO(@Qualifier("jdbcTemplate") final JdbcTemplate template) {
        notNull(template, "template may not be null");

        this.template = template;

        gameVariationExtractor = new GameVariationExtractor();
    }

    public Collection<GameVariation> retrieveAll() {
        return template.query(SQL_SELECT_ALL, gameVariationExtractor);
    }

    private static class GameVariationExtractor implements ResultSetExtractor<Collection<GameVariation>> {
        @Override
        public Collection<GameVariation> extractData(final ResultSet rs)
                throws SQLException, DataAccessException {
            final Map<BigDecimal, GameVariationBuilder> gameVariations
                    = new HashMap<BigDecimal, GameVariationBuilder>();

            while (rs.next()) {
                final BigDecimal gameVariationId = rs.getBigDecimal("GAME_VARIATION_TEMPLATE_ID");

                final GameVariationBuilder gameVariation;
                if (gameVariations.containsKey(gameVariationId)) {
                    gameVariation = gameVariations.get(gameVariationId);
                } else {
                    final String templateName = rs.getString("TEMPLATE_NAME");
                    final String gameType = rs.getString("GAME_TYPE");

                    gameVariation = new GameVariationBuilder(gameVariationId, templateName, gameType);
                    gameVariations.put(gameVariationId, gameVariation);
                }

                gameVariation.withProperty(rs.getString("NAME"), rs.getString("VALUE"));
            }

            return newArrayList(Collections2.transform(gameVariations.values(), GameVariationBuilder.transformer()));
        }
    }

    private static class GameVariationBuilder {
        private final Map<String, String> properties = new HashMap<String, String>();
        private final BigDecimal id;
        private final String gameType;
        private final String name;

        public GameVariationBuilder(final BigDecimal id,
                                    final String name,
                                    final String gameType) {
            notNull(id, "id may not be null");
            notNull(gameType, "gameType may not be null");
            notNull(name, "name may not be null");

            this.id = id;
            this.gameType = gameType;
            this.name = name;
        }

        public static Function<GameVariationBuilder, GameVariation> transformer() {
            return new Function<GameVariationBuilder, GameVariation>() {
                @Override
                public GameVariation apply(final GameVariationBuilder gameVariationBuilder) {
                    return gameVariationBuilder.build();
                }
            };
        }

        public GameVariationBuilder withProperty(final String propertyName,
                                                 final String propertyValue) {
            notNull(propertyName, "propertyName may not be null");
            properties.put(propertyName, propertyValue);
            return this;
        }

        public GameVariation build() {
            return new GameVariation(id, gameType, name, new HashMap<String, String>(properties));
        }
    }
}
