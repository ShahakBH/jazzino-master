package com.yazino.platform.bonus.persistence;

import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import static org.apache.commons.lang.Validate.notNull;

@Repository
public class BonusDao {

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public BonusDao(final JdbcTemplate jdbcTemplate) {
        notNull(jdbcTemplate, "jdbcTemplate can't be null");
        this.jdbcTemplate = jdbcTemplate;
    }

    public DateTime getLastBonusTime(final BigDecimal playerId) {
        Timestamp ts = null;
        ts = (Timestamp) jdbcTemplate.query("select last_bonus from LOCKOUT_BONUS where player_id=?", new PreparedStatementSetter() {
            @Override
            public void setValues(final PreparedStatement preparedStatement) throws SQLException {
                preparedStatement.setBigDecimal(1, playerId);
            }
        }, new ResultSetExtractor<Timestamp>() {
            @Override
            public Timestamp extractData(final ResultSet resultSet) throws SQLException, DataAccessException {
                if (resultSet.next())

                {
                    return resultSet.getTimestamp(1);
                } else {
                    return null;
                }

            }
        });

        return ts == null ? null : new DateTime(ts.getTime());
    }

    public void setLockoutTime(final BigDecimal playerId, final DateTime lockoutTime) {
        jdbcTemplate.update("replace into LOCKOUT_BONUS (player_id, last_bonus) values(?,?)", new PreparedStatementSetter() {
            @Override
            public void setValues(final PreparedStatement preparedStatement) throws SQLException {
                preparedStatement.setBigDecimal(1, playerId);
                preparedStatement.setTimestamp(2, new Timestamp(lockoutTime.getMillis()));
            }
        });


    }
}
