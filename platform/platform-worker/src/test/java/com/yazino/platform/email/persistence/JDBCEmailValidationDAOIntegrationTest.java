package com.yazino.platform.email.persistence;

import com.google.common.base.Optional;
import com.yazino.email.EmailVerificationResult;
import com.yazino.email.EmailVerificationStatus;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static com.yazino.email.EmailVerificationStatus.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.is;

@ContextConfiguration
@Transactional
@RunWith(SpringJUnit4ClassRunner.class)
public class JDBCEmailValidationDAOIntegrationTest {

    @Autowired
    private JDBCEmailValidationDAO underTest;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void aNonExistentEmailAddressReturnsAnAbsentOptional() {
        final Optional<EmailVerificationResult> result = underTest.findByAddress("aNonExistentEmail");

        assertThat(result.isPresent(), is(false));
    }

    @Test
    public void anExitingEmailAddressReturnsThePersistedValidationResult() {
        jdbcTemplate.update("INSERT INTO EMAIL_VALIDATION (EMAIL_ADDRESS,STATUS,IS_DISPOSABLE,IS_ROLE) VALUES (?,?,?,?)",
                "aTestEmail@example.com", MALFORMED.getId(), false, true);

        final Optional<EmailVerificationResult> result = underTest.findByAddress("aTestEmail@example.com");

        assertThat(result.isPresent(), is(true));
        assertThat(result.get(), is(equalTo(new EmailVerificationResult("aTestEmail@example.com", MALFORMED, false, true))));
    }

    @Test
    public void aNewValidationResultIsSavedToTheDatabase() {
        underTest.save(new EmailVerificationResult("aTestEmail@example.com", VALID, false, false));

        final Map<String,Object> resultsFromDb = jdbcTemplate.queryForMap("SELECT * FROM EMAIL_VALIDATION WHERE EMAIL_ADDRESS=?", "aTestEmail@example.com");

        assertThat((String) resultsFromDb.get("EMAIL_ADDRESS"), is(equalTo("aTestEmail@example.com")));
        assertThat(EmailVerificationStatus.forId((String) resultsFromDb.get("STATUS")), is(equalTo(VALID)));
        assertThat((Boolean) resultsFromDb.get("IS_DISPOSABLE"), is(equalTo(false)));
        assertThat((Boolean) resultsFromDb.get("IS_ROLE"), is(equalTo(false)));
    }

    @Test
    public void anExistingValidationResultIsUpdatedTheDatabase() {
        underTest.save(new EmailVerificationResult("aTestEmail@example.com", UNKNOWN_TEMPORARY, false, false));

        underTest.save(new EmailVerificationResult("aTestEmail@example.com", INVALID, true, true));

        final Map<String,Object> resultsFromDb = jdbcTemplate.queryForMap("SELECT * FROM EMAIL_VALIDATION WHERE EMAIL_ADDRESS=?", "aTestEmail@example.com");

        assertThat((String) resultsFromDb.get("EMAIL_ADDRESS"), is(equalTo("aTestEmail@example.com")));
        assertThat(EmailVerificationStatus.forId((String) resultsFromDb.get("STATUS")), is(equalTo(INVALID)));
        assertThat((Boolean) resultsFromDb.get("IS_DISPOSABLE"), is(equalTo(true)));
        assertThat((Boolean) resultsFromDb.get("IS_ROLE"), is(equalTo(true)));
    }

}
