package strata.server.worker.event.consumer;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.event.message.PlayerReferrerEvent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import strata.server.worker.event.persistence.PostgresPlayerReferrerDWDAO;

import java.math.BigDecimal;

import static com.google.common.collect.Sets.newHashSet;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PlayerReferrerEventConsumerTest {
    @Mock
    private YazinoConfiguration configuration;
    @Mock
    private PostgresPlayerReferrerDWDAO externalDao;

    private PlayerReferrerEventConsumer underTest;

    @Before
    public void setup() {
        underTest = new PlayerReferrerEventConsumer(configuration, externalDao);
    }

    @Test
    public void shouldSaveUsingExternalDaoWhenEnabled() {
        when(configuration.getBoolean("data-warehouse.write.enabled")).thenReturn(true);

        underTest.handle(aPlayerReferrerEvent(BigDecimal.ONE));
        underTest.consumerCommitting();

        verify(externalDao).save(newHashSet(aPlayerReferrerEvent(BigDecimal.ONE)));
    }

    @Test
    public void shouldSaveMultipleEventsUsingExternalDaoWhenEnabled() {
        when(configuration.getBoolean("data-warehouse.write.enabled")).thenReturn(true);

        underTest.handle(aPlayerReferrerEvent(BigDecimal.ONE));
        underTest.handle(aPlayerReferrerEvent(BigDecimal.TEN));
        underTest.consumerCommitting();

        verify(externalDao).save(newHashSet(aPlayerReferrerEvent(BigDecimal.ONE), aPlayerReferrerEvent(BigDecimal.TEN)));
    }

    @Test
    public void shouldMergeMultipleEventsForTheSamePlayerOnSave() {
        when(configuration.getBoolean("data-warehouse.write.enabled")).thenReturn(true);

        underTest.handle(new PlayerReferrerEvent(BigDecimal.ONE, "aRef", null, null));
        underTest.handle(new PlayerReferrerEvent(BigDecimal.ONE, null, "aPlatform", null));
        underTest.handle(new PlayerReferrerEvent(BigDecimal.ONE, null, null, "aGameType"));
        underTest.consumerCommitting();

        verify(externalDao).save(newHashSet(new PlayerReferrerEvent(BigDecimal.ONE, "aRef", "aPlatform", "aGameType")));
    }

    @Test
    public void shouldNotOverwriteReferrerWhenMergingMultipleEventsForTheSamePlayerOnSaveAndReferrerIsInvite() {
        when(configuration.getBoolean("data-warehouse.write.enabled")).thenReturn(true);

        underTest.handle(new PlayerReferrerEvent(BigDecimal.ONE, "aRef", null, null));
        underTest.handle(new PlayerReferrerEvent(BigDecimal.ONE, null, "aPlatform", null));
        underTest.handle(new PlayerReferrerEvent(BigDecimal.ONE, "INVITE", null, null));
        underTest.consumerCommitting();

        verify(externalDao).save(newHashSet(new PlayerReferrerEvent(BigDecimal.ONE, "aRef", "aPlatform", null)));
    }

    @Test
    public void shouldOverwriteReferrerWhenMergingMultipleEventsForTheSamePlayerOnSaveAndReferrerIsNotInvite() {
        when(configuration.getBoolean("data-warehouse.write.enabled")).thenReturn(true);

        underTest.handle(new PlayerReferrerEvent(BigDecimal.ONE, "aRef", null, null));
        underTest.handle(new PlayerReferrerEvent(BigDecimal.ONE, null, "aPlatform", null));
        underTest.handle(new PlayerReferrerEvent(BigDecimal.ONE, "aNewRef", null, null));
        underTest.consumerCommitting();

        verify(externalDao).save(newHashSet(new PlayerReferrerEvent(BigDecimal.ONE, "aNewRef", "aPlatform", null)));
    }

    @Test
    public void shouldAlwaysOverwriteGameTypeWhenMergingMultipleEventsForTheSamePlayerOnSave() {
        when(configuration.getBoolean("data-warehouse.write.enabled")).thenReturn(true);

        underTest.handle(new PlayerReferrerEvent(BigDecimal.ONE, "aRef", null, null));
        underTest.handle(new PlayerReferrerEvent(BigDecimal.ONE, null, null, "aGameType"));
        underTest.handle(new PlayerReferrerEvent(BigDecimal.ONE, null, null, "aNewGameType"));
        underTest.consumerCommitting();

        verify(externalDao).save(newHashSet(new PlayerReferrerEvent(BigDecimal.ONE, "aRef", null, "aNewGameType")));
    }

    @Test
    public void shouldAlwaysOverwritePlatformWhenMergingMultipleEventsForTheSamePlayerOnSave() {
        when(configuration.getBoolean("data-warehouse.write.enabled")).thenReturn(true);

        underTest.handle(new PlayerReferrerEvent(BigDecimal.ONE, "aRef", null, null));
        underTest.handle(new PlayerReferrerEvent(BigDecimal.ONE, null, "aPlatform", null));
        underTest.handle(new PlayerReferrerEvent(BigDecimal.ONE, null, "aNewPlatform", null));
        underTest.consumerCommitting();

        verify(externalDao).save(newHashSet(new PlayerReferrerEvent(BigDecimal.ONE, "aRef", "aNewPlatform", null)));
    }

    @Test
    public void shouldFlushBatchOnCommit() {
        when(configuration.getBoolean("data-warehouse.write.enabled")).thenReturn(true);

        underTest.handle(aPlayerReferrerEvent(BigDecimal.valueOf(1)));
        underTest.handle(aPlayerReferrerEvent(BigDecimal.valueOf(2)));
        underTest.consumerCommitting();
        underTest.handle(aPlayerReferrerEvent(BigDecimal.valueOf(3)));
        underTest.handle(aPlayerReferrerEvent(BigDecimal.valueOf(4)));
        underTest.consumerCommitting();

        verify(externalDao).save(newHashSet(aPlayerReferrerEvent(BigDecimal.valueOf(1)), aPlayerReferrerEvent(BigDecimal.valueOf(2))));
        verify(externalDao).save(newHashSet(aPlayerReferrerEvent(BigDecimal.valueOf(3)), aPlayerReferrerEvent(BigDecimal.valueOf(4))));
    }

    @Test
    public void shouldNotSaveUsingExternalDaoWhenDisabled() {
        underTest.handle(aPlayerReferrerEvent(BigDecimal.ONE));
        underTest.consumerCommitting();

        verifyZeroInteractions(externalDao);
    }

    private PlayerReferrerEvent aPlayerReferrerEvent(final BigDecimal id) {
        return new PlayerReferrerEvent(id, "ref", "platform", "gameType");
    }
}
