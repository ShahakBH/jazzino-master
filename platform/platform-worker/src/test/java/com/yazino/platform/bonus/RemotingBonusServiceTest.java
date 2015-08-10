package com.yazino.platform.bonus;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.bonus.persistence.BonusDao;
import com.yazino.platform.community.PlayerService;
import com.yazino.platform.event.message.BonusCollectedEvent;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;

import static com.yazino.platform.bonus.RemotingBonusService.AMOUNT;
import static com.yazino.platform.bonus.RemotingBonusService.LOCKOUT;
import static java.math.BigDecimal.valueOf;
import static org.hamcrest.Matchers.equalTo;
import static org.joda.time.DateTime.now;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RemotingBonusServiceTest {
    public static final BigDecimal PLAY_ERID = BigDecimal.ONE;
    public static final BigDecimal SESSION_ID = valueOf(666);
    public static final long MINUTES = 60 * 1000L;
    public static DateTime LOCKOUT_EXPIRY;
    private RemotingBonusService underTest;

    @Mock
    private PlayerService playerService;
    @Mock
    private BonusDao bonusDao;
    @Mock
    private YazinoConfiguration configuration;
    @Mock
    private QueuePublishingService<BonusCollectedEvent> bonusEventService;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this.getClass());
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(1000000000);
        LOCKOUT_EXPIRY = now().minusMinutes(10);
        underTest = new RemotingBonusService(bonusDao, playerService, configuration, bonusEventService);
    }

    @After
    public void tearDown() throws Exception {
        MockitoAnnotations.initMocks(this);
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();

    }

    @Test
    public void getBonusStatusShouldReturnTimeToLockoutExpiry() {
        when(configuration.getLong(AMOUNT, 2000l)).thenReturn(100l);
        when(configuration.getInt(LOCKOUT, 4 * 60)).thenReturn(60);//60 minutes

        when(bonusDao.getLastBonusTime(PLAY_ERID)).thenReturn(LOCKOUT_EXPIRY);//10 minutes ago

        assertThat(underTest.getBonusStatus(PLAY_ERID), equalTo(new BonusStatus(50 * MINUTES, 100l)));//50minutes
    }

    @Test
    public void getBonusStatusShouldReturnBonusWithDefaulted() {
        when(configuration.getLong(AMOUNT, 2000l)).thenReturn(100l);
        when(bonusDao.getLastBonusTime(PLAY_ERID)).thenReturn(null);
        assertThat(underTest.getBonusStatus(PLAY_ERID), equalTo(new BonusStatus(0l, 100l)));
    }

    @Test
    public void collectBonusShouldSetLockout() throws BonusException {
        when(configuration.getInt(LOCKOUT, 4 * 60)).thenReturn(60);//60 mins
        when(bonusDao.getLastBonusTime(PLAY_ERID)).thenReturn(now().minusMinutes(70));//

        underTest.collectBonus(PLAY_ERID, null);

        verify(bonusDao).setLockoutTime(PLAY_ERID, now());
    }

    @Test
    public void collectBonusShouldReturnNewLockoutOnSuccess() throws BonusException {
        when(configuration.getInt(LOCKOUT, 4 * 60)).thenReturn(60 * 4);//4 hours
        when(configuration.getLong(AMOUNT, 2000l)).thenReturn(100l);
        assertThat(underTest.collectBonus(PLAY_ERID, null), equalTo(new BonusStatus(4 * 60 * MINUTES, 100l)));

    }

    @Test
    public void collectBonusShouldCheckThatCollectionIsValid() throws BonusException {

        when(bonusDao.getLastBonusTime(PLAY_ERID)).thenReturn(now().minusMinutes(5));
        underTest.collectBonus(PLAY_ERID, null);
    }

    @Test
    public void invalidCollectionShouldReturnNull() throws BonusException {
        final DateTime expiry = now().plusMinutes(5);
        try {
            when(bonusDao.getLastBonusTime(PLAY_ERID)).thenReturn(expiry);
        } catch (Exception e) {
            assertThat(e.getMessage(), equalTo("Lockout has not expired:" + expiry));
        }
    }

    @Test
    public void walletExceptionShouldReturnException() throws WalletServiceException {
        when(configuration.getInt(LOCKOUT, 4 * 60)).thenReturn(60 * 4);//4 hours
        when(configuration.getLong(AMOUNT, 2000l)).thenReturn(100l);
        when(playerService.postTransaction(PLAY_ERID, SESSION_ID, valueOf(100), "LockoutBonus", "LockoutBonus")).thenThrow(new RuntimeException("no db"));

        try {
            underTest.collectBonus(PLAY_ERID, SESSION_ID);
        } catch (BonusException e) {
            assertThat(e.getMessage(), equalTo("Internal Wallet error: error trying to give player " + PLAY_ERID + " with " + 100 + " chips for Bonus "));
            assertThat((RuntimeException) e.getCause(), equalTo(new RuntimeException("no db")));
        }
    }

    @Test
    public void collectBonusShouldGiveChips() throws WalletServiceException, BonusException {
        when(configuration.getInt(LOCKOUT, 4 * 60)).thenReturn(60 * 4);//4 hours
        when(configuration.getLong(AMOUNT, 2000l)).thenReturn(100l);
        when(bonusDao.getLastBonusTime(PLAY_ERID)).thenReturn(now().minusDays(1));
        assertThat(underTest.collectBonus(PLAY_ERID, SESSION_ID), equalTo(new BonusStatus(4 * 60 * MINUTES, 100l)));
        verify(playerService).postTransaction(PLAY_ERID, SESSION_ID, valueOf(100), "LockoutBonus", "LockoutBonus 100");
    }

    @Test
    public void collectBonusShouldSendBonusCollectedEvent() throws BonusException {
        when(configuration.getInt(LOCKOUT, 4 * 60)).thenReturn(60 * 4);//4 hours
        when(configuration.getLong(AMOUNT, 2000l)).thenReturn(100l);
        when(bonusDao.getLastBonusTime(PLAY_ERID)).thenReturn(now().minusDays(1));
        underTest.collectBonus(PLAY_ERID, SESSION_ID);
        ArgumentCaptor<BonusCollectedEvent> captor = ArgumentCaptor.forClass(BonusCollectedEvent.class);
        verify(bonusEventService).send(captor.capture());
        assertThat(captor.getValue().getCollected(), equalTo(now()));
        assertThat(captor.getValue().getPlayerId(), equalTo(PLAY_ERID));
    }
}
