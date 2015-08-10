package com.yazino.host.table;

import com.yazino.host.TableRequestWrapperQueue;
import com.yazino.platform.model.table.CommandWrapper;
import com.yazino.platform.model.table.TableRequestWrapper;
import com.yazino.platform.table.Command;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class StandaloneTableServiceTest {
    private TableRequestWrapperQueue requestQueue;
    private StandaloneTableService underTest;

    @Before
    public void setUp() {
        requestQueue = mock(TableRequestWrapperQueue.class);
        underTest = new StandaloneTableService(requestQueue);
    }

    @Test
    public void shouldSendCommand() {
        final Command command = new Command(BigDecimal.ONE, 1l, BigDecimal.TEN, null, "Cmd", "arg1", "arg2");
        underTest.sendCommand(command);
        final ArgumentCaptor<TableRequestWrapper> requestCaptor = ArgumentCaptor.forClass(TableRequestWrapper.class);
        verify(requestQueue).addRequest(requestCaptor.capture());
        final TableRequestWrapper expected = new TableRequestWrapper(new CommandWrapper(command));
        final TableRequestWrapper actual = requestCaptor.getValue();
        expected.setRequestID(actual.getRequestID());
        assertEquals(expected, actual);
    }
}
