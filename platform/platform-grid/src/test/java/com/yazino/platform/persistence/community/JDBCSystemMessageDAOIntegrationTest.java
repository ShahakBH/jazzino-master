package com.yazino.platform.persistence.community;

import com.yazino.platform.model.community.SystemMessage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TransactionConfiguration(defaultRollback = true, transactionManager = "jdbcTransactionManager")
public class JDBCSystemMessageDAOIntegrationTest {

    private SystemMessage invalidMessage;
    private SystemMessage message1;
    private SystemMessage message2;

    @Autowired(required = true)
    @Qualifier("jdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @Autowired(required = true)
    private SystemMessageDAO systemMessageDAO;

    @Before
    @Transactional
    public void setUp() {
        final Calendar startDateCal = Calendar.getInstance();
        startDateCal.set(Calendar.MILLISECOND, 0);
        startDateCal.add(Calendar.DAY_OF_MONTH, -1);
        final Calendar endDateCal = Calendar.getInstance();
        endDateCal.set(Calendar.MILLISECOND, 0);
        endDateCal.add(Calendar.DAY_OF_MONTH, 3);

        message1 = new SystemMessage(BigDecimal.valueOf(1), "Test message 1", startDateCal.getTime(), endDateCal.getTime());

        startDateCal.add(Calendar.DAY_OF_MONTH, -2);
        endDateCal.add(Calendar.DAY_OF_MONTH, 7);
        message2 = new SystemMessage(BigDecimal.valueOf(2), "Test message 2", startDateCal.getTime(), endDateCal.getTime());

        startDateCal.add(Calendar.DAY_OF_MONTH, -30);
        endDateCal.add(Calendar.DAY_OF_MONTH, -30);
        invalidMessage = new SystemMessage(BigDecimal.valueOf(3), "Invalid Message", startDateCal.getTime(), endDateCal.getTime());

        clean();

        jdbcTemplate.update("INSERT INTO SYSTEM_MESSAGE (SYSTEM_MESSAGE_ID,MESSAGE,VALID_FROM,VALID_TO) VALUES (?,?,?,?),(?,?,?,?),(?,?,?,?)",
                message1.getId(), message1.getMessage(), message1.getValidFrom(), message1.getValidTo(),
                message2.getId(), message2.getMessage(), message2.getValidFrom(), message2.getValidTo(),
                invalidMessage.getId(), invalidMessage.getMessage(), invalidMessage.getValidFrom(), invalidMessage.getValidTo());
    }

    @Test
    @Transactional
    public void findValidReturnsAllSystemMessages() {
        final Collection<SystemMessage> match = systemMessageDAO.findValid();

        assertThat(match, is(not(nullValue())));
        assertThat(match, hasItem(message1));
        assertThat(match, hasItem(message2));
    }

    @Test
    @Transactional
    public void findValidExcludesInvalidMessages() {
        final Collection<SystemMessage> match = systemMessageDAO.findValid();

        assertThat(match, is(not(nullValue())));
        assertThat(match, not(hasItem(invalidMessage)));
    }

    private void clean() {
        jdbcTemplate.update("DELETE FROM SYSTEM_MESSAGE");
    }
}
