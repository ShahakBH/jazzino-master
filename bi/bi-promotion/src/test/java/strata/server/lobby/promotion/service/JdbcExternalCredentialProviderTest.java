package strata.server.lobby.promotion.service;

import com.yazino.platform.player.PlayerProfile;
import com.yazino.platform.player.service.PlayerProfileService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import strata.server.lobby.promotion.domain.ExternalCredentials;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class JdbcExternalCredentialProviderTest {

    public static final BigDecimal PLAYER_ID = BigDecimal.ONE;
    public static final String PROVIDER_NAME = "providerName";
    public static final String EXTERNAL_ID = "externalId";
    public static final String YAZINO_PROVIDER = "yaZIno";

    @Mock
    private PlayerProfileService playerProfileService;

    private ExternalCredentialsProvider underTest;
    
    @Before
    public void setUp() {
        underTest = new JdbcExternalCredentialsProvider(playerProfileService);
    }

    @Test
    public void shouldReturnNullForUnknownPlayer() {
        when(playerProfileService.findByPlayerId(PLAYER_ID)).thenReturn(null);

        assertNull(underTest.lookupByPlayerId(PLAYER_ID));
    }

    @Test
    public void shouldReturnExternalIdForKnownPlayer() {
        // given a known player
        PlayerProfile playerProfile = new PlayerProfile();
        playerProfile.setExternalId(EXTERNAL_ID);
        playerProfile.setProviderName(PROVIDER_NAME);
        when(playerProfileService.findByPlayerId(PLAYER_ID)).thenReturn(playerProfile);
        ExternalCredentials expectedExternalCredentials = new ExternalCredentials(PROVIDER_NAME, EXTERNAL_ID);

        // when requesting the external credentials for an unknown player
        final ExternalCredentials externalCredentials = underTest.lookupByPlayerId(PLAYER_ID);

        // then the players associated external id should be returned
        assertThat(externalCredentials, is(equalTo(expectedExternalCredentials)));
    }

    @Test
    public void shouldReturnUserIdAsExternalIdOfYazinoPlayer() {
        // given a known player
        PlayerProfile playerProfile = new PlayerProfile();
        playerProfile.setPlayerId(PLAYER_ID);
        playerProfile.setProviderName(YAZINO_PROVIDER);
        when(playerProfileService.findByPlayerId(PLAYER_ID)).thenReturn(playerProfile);
        ExternalCredentials expectedExternalCredentials = new ExternalCredentials(YAZINO_PROVIDER, PLAYER_ID.toString());

        // when requesting the external credentials for an unknown player
        final ExternalCredentials externalCredentials = underTest.lookupByPlayerId(PLAYER_ID);

        // then the players associated external id should be returned
        assertThat(externalCredentials, is(equalTo(expectedExternalCredentials)));
    }

}
