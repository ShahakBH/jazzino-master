package strata.server.worker.audit.consumer;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.engagement.campaign.dao.FacebookExclusionsDao;
import com.yazino.platform.audit.message.SessionKey;
import com.yazino.platform.audit.message.SessionKeyMessage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import strata.server.worker.audit.persistence.PostgresClientContextDAO;
import strata.server.worker.audit.persistence.PostgresSessionKeyDAO;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SessionKeyMessageConsumerTest {
    @Mock
    private PostgresSessionKeyDAO postgresSessionKeyDAO;
    @Mock
    private YazinoConfiguration configuration;
    @Mock
    private PostgresClientContextDAO postgresClientContextDAO;

    @Captor
    private ArgumentCaptor<ArrayList<SessionKey>> captor;

    private SessionKeyMessageConsumer underTest;

    @Mock
    private FacebookExclusionsDao facebookExclusionDao;

    @Before
    public void setUp() {
        underTest = new SessionKeyMessageConsumer(configuration, postgresSessionKeyDAO, postgresClientContextDAO, facebookExclusionDao);
    }

    @Test
    public void facebookExclusionsAreResetForAPlayerWhoHasLoggedIn(){
        when(configuration.getBoolean("data-warehouse.write.enabled")).thenReturn(true);

        underTest.handle(aMessage(1));
        underTest.consumerCommitting();

        verify(facebookExclusionDao).resetFacebookExclusions(asList(aSessionKey(1)));
    }

    @Test
    public void aSessionKeyIsSavedToTheExternalDWWhenEnabled() {
        when(configuration.getBoolean("data-warehouse.write.enabled")).thenReturn(true);

        underTest.handle(aMessage(1));
        underTest.consumerCommitting();

        verify(postgresSessionKeyDAO).saveAll(asList(aSessionKey(1)));
        verify(postgresClientContextDAO).saveAll(asList(aSessionKey(1)));
    }

    @Test
    public void multipleSessionKeysAreSavedToTheExternalDWWhenEnabled() {
        when(configuration.getBoolean("data-warehouse.write.enabled")).thenReturn(true);

        underTest.handle(aMessage(1));
        underTest.handle(aMessage(2));
        underTest.handle(aMessage(3));
        underTest.consumerCommitting();

        verify(postgresSessionKeyDAO).saveAll(captor.capture());
        verify(postgresClientContextDAO).saveAll(captor.capture());

        List<ArrayList<SessionKey>> allValues = captor.getAllValues();
        assertThat(allValues.get(0), hasItems(aSessionKey(1), aSessionKey(2), aSessionKey(3)));
        assertThat(allValues.get(1), hasItems(aSessionKey(1), aSessionKey(2), aSessionKey(3)));
    }

    @Test
    public void theExternalBatchIsFlushedOnCommit() {
        when(configuration.getBoolean("data-warehouse.write.enabled")).thenReturn(true);

        underTest.handle(aMessage(1));
        underTest.handle(aMessage(2));
        underTest.consumerCommitting();
        underTest.handle(aMessage(3));
        underTest.handle(aMessage(4));
        underTest.consumerCommitting();

        verify(postgresSessionKeyDAO, times(2)).saveAll(captor.capture());
        verify(postgresClientContextDAO, times(2)).saveAll(captor.capture());

        List<ArrayList<SessionKey>> allValues = captor.getAllValues();

        assertThat(allValues.get(0), hasItems(aSessionKey(1), aSessionKey(2)));
        assertThat(allValues.get(1), hasItems(aSessionKey(3), aSessionKey(4)));
        assertThat(allValues.get(2), hasItems(aSessionKey(1), aSessionKey(2)));
        assertThat(allValues.get(3), hasItems(aSessionKey(3), aSessionKey(4)));
    }

    @Test
    public void aSessionKeyIsNotSavedToTheExternalDWWhenDisabled() {
        underTest.handle(aMessage(1));
        underTest.consumerCommitting();

        verifyZeroInteractions(postgresSessionKeyDAO);
        verifyZeroInteractions(postgresClientContextDAO);
    }

    private SessionKeyMessage aMessage(final int playerId) {
        final SessionKeyMessage message = new SessionKeyMessage();
        message.setSessionKey(aSessionKey(playerId));
        return message;
    }

    private SessionKey aSessionKey(final int playerId) {
        final SessionKey sessionKey = new SessionKey();
        sessionKey.setPlayerId(BigDecimal.valueOf(playerId));
        sessionKey.setSessionId(BigDecimal.valueOf(playerId));
        return sessionKey;
    }
}
