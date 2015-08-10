package com.yazino.platform.processor.table;

import com.j_spaces.core.client.SQLQuery;
import com.yazino.platform.model.session.InboxMessage;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.model.table.TableControlMessage;
import com.yazino.platform.model.table.TableControlMessageType;
import com.yazino.platform.model.table.TableRequestWrapper;
import com.yazino.platform.repository.session.InboxMessageRepository;
import com.yazino.platform.util.concurrent.ThreadPoolFactory;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openspaces.core.GigaSpace;
import com.yazino.game.api.GameType;
import com.yazino.game.api.NewsEvent;
import com.yazino.game.api.NewsEventType;
import com.yazino.game.api.ParameterisedMessage;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PrivateTableExpirerTest {

    private static final long ONE_DAY_IN_MS = 1000 * 60 * 60 * 24;
    private static final int NUMBER_TO_PROCESS = Integer.MAX_VALUE;
    private static final int THIRTY_DAYS = 30;
    private static final int ONE_DAY = 1;
    private static final int DEFAULT_NOTIFICATION_PERIOD = ONE_DAY;
    private static final int DEFAULT_EXPIRY_PERIOD = THIRTY_DAYS;
    private static final BigDecimal EXPIRED_TABLE_1 = BigDecimal.valueOf(10);
    private static final BigDecimal EXPIRED_TABLE_2 = BigDecimal.valueOf(20);
    private static final BigDecimal WARNING_TABLE_1 = BigDecimal.valueOf(30);
    private static final BigDecimal WARNING_TABLE_2 = BigDecimal.valueOf(40);
    private static final BigDecimal PLAYER_1 = BigDecimal.valueOf(300);
    private static final BigDecimal PLAYER_2 = BigDecimal.valueOf(400);
    private static final GameType GAME_TYPE = new GameType("ROULETTE", "Roulette", Collections.<String>emptySet());
    private static final long INITIAL_SCAN = 120 * 1000L;

    @Mock
    private GigaSpace gigaSpace;
    @Mock
    private ThreadPoolFactory threadPoolFactory;
    @Mock
    private ScheduledExecutorService scheduledExecutorService;
    @Mock
    private ScheduledFuture scheduledFuture;
    @Mock
    private InboxMessageRepository inboxMessageRepository;

    private PrivateTableExpirer underTest;

    @SuppressWarnings({"unchecked"})
    @Before
    public void setUp() throws Exception {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(System.currentTimeMillis());

        underTest = new PrivateTableExpirer(gigaSpace, threadPoolFactory, inboxMessageRepository);

        when(threadPoolFactory.getScheduledThreadPool(1)).thenReturn(scheduledExecutorService);
        when(scheduledExecutorService.scheduleAtFixedRate(
                any(PrivateTableExpirer.EventTask.class), eq(INITIAL_SCAN), eq(ONE_DAY_IN_MS), eq(MILLISECONDS)))
                .thenReturn(scheduledFuture);

        when(gigaSpace.readMultiple(tablesUpdatedBefore(warningTime()), NUMBER_TO_PROCESS)).thenReturn(new Table[]{
                anExpiredTableWithId(EXPIRED_TABLE_1), anExpiredTableWithId(EXPIRED_TABLE_2),
                aSoonToExpireTableWithId(WARNING_TABLE_1), aSoonToExpireTableWithId(WARNING_TABLE_2)});
    }

    @After
    public void cleanUp() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void theDefaultPollPeriodShouldBeOnceADay() {
        assertThat(underTest.getPollingDelay(), is(equalTo(ONE_DAY_IN_MS)));
    }

    @Test
    public void theDefaultTimeToWaitBeforeExpiryShouldBeThirtyDays() {
        assertThat(underTest.getDaysToWaitBeforeExpiring(), is(equalTo(THIRTY_DAYS)));
    }

    @Test
    public void theDefaultTimeToWarnOwnersBeforeExpiryShouldBeOneDay() {
        assertThat(underTest.getDaysToWarnBeforeExpiring(), is(equalTo(ONE_DAY)));
    }

    @Test
    public void anExecutorIsCreatedWithTheCorrectPollingPeriodAndAnInitialPollInTwoMinutes() throws Exception {
        underTest.initialise();

        verify(scheduledExecutorService).scheduleAtFixedRate(
                any(PrivateTableExpirer.EventTask.class), eq(INITIAL_SCAN), eq(ONE_DAY_IN_MS), eq(MILLISECONDS));
    }

    @Test
    public void anExecutorIsCreatedWithACustomPollingPeriodAndAnImmediatePoll() throws Exception {
        final long customPollingPeriod = 10000;
        underTest.setPollingDelay(customPollingPeriod);

        underTest.initialise();

        verify(scheduledExecutorService).scheduleAtFixedRate(
                any(PrivateTableExpirer.EventTask.class), eq(INITIAL_SCAN), eq(customPollingPeriod), eq(MILLISECONDS));
    }

    @Test(expected = RuntimeException.class)
    public void anyExceptionDuringSchedulerCreationIsPropagatedToTheSpace() throws Exception {
        when(scheduledExecutorService.scheduleAtFixedRate(
                any(Runnable.class), eq(INITIAL_SCAN), eq(ONE_DAY_IN_MS), eq(MILLISECONDS)))
                .thenThrow(new RuntimeException("anException"));

        underTest.initialise();
    }

    @Test(expected = IllegalStateException.class)
    public void theExpirerCannotBeInitialisedTwice() throws Exception {
        underTest.initialise();

        underTest.initialise();
    }

    @Test
    public void theExecutorIsClosedOnShutdown() throws Exception {
        underTest.initialise();

        underTest.shutdown();

        verify(scheduledFuture).cancel(true);
        verify(scheduledExecutorService).shutdown();
    }

    @Test
    public void anyExceptionsDuringShutdownAreNotPropagatedToTheSpace() throws Exception {
        underTest.initialise();
        doThrow(new RuntimeException("anException")).when(scheduledFuture).cancel(true);

        underTest.shutdown();
    }

    @Test
    public void theExpirerCanBeInitialisedAgainAfterShutdown() throws Exception {
        underTest.initialise();
        underTest.shutdown();

        underTest.initialise();
    }

    @Test
    public void theSpaceIsQueriedForAllTablesUnusedForTheExpiryTimeMinusTheNotificationTime() throws Exception {
        executeTask();

        verify(gigaSpace).readMultiple(tablesUpdatedBefore(warningTime()), NUMBER_TO_PROCESS);
    }

    private long warningTime() {
        return currentTime().minusDays(DEFAULT_EXPIRY_PERIOD - DEFAULT_NOTIFICATION_PERIOD).getMillis();
    }

    @Test
    public void anyExpiredTablesAreSentCloseMessages() throws Exception {
        executeTask();

        verify(gigaSpace).readMultiple(tablesUpdatedBefore(warningTime()), NUMBER_TO_PROCESS);
        verify(gigaSpace).write(closeMessageFor(EXPIRED_TABLE_1));
        verify(gigaSpace).write(closeMessageFor(EXPIRED_TABLE_2));
        verifyNoMoreInteractions(gigaSpace);
    }

    @Test
    public void anySoonToExpireTablesResultInWarningMessagesSentToTheirOwner() throws Exception {
        executeTask();

        final DateTime expectedExpiryDate = new DateTime().plusDays(1);
        verify(inboxMessageRepository).send(new InboxMessage(PLAYER_1, expiryWarningFor(PLAYER_1, expectedExpiryDate), new DateTime()));
        verify(inboxMessageRepository).send(new InboxMessage(PLAYER_2, expiryWarningFor(PLAYER_2, expectedExpiryDate), new DateTime()));
        verifyNoMoreInteractions(inboxMessageRepository);
    }

    @Test
    public void anyExceptionsFromExpiryAreNotPropagated() throws Exception {
        when(gigaSpace.readMultiple(tablesUpdatedBefore(warningTime()), NUMBER_TO_PROCESS))
                .thenThrow(new RuntimeException("anException"));

        executeTask();
    }

    private NewsEvent expiryWarningFor(final BigDecimal playerId, final DateTime expectedExpiryDate) {
        final ParameterisedMessage message = new ParameterisedMessage(
                "Your private %s table has not been used recently and will be removed on %2$td/%2$tm/%2$tY.",
                GAME_TYPE.getName(), expectedExpiryDate.toDate());
        final ParameterisedMessage shortMessage = new ParameterisedMessage(
                "Your private %s table will be removed on %2$td/%2$tm/%2$tY.",
                GAME_TYPE.getName(), expectedExpiryDate.toDate());
        return new NewsEvent.Builder(playerId, message).setType(NewsEventType.NEWS).setShortDescription(shortMessage).setDelay(0).build();
    }

    private TableRequestWrapper closeMessageFor(final BigDecimal tableId) {
        return new TableRequestWrapper(new TableControlMessage(tableId, TableControlMessageType.CLOSE));
    }

    private SQLQuery<Table> tablesUpdatedBefore(final long timestamp) {
        return new SQLQuery<Table>(
                Table.class, "lastUpdated <= ? AND ownerId is NOT null", timestamp);
    }

    private DateTime currentTime() {
        return new DateTime();
    }

    private void executeTask() throws Exception {
        final ArgumentCaptor<PrivateTableExpirer.EventTask> eventTaskCaptor
                = ArgumentCaptor.forClass(PrivateTableExpirer.EventTask.class);
        underTest.initialise();
        verify(scheduledExecutorService).scheduleAtFixedRate(
                eventTaskCaptor.capture(), eq(INITIAL_SCAN), eq(ONE_DAY_IN_MS), eq(MILLISECONDS));
        eventTaskCaptor.getValue().run();
    }

    private Table anExpiredTableWithId(final BigDecimal tableId) {
        final Table table = new Table();
        table.setTableId(tableId);
        table.setGameType(GAME_TYPE);
        table.setLastUpdated(new DateTime().minusDays(DEFAULT_EXPIRY_PERIOD).minusDays(3).getMillis());
        return table;
    }

    private Table aSoonToExpireTableWithId(final BigDecimal tableId) {
        final Table table = new Table();
        table.setTableId(tableId);
        table.setOwnerId(tableId.multiply(BigDecimal.TEN));
        table.setGameType(GAME_TYPE);
        table.setLastUpdated(new DateTime().minusDays(DEFAULT_EXPIRY_PERIOD)
                .plusDays(DEFAULT_NOTIFICATION_PERIOD).minusMinutes(60).getMillis());
        return table;
    }

}
