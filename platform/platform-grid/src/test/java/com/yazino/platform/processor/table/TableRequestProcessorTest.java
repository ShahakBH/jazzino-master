package com.yazino.platform.processor.table;


import com.yazino.platform.model.table.TableRequest;
import com.yazino.platform.model.table.TableRequestType;
import com.yazino.platform.model.table.TableRequestWrapper;
import com.yazino.platform.processor.table.handler.TableRequestHandler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TableRequestProcessorTest {
    @Mock
    private TableRequestHandler commandHandler;

    @Mock
    private TableRequestHandler processHandler;

    private TableRequestProcessor unit;

    @Before
    public void setUp() throws Exception {
        when(commandHandler.accepts(TableRequestType.COMMAND)).thenReturn(true);
        when(processHandler.accepts(TableRequestType.PROCESS)).thenReturn(true);

        unit = new TableRequestProcessor(asList(commandHandler, processHandler));
    }

    @Test(expected = NullPointerException.class)
    public void unitRejectsNullHandlerList() {
        new TableRequestProcessor(null);
    }

    @Test
    public void nullRequestAreIgnored() {
        unit.process(null);

        verifyZeroInteractions(commandHandler);
        verifyZeroInteractions(processHandler);
    }

    @Test
    public void unhandledRequestTypesAreIgnored() {
        unit.process(wrap(new TestTableRequest(TableRequestType.UNKNOWN)));
    }

    @Test
    public void requestTypesAreCheckedAgainstAllHandlersWhenNotAccepted() {
        unit.process(wrap(request(TableRequestType.UNKNOWN)));


        verify(commandHandler).accepts(TableRequestType.UNKNOWN);
        verify(processHandler).accepts(TableRequestType.UNKNOWN);

        verifyNoMoreInteractions(commandHandler);
        verifyNoMoreInteractions(processHandler);
    }

    @SuppressWarnings({"unchecked"})
    @Test
    public void requestIsRoutedToTheCorrectHandler() {
        final TableRequest request = request(TableRequestType.PROCESS);
        unit.process(wrap(request));


        verify(processHandler).accepts(TableRequestType.PROCESS);
        verify(processHandler).handle(request);

        verify(commandHandler).accepts(TableRequestType.PROCESS);
        verifyNoMoreInteractions(commandHandler);
    }

    @SuppressWarnings({"unchecked", "ThrowableInstanceNeverThrown"})
    @Test
    public void handlerErrorsAreNotPropagated() {
        final TableRequest request = request(TableRequestType.PROCESS);

        doThrow(new RuntimeException("anException")).when(processHandler).handle(request);

        unit.process(wrap(request));
    }

    @Test
    public void processorTemplateIsEmpty() {
        assertThat(unit.eventTemplate().getRequestType(), is(nullValue()));
        assertThat(unit.eventTemplate().getTableRequest(), is(nullValue()));
        assertThat(unit.eventTemplate().getRequestID(), is(nullValue()));
        assertThat(unit.eventTemplate().getSelector(), is(nullValue()));
        assertThat(unit.eventTemplate().getTableId(), is(nullValue()));
    }

    private TableRequest request(final TableRequestType type) {
        return new TestTableRequest(type);
    }

    private TableRequestWrapper wrap(final TableRequest request) {
        return new TableRequestWrapper(request);
    }

    private static class TestTableRequest implements TableRequest {
        private final TableRequestType requestType;

        public TestTableRequest(final TableRequestType requestType) {
            this.requestType = requestType;
        }

        @Override
        public BigDecimal getTableId() {
            return BigDecimal.ZERO;
        }


        @Override
        public TableRequestType getRequestType() {
            return requestType;
        }
    }

}
