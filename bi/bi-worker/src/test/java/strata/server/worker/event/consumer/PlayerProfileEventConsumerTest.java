package strata.server.worker.event.consumer;

import com.google.common.collect.Lists;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.Partner;
import com.yazino.platform.event.message.PlayerProfileEvent;
import com.yazino.platform.player.PlayerProfileStatus;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import strata.server.worker.event.consumer.crm.CRMRegistrar;
import strata.server.worker.event.persistence.PostgresPlayerProfileDWDAO;

import java.math.BigDecimal;
import java.util.ArrayList;

import static java.math.BigDecimal.valueOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.Mockito.*;

public class PlayerProfileEventConsumerTest {

    @Mock
    private PostgresPlayerProfileDWDAO postgresDao;
    @Mock
    private YazinoConfiguration configuration;
    @Mock
    private CRMRegistrar crmRegistrar;
    private PlayerProfileEventConsumer underTest;

    @Captor
    private ArgumentCaptor<ArrayList<PlayerProfileEvent>> captor;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        underTest = new PlayerProfileEventConsumer(postgresDao, configuration, crmRegistrar);
    }

    @Test
    public void shouldSaveUsingPostgresDao() {
        when(configuration.getBoolean("data-warehouse.write.enabled")).thenReturn(true);
        final PlayerProfileEvent message = aPlayerProfile(BigDecimal.ONE);
        underTest.handle(message);
        underTest.consumerCommitting();
        verify(postgresDao).saveAll(Lists.newArrayList(message));
    }

    @Test
    public void shouldSaveMultipleMessagesOnCommitUsingPostgresqlDAO() {
        when(configuration.getBoolean("data-warehouse.write.enabled")).thenReturn(true);

        underTest.handle(aPlayerProfile(valueOf(1)));
        underTest.handle(aPlayerProfile(valueOf(2)));
        underTest.handle(aPlayerProfile(valueOf(3)));
        underTest.consumerCommitting();


        verify(postgresDao).saveAll(captor.capture());
        assertThat(captor.getValue(), containsInAnyOrder(aPlayerProfile(valueOf(1)), aPlayerProfile(valueOf(2)), aPlayerProfile(valueOf(3))));
    }

    @Test
    public void shouldFlushMessageOnCommitUsingPostgresqlDAO() {
        when(configuration.getBoolean("data-warehouse.write.enabled")).thenReturn(true);

        underTest.handle(aPlayerProfile(valueOf(1)));
        underTest.handle(aPlayerProfile(valueOf(2)));
        underTest.handle(aPlayerProfile(valueOf(3)));
        underTest.consumerCommitting();

        verify(postgresDao).saveAll(captor.capture());
        assertThat(captor.getValue(), containsInAnyOrder(aPlayerProfile(valueOf(1)), aPlayerProfile(valueOf(2)), aPlayerProfile(valueOf(3))));

        underTest.handle(aPlayerProfile(valueOf(4)));
        underTest.handle(aPlayerProfile(valueOf(5)));
        underTest.consumerCommitting();

        verify(postgresDao, times(2)).saveAll(captor.capture());
        assertThat(captor.getValue(), containsInAnyOrder(aPlayerProfile(valueOf(4)), aPlayerProfile(valueOf(5))));
    }

    @Test
    public void shouldDisableSaveUsingPostgresqlDAO() {
        when(configuration.getBoolean("data-warehouse.write.enabled")).thenReturn(false);
        final PlayerProfileEvent message = aPlayerProfile(BigDecimal.ONE);

        underTest.handle(message);
        underTest.consumerCommitting();

        verifyZeroInteractions(postgresDao);
    }

    @Test
    public void shouldRegisterNewPlayer() {
        final PlayerProfileEvent message = aPlayerProfile(BigDecimal.ONE);
        message.setNewPlayer(true);
        underTest.handle(message);
        verify(crmRegistrar).register(message);
    }

    @Test
    public void shouldNotRegisterExistingPlayer() {
        final PlayerProfileEvent message = aPlayerProfile(BigDecimal.ONE);
        message.setNewPlayer(false);
        underTest.handle(message);
        verifyZeroInteractions(crmRegistrar);
    }

    @Test(expected = Exception.class)
    public void shouldPropagateExceptionsThrownByPostgresDao() {
        when(configuration.getBoolean("data-warehouse.write.enabled")).thenReturn(true);
        doThrow(new RuntimeException("Some Exception")).when(postgresDao).saveAll(Lists.newArrayList(aPlayerProfile(BigDecimal.ONE)));
        final PlayerProfileEvent message = aPlayerProfile(BigDecimal.ONE);
        underTest.handle(message);
        underTest.consumerCommitting();
    }

    @Test
    public void shouldNotPropagateExceptionsThrownDuringRegistration() {
        doThrow(new RuntimeException("Some Exception")).when(crmRegistrar).register(any(PlayerProfileEvent.class));
        final PlayerProfileEvent message = aPlayerProfile(BigDecimal.ONE);
        underTest.handle(message);
    }


    private PlayerProfileEvent aPlayerProfile(final BigDecimal id) {
        return new PlayerProfileEvent(id,
                new DateTime(2011, 11, 11, 11, 11, 11, 0),
                "aPlayer",
                "The Player",
                "Player",
                "pic",
                "email",
                "GB",
                "extId",
                "a-verifcation-identifier",
                "prov",
                PlayerProfileStatus.ACTIVE,
                Partner.YAZINO,
                new DateTime(1980, 11, 11, 0, 0, 0, 0),
                "M",
                "ref",
                null,
                true,
                null,
                "G");
    }

}
