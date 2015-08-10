package com.yazino.platform.invitation.persistence;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import com.yazino.logging.appender.ListAppender;
import com.yazino.platform.invitation.InvitationSource;
import com.yazino.platform.invitation.InvitationStatus;
import org.hamcrest.Matcher;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "JDBCInvitationDAOTest-context.xml")
@TransactionConfiguration(defaultRollback = true)
public class JDBCInvitationDAOSafetyTest {
    private static final String FALLBACK_LOGGER_NAME = "strata.datawarehouse.fallback";
    private static final String SQL_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String RECIPIENT_ID = "recipient";
    private static final InvitationSource SOURCE = InvitationSource.FACEBOOK;
    private static final InvitationStatus STATUS = InvitationStatus.WAITING;
    private static final DateTime CREATE_TIME = new DateTime(2009, 3, 30, 23, 45, 59, 345);
    private static final String GAME_TYPE = "GAME_TYPE";
    private static final String SCREEN_SOURCE = "FB_SCREEN";

    private JdbcTemplate mockJdbcTemplate = mock(JdbcTemplate.class);
    private JDBCInvitationDAO daoUnderTest;
    private Invitation sampleInvitation;

    @Autowired
    @Qualifier(value = "jdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    private Logger primaryLogger;
    private Logger fallbackLogger;

    @Before
    public void before() {
        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        primaryLogger = lc.getLogger(JDBCInvitationDAO.class);
        primaryLogger.detachAndStopAllAppenders();

        fallbackLogger = lc.getLogger(FALLBACK_LOGGER_NAME);
        fallbackLogger.detachAndStopAllAppenders();

        daoUnderTest = new JDBCInvitationDAO(mockJdbcTemplate);

        sampleInvitation =
                new Invitation(BigDecimal.ONE, RECIPIENT_ID, SOURCE, STATUS, CREATE_TIME, GAME_TYPE, SCREEN_SOURCE);
    }

    @Transactional
    @Test
    public void create_shouldNeverThrowException() {
        doThrow(new RuntimeException("UH OH")).when(mockJdbcTemplate).update(anyString(), any(PreparedStatementSetter.class));

        daoUnderTest.save(sampleInvitation);

        verify(mockJdbcTemplate).update(anyString(), any(PreparedStatementSetter.class)); // sanity check
        // that exception
        // will actually
        // have been
        // thrown
    }

    @Transactional
    @Test
    public void create_whenAnExceptionIsCaughtTheSQLShouldBeLoggedToTheFallbackLogFile() {
        final ListAppender fallbackAppender = ListAppender.addTo(FALLBACK_LOGGER_NAME);
        doThrow(new RuntimeException("UH OH")).when(mockJdbcTemplate).update(anyString(), any(PreparedStatementSetter.class));

        daoUnderTest.save(sampleInvitation);

        verify(mockJdbcTemplate).update(anyString(), any(PreparedStatementSetter.class)); // sanity check
        // that exception
        // will actually
        // have been
        // thrown
        final List<String> loggedMessages = fallbackAppender.getMessages();
        assertThat(loggedMessages, hasItem(insertSqlFor(sampleInvitation)));
    }

    private String insertSqlFor(final Invitation invitation) {
        return new PreparedStatementFormatter().format(JDBCInvitationDAO.SQL_INSERT_UPDATE+";",
                invitation.getIssuingPlayerId(), sampleInvitation.getRecipientIdentifier(), sampleInvitation
                .getSource().name(), sampleInvitation.getStatus().name(), sampleInvitation
                .getRewardAmount(), invitation.getCreateTime().toDate(),
                invitation.getUpdateTime().toDate(), invitation.getGameType(),
                invitation.getScreenSource());
    }

    @Transactional
    @Test
    public void create_validSqlWrittenToTheFallbackLogFile() {
        try {
            final String sql = throwExceptionAndCaptureCreateSql();
            jdbcTemplate.execute(sql);
        } catch (final Exception e) {
            fail("Expected valid insert statement");
        }
    }

    private String throwExceptionAndCaptureCreateSql() {
        final ListAppender fallbackAppender = ListAppender.addTo(FALLBACK_LOGGER_NAME);
        doThrow(new RuntimeException("UH OH")).when(mockJdbcTemplate).update(anyString(), any(PreparedStatementSetter.class));
        daoUnderTest.save(sampleInvitation);
        final List<String> loggedMessages = fallbackAppender.getMessages();
        return loggedMessages.get(loggedMessages.size() - 1);
    }

    @Transactional
    @Test
    public void create_whenAnExceptionIsCaughtAndTheFallbackFailsTheExceptionIsNotPropagated() {
        final Appender<ILoggingEvent> failingAppender = mock(Appender.class);
        doThrow(new RuntimeException("UH OH")).when(mockJdbcTemplate).update(anyString(), any(PreparedStatementSetter.class));
        doThrow(new RuntimeException("anException")).when(failingAppender).doAppend(any(ILoggingEvent.class));
        fallbackLogger.addAppender(failingAppender);

        daoUnderTest.save(sampleInvitation);

        verify(failingAppender).doAppend(any(ILoggingEvent.class)); // sanity check that
        // exception will actually
        // have been thrown
    }

    @Transactional
    @Test
    public void create_sqlLoggedToPrimaryLogFileWhenWriteToFallbackLogFails() {
        final Appender<ILoggingEvent> failingAppender = mock(Appender.class);
        doThrow(new RuntimeException("UH OH")).when(mockJdbcTemplate).update(anyString(), any(PreparedStatementSetter.class));
        doThrow(new RuntimeException("anException")).when(failingAppender).doAppend(any(ILoggingEvent.class));
        fallbackLogger.addAppender(failingAppender);
        final ListAppender primaryAppender = ListAppender.addTo(primaryLogger.getName());

        daoUnderTest.save(sampleInvitation);

        final List<String> messages = primaryAppender.getMessages();
        final Matcher<Iterable<? super String>> matcher = hasItem("Failed to write to fallback log: "
                + insertSqlFor(sampleInvitation));
        assertThat(messages, matcher);
    }

}
