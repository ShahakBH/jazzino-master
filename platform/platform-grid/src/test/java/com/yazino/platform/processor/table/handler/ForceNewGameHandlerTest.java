package com.yazino.platform.processor.table.handler;

import com.yazino.platform.gamehost.GameHost;
import com.yazino.platform.messaging.host.HostDocument;
import com.yazino.platform.model.table.Client;
import com.yazino.platform.model.table.ForceNewGameRequest;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.repository.table.ClientRepository;
import com.yazino.platform.repository.table.GameVariationRepository;
import com.yazino.platform.table.GameVariation;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import com.yazino.game.api.GamePlayer;
import com.yazino.game.api.PlayerAtTableInformation;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ForceNewGameHandlerTest {
    private static final BigDecimal TABLE_ID = BigDecimal.valueOf(17);
    private static final BigDecimal VARIATION_TEMPLATE_ID = BigDecimal.valueOf(18);
    private static final String CLIENT_ID = "TEST";

    @Mock
    private GameHost gameHost;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private GameVariationRepository gameTemplateRepository;

    private ForceNewGameHandler underTest;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        underTest = new ForceNewGameHandler(clientRepository, gameTemplateRepository);
        underTest.setGameHost(gameHost);
    }

    @Test
    public void ForceNewGameRequest_updates_table_client_and_variation_and_executes_gameHost_force_new_game() {
        final Client client = new Client(CLIENT_ID);
        final Map<BigDecimal, BigDecimal> overriddenAccounts = new HashMap<BigDecimal, BigDecimal>();
        final Map<String, String> variation = new HashMap<String, String>();
        variation.put("ONE", "TWO");
        final Table table = new Table();
        table.setTableId(TABLE_ID);
        final Collection<PlayerAtTableInformation> playerAtTableInformationCollection = Arrays.asList(
                new PlayerAtTableInformation(new GamePlayer(BigDecimal.ONE, null, "Mike"), Collections.<String, String>emptyMap()));
        final ForceNewGameRequest request = new ForceNewGameRequest(TABLE_ID, playerAtTableInformationCollection, VARIATION_TEMPLATE_ID, CLIENT_ID, overriddenAccounts);

        when(gameTemplateRepository.findById(VARIATION_TEMPLATE_ID)).thenReturn(
                new GameVariation(VARIATION_TEMPLATE_ID, "BLACKJACK", "aTemplate", variation));
        when(clientRepository.findById(CLIENT_ID)).thenReturn(client);
        when(gameHost.forceNewGame(table, playerAtTableInformationCollection, overriddenAccounts))
                .thenReturn(Collections.<HostDocument>emptyList());

        underTest.execute(request, gameHost, table);

        verify(gameHost).forceNewGame(table, playerAtTableInformationCollection, overriddenAccounts);

        assertEquals(variation, table.getVariationProperties());
        assertEquals(VARIATION_TEMPLATE_ID, table.getTemplateId());
        assertEquals(client, table.getClient());
        assertEquals(CLIENT_ID, table.getClientId());
    }
}
