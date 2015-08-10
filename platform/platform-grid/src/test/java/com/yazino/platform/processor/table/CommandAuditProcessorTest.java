package com.yazino.platform.processor.table;

import com.yazino.game.api.Command;
import com.yazino.game.api.GamePlayer;
import com.yazino.platform.audit.AuditService;
import com.yazino.platform.audit.message.CommandAudit;
import com.yazino.platform.model.table.AuditContext;
import com.yazino.platform.model.table.CommandAuditWrapper;
import com.yazino.platform.repository.community.PlayerRepository;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class CommandAuditProcessorTest {
    public static final BigDecimal PLAYER_ID = BigDecimal.TEN;
    @Mock
    private AuditService auditService;
    @Mock
    private PlayerRepository playerRepository;

    private CommandAuditProcessor underTest;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        underTest = new CommandAuditProcessor(auditService, playerRepository);
    }

    @Test
    public void commandProcessorTestCallsRepository() {
        CommandAuditWrapper wrappedCommand = new CommandAuditWrapper(aCommand(PLAYER_ID, "X"), anAuditContext(new DateTime()));

        underTest.store(new CommandAuditWrapper[]{wrappedCommand});

        verify(auditService).auditCommands(asList(aCommandAuditFor(wrappedCommand)));
    }

    @Test
    public void storeShouldNotAttemptToWriteAnEmptyArrayOfPlayerLastPlayedToTheSpace() {
        DateTime auditTs = new DateTime(2013, 11, 23, 1, 0);

        CommandAuditWrapper wrappedCommandTypeBet = new CommandAuditWrapper(aCommand(PLAYER_ID, "headbutt"), anAuditContext(auditTs));

        underTest.store(new CommandAuditWrapper[]{wrappedCommandTypeBet});
        verifyNoMoreInteractions(playerRepository);
    }


    @Test
    public void storeShouldWritePlayerLastPlayedIntoSpaceIfCommandTypeIsBet() {
        DateTime auditTs = new DateTime(2013, 11, 23, 1, 0);

        PlayerLastPlayedUpdateRequest[] expected = new PlayerLastPlayedUpdateRequest[]{new PlayerLastPlayedUpdateRequest(PLAYER_ID, auditTs)};
        CommandAuditWrapper wrappedCommandTypeBet = new CommandAuditWrapper(aCommand(PLAYER_ID, "Bet"), anAuditContext(auditTs));

        underTest.store(new CommandAuditWrapper[]{wrappedCommandTypeBet});
        verify(playerRepository).requestLastPlayedUpdates(expected);
    }

    @Test
    public void storeShouldOnlyStoreOneBetMessagePerPlayerInTheSpace() {
        DateTime auditTs = new DateTime(2013, 11, 23, 1, 0);
        DateTime auditTsPlus10Seconds = new DateTime(2013, 11, 23, 1, 0).plusSeconds(10);

        CommandAuditWrapper wrappedCommandTypeBet = new CommandAuditWrapper(aCommand(PLAYER_ID, "Bet"), anAuditContext(auditTs));
        CommandAuditWrapper wrappedCommandTypeBet2 = new CommandAuditWrapper(aCommand(PLAYER_ID, "Bet"), anAuditContext(auditTsPlus10Seconds));

        underTest.store(new CommandAuditWrapper[]{wrappedCommandTypeBet, wrappedCommandTypeBet2});

        ArgumentCaptor<PlayerLastPlayedUpdateRequest[]> gigaSpaceArgumentCaptor = ArgumentCaptor.forClass(PlayerLastPlayedUpdateRequest[].class);
        verify(playerRepository).requestLastPlayedUpdates(gigaSpaceArgumentCaptor.capture());

        PlayerLastPlayedUpdateRequest[] playerLastPlayedArray = gigaSpaceArgumentCaptor.getValue();
        assertThat(playerLastPlayedArray.length, is(equalTo(1)));
        PlayerLastPlayedUpdateRequest playerWhoLastPlayed = playerLastPlayedArray[0];

        assertThat(playerWhoLastPlayed.getPlayerId(), is(equalTo(PLAYER_ID)));
        // we cannot guarantee that these are received in correct order but for our purposes the time will be in the same ballpark.
        assertThat(playerWhoLastPlayed.getLastPlayed(), any(DateTime.class));
    }

    private CommandAudit aCommandAuditFor(final CommandAuditWrapper wrappedCommand) {
        return new CommandAudit(
                wrappedCommand.getAuditContext().getLabel(),
                wrappedCommand.getAuditContext().getHostname(),
                wrappedCommand.getAuditContext().getAuditDate(),
                wrappedCommand.getCommand().getTableId(),
                wrappedCommand.getCommand().getGameId(),
                wrappedCommand.getCommand().getType(),
                wrappedCommand.getCommand().getArgs(),
                wrappedCommand.getCommand().getPlayer().getId(),
                wrappedCommand.getCommand().getUuid());
    }

    private Command aCommand(final BigDecimal playerId, final String type) {
        return new Command(new GamePlayer(playerId, null, "y"), BigDecimal.ZERO, 100l, "123", type);
    }

    private AuditContext anAuditContext(DateTime auditTs) {
        return new AuditContext("1", auditTs.toDate(), "2");
    }

}
