package com.yazino.platform.email.persistence;

import com.google.common.base.Optional;
import com.yazino.email.EmailVerificationResult;
import com.yazino.email.EmailVerificationStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

@Repository
public class JDBCEmailValidationDAO {
    private final String SQL_SELECT = "SELECT * FROM EMAIL_VALIDATION WHERE EMAIL_ADDRESS=?";
    private final String SQL_UPSERT = "INSERT INTO EMAIL_VALIDATION (EMAIL_ADDRESS,STATUS,IS_DISPOSABLE,IS_ROLE) VALUES (?,?,?,?) "
            + "ON DUPLICATE KEY UPDATE STATUS=VALUES(STATUS),IS_DISPOSABLE=VALUES(IS_DISPOSABLE),IS_ROLE=VALUES(IS_ROLE)";

    private final EmailVerificationResultRowMapper rowMapper = new EmailVerificationResultRowMapper();

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public JDBCEmailValidationDAO(@Qualifier("jdbcTemplate") final JdbcTemplate jdbcTemplate) {
        notNull(jdbcTemplate, "jdbcTemplate may not be null");

        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(final EmailVerificationResult emailVerificationResult) {
        notNull(emailVerificationResult, "emailVerificationResult may not be null");

        jdbcTemplate.update(SQL_UPSERT, emailVerificationResult.getAddress(),
                emailVerificationResult.getStatus().getId(),
                emailVerificationResult.isDisposable(),
                emailVerificationResult.isRole());
    }

    public Optional<EmailVerificationResult> findByAddress(final String emailAddress) {
        notNull(emailAddress, "emailAddress may not be null");

        final List<EmailVerificationResult> results = jdbcTemplate.query(SQL_SELECT, rowMapper, emailAddress);
        if (!results.isEmpty()) {
            return Optional.fromNullable(results.get(0));
        }

        return Optional.absent();
    }

    private class EmailVerificationResultRowMapper implements RowMapper<EmailVerificationResult> {
        @Override
        public EmailVerificationResult mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            return new EmailVerificationResult(rs.getString("EMAIL_ADDRESS"),
                    EmailVerificationStatus.forId(rs.getString("STATUS")),
                    rs.getBoolean("IS_DISPOSABLE"),
                    rs.getBoolean("IS_ROLE"));
        }
    }
}
