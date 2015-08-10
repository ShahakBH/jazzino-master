package com.yazino.platform.processor.table.handler;

import com.yazino.platform.gamehost.GameHost;
import com.yazino.platform.model.table.CommandWrapper;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.model.table.TableRequestType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class TableCommandHandlerTest {
    private static final BigDecimal PLAYER_ID = BigDecimal.TEN;
    private static final Long GAME_ID = 5L;
    private static final BigDecimal SESSION_ID = BigDecimal.valueOf(1234);
    private static final BigDecimal TABLE_ID = BigDecimal.ONE;
    private static final String COMMAND_TYPE = "aCommandType";
    private static final String ARGUMENTS = "anArgument";
    private static final String REQUEST_ID = "aRequestId";

    @Mock
    private GameHost gameHost;

    private final Table table = new Table();

    private final CommandWrapper wrapper = new CommandWrapper(
            TABLE_ID, GAME_ID, PLAYER_ID, SESSION_ID, COMMAND_TYPE, ARGUMENTS);

    private TableCommandHandler unit;

    @Before
    public void setUp() {
        wrapper.setRequestId(REQUEST_ID);
        wrapper.setTimestamp(new Date());

        unit = new TableCommandHandler();
    }

    @Test
    public void commandShouldBePassedToTheGameHost() {
        unit.execute(wrapper, gameHost, table);

        verify(gameHost).execute(table, wrapper);
    }

    @Test
    public void commandsShouldBeExecuted() {
        assertThat(unit.accepts(TableRequestType.COMMAND), is(true));
    }

}
