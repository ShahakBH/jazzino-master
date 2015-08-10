package com.yazino.platform.reference;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import static org.apache.commons.lang3.Validate.notNull;

@Repository
public class JDBCCountryRepository {
    private final JdbcTemplate jdbcTemplate;
    private final CountryRowMapper countryMapper = new CountryRowMapper();

    @Autowired
    public JDBCCountryRepository(final JdbcTemplate jdbcTemplate) {
        notNull(jdbcTemplate, "jdbcTemplate may not be null");

        this.jdbcTemplate = jdbcTemplate;
    }

    public Set<Country> getCountries() {
        return new HashSet<Country>(jdbcTemplate.query("SELECT * FROM COUNTRY", countryMapper));
    }

    private static class CountryRowMapper implements RowMapper<Country> {
        @Override
        public Country mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            return new Country(rs.getString("ISO_3166_1_CODE"),
                    rs.getString("NAME"),
                    rs.getString("CURRENCY_ISO_4217_CODE"));
        }
    }

}
