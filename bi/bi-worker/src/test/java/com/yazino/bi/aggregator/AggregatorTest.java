package com.yazino.bi.aggregator;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.hamcrest.core.Is;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;

import java.sql.Timestamp;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.joda.time.DateTimeConstants.FRIDAY;
import static org.joda.time.DateTimeConstants.THURSDAY;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.setField;

@RunWith(MockitoJUnitRunner.class)
public class AggregatorTest {

    @Mock private AggregatorLastUpdateDAO aggregatorLastUpdateDAO;
    @Mock private AggregatorLockDao aggregatorLockDao;
    @Mock private YazinoConfiguration configuration;
    @Mock private JdbcTemplate template;

    private Aggregator underTest;
    private final String aggregatorId = "myOwnAggregator";
    private Timestamp currentTimestamp;

    @Before
    public void setUp(){
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(System.currentTimeMillis());
        currentTimestamp = new Timestamp(new DateTime().getMillis());
        underTest= new TestAggregator(template,aggregatorLastUpdateDAO,aggregatorLockDao,aggregatorId,configuration,null);
        setRedshiftEnabled(TRUE);
        setAllAggregatorsEnabledTo(TRUE);
        when(aggregatorLockDao.lock(Mockito.anyString(), Mockito.anyString())).thenReturn(TRUE);

        when(configuration.getProperty("data-warehouse.maintenance.window.start.day")).thenReturn(""+FRIDAY);
        when(configuration.getProperty("data-warehouse.maintenance.window.end.day")).thenReturn(""+FRIDAY);
        when(configuration.getProperty("data-warehouse.maintenance.window.start.time")).thenReturn("6:55");
        when(configuration.getProperty("data-warehouse.maintenance.window.end.time")).thenReturn("7:35");

    }

    @Test
    public void updateWithLocksShouldNotCallDaoLockIfRedshiftTurnedOff(){
        setRedshiftEnabled(FALSE);
        underTest.updateWithLocks(currentTimestamp);
        verifyNoMoreInteractions(aggregatorLockDao);
    }

    private void setRedshiftEnabled(Boolean enabled) {
        when(configuration.getBoolean("data-warehouse.write.enabled")).thenReturn(enabled);
    }

    @Test
    public void updateShouldRunIfNoDependencies(){
        setRedshiftEnabled(TRUE);
        setAllAggregatorsEnabledTo(TRUE);
        doNotDisableThisAggregator();
        underTest.updateWithLocks(currentTimestamp);
        verify(aggregatorLastUpdateDAO).getLastRun(Mockito.anyString());
    }

    @Test
    public void updateShouldNotExecuteIfDependantTestHasNotCompleted(){
        setRedshiftEnabled(TRUE);
        setAllAggregatorsEnabledTo(TRUE);
        doNotDisableThisAggregator();
        setField(underTest, "dependentUpon", "yoMomma");
        when(aggregatorLastUpdateDAO.getLastRun(aggregatorId)).thenReturn(toTs(new DateTime().minusDays(1)));
        when(aggregatorLastUpdateDAO.getLastRun("yoMomma")).thenReturn(toTs(new DateTime().minusDays(1)));

        underTest.updateWithLocks(currentTimestamp);
        verify(aggregatorLastUpdateDAO).getLastRun(aggregatorId);
        verify(aggregatorLastUpdateDAO).getLastRun("yoMomma");
        verifyNoMoreInteractions(aggregatorLastUpdateDAO);
    }

    @Test
    public void updateShouldExecuteIfDependantTestHasCompleted(){
        setRedshiftEnabled(TRUE);
        setAllAggregatorsEnabledTo(TRUE);
        doNotDisableThisAggregator();
        setField(underTest, "dependentUpon", "yoMomma");
        final DateTime now = new DateTime();
        final Timestamp yesterday = toTs(now.minusDays(1));
        when(aggregatorLastUpdateDAO.getLastRun(aggregatorId)).thenReturn(yesterday);
        when(aggregatorLastUpdateDAO.getLastRun("yoMomma")).thenReturn(toTs(now));

        underTest.updateWithLocks(currentTimestamp);
        verify(aggregatorLastUpdateDAO).getLastRun(aggregatorId);
        verify(aggregatorLastUpdateDAO).getLastRun("yoMomma");

        verify(aggregatorLastUpdateDAO,times(2)).updateLastRunFor(aggregatorId, toTs(now));

        verify(template).update(eq("blah"), any(PreparedStatementSetter.class));
    }



    private Timestamp toTs(DateTime dt){
        return new Timestamp(dt.getMillis());
    }
    @Test
    public void updateWithLocksShouldNotCallDaoLockIfAllAggregatorsTurnedOff(){
        setRedshiftEnabled(TRUE);
        setAllAggregatorsEnabledTo(FALSE);
        underTest.updateWithLocks(currentTimestamp);
        verifyNoMoreInteractions(aggregatorLockDao);
    }

    @Test
    public void updateWithLocksShouldNotCallDaoLockIfAllAggregatorsTurnedOffAndSpecificAggregatorNotDisabled(){
        setRedshiftEnabled(TRUE);
        setAllAggregatorsEnabledTo(FALSE);
        doNotDisableThisAggregator();
        underTest.updateWithLocks(currentTimestamp);
        verifyNoMoreInteractions(aggregatorLockDao);
    }

    @Test
    public void updateWithLocksShouldNotCallDaoLockIfAggregatorsTurnedOffAndSpecificAggregatorDisabled(){
        setRedshiftEnabled(TRUE);
        setAllAggregatorsEnabledTo(FALSE);
        disableThisAggregator();
        underTest.updateWithLocks(currentTimestamp);
        verifyNoMoreInteractions(aggregatorLockDao);
    }

    @Test
    public void updateWithLocksShouldCallDaoLockIfAllAggregatorsTurnedOnAndSpecificAggregatorNotDisabled(){
        setRedshiftEnabled(TRUE);
        setAllAggregatorsEnabledTo(TRUE);
        doNotDisableThisAggregator();
        underTest.updateWithLocks(currentTimestamp);
        verify(aggregatorLastUpdateDAO).getLastRun(Mockito.anyString());
    }



    @Test
    public void updateWithLocksShouldNotCallDaoLockIfTurnedOff(){
        setRedshiftEnabled(FALSE);
        underTest.updateWithLocks(currentTimestamp);
        verifyNoMoreInteractions(aggregatorLockDao);
    }

    @Test
    public void updateWithLocksShouldNotCallLastUpdateDaoIfItCannotAcquireLock() {
        setRedshiftEnabled(TRUE);
        when(aggregatorLockDao.lock(Mockito.anyString(), Mockito.anyString())).thenReturn(FALSE);
        underTest.updateWithLocks(currentTimestamp);

        verify(aggregatorLastUpdateDAO, never()).getLastRun(Mockito.anyString());
    }

    @Test
    public void updateWithLocksShouldCallLastUpdateDaoIfRedshiftTurnedOnAndItCanAcquireLock() {
        setRedshiftEnabled(TRUE);
        when(aggregatorLockDao.lock(Mockito.anyString(), Mockito.anyString())).thenReturn(TRUE);

        underTest.updateWithLocks(currentTimestamp);

        verify(aggregatorLastUpdateDAO).getLastRun(Mockito.anyString());
    }

    @Test
     public void updateWithLocksShouldResetLastUpdateDaoIfRedshiftTurnedOnAndItCanAcquireLock() {
         setRedshiftEnabled(TRUE);
         when(aggregatorLockDao.lock(Mockito.anyString(), Mockito.anyString())).thenReturn(TRUE);

         underTest.updateWithLocks(currentTimestamp);

         verify(aggregatorLastUpdateDAO,times(2)).updateLastRunFor(aggregatorId, currentTimestamp);
     }

    @Test
    public void updateWithLocksShouldReleaseLockIfItSuccessfullyCompletesOperation() {
        setRedshiftEnabled(TRUE);
        when(aggregatorLockDao.lock(Mockito.anyString(), Mockito.anyString())).thenReturn(TRUE);

        underTest.updateWithLocks(currentTimestamp);
        verify(aggregatorLockDao).unlock(Mockito.anyString(), Mockito.anyString());

    }

    @Test
    public void updateWithLocksShouldReleaseLockIfLastUpdateDaoThrowsException() {
        setRedshiftEnabled(TRUE);
        when(aggregatorLockDao.lock(Mockito.anyString(), Mockito.anyString())).thenReturn(TRUE);
        when(aggregatorLastUpdateDAO.getLastRun(Mockito.anyString())).thenThrow(new RuntimeException());

        underTest.updateWithLocks(currentTimestamp);
        verify(aggregatorLockDao).unlock(Mockito.anyString(), Mockito.anyString());

    }

    @Test
    public void dependantHasCompletedShouldBeValidForRanges(){
        //run for now. dep should be set to 12:10
        assertThat(underTest.dependantHasCompleted(DateTime.now(),DateTime.now()),is(true));
        assertThat(underTest.dependantHasCompleted(DateTime.now(),DateTime.now().minusDays(1)),is(true));
        assertThat(underTest.dependantHasCompleted(DateTime.now().minusDays(1),DateTime.now().minusDays(1)),is(true));
        assertThat(underTest.dependantHasCompleted(DateTime.now().minusDays(1),DateTime.now()),is(false));
        assertThat(underTest.dependantHasCompleted(new DateTime().withTime(0,10,0,0),new DateTime().withTime(0,0,0,0)),is(true));


    }

    private void setAllAggregatorsEnabledTo(Boolean enabled) {
        when(configuration.getBoolean("data-warehouse.aggregators.enabled")).thenReturn(enabled);
    }

    private void disableThisAggregator() {
        when(configuration.getBoolean("data-warehouse.aggregators." + aggregatorId + ".disabled")).thenReturn(true);
    }

    private void doNotDisableThisAggregator() {
        when(configuration.getBoolean("data-warehouse.aggregators." + aggregatorId + ".disabled")).thenReturn(FALSE);
    }

    class TestAggregator extends Aggregator{

        TestAggregator(final JdbcTemplate template,
                       final AggregatorLastUpdateDAO aggregatorLastUpdateDAO,
                       final AggregatorLockDao aggregatorLockDao,
                       final String aggregatorId, final YazinoConfiguration configuration, final String dependentUpon) {
            super(template, aggregatorLastUpdateDAO, aggregatorLockDao, aggregatorId, configuration, dependentUpon);
        }

        @Override
        protected PreparedStatementSetter getPreparedStatementSetter(final DateTime... runDay) {
            return null;
        }

        @Override
        protected void update() {}

        @Override
        public Timestamp materializeData(final Timestamp timeLastRun, final Timestamp toDate) {
            Timestamp startOfToDate = new Timestamp(new DateTime(toDate).getMillis());
            DateTime fromDate = new DateTime(toDate).minusDays(1);

            return executingQueryEveryDayForDateRange(startOfToDate, fromDate, true, "blah");
        }
    }


}
