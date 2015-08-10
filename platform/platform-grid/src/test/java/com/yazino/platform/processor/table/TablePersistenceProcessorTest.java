package com.yazino.platform.processor.table;

import com.mysql.jdbc.exceptions.jdbc4.MySQLTransactionRollbackException;
import com.yazino.platform.event.message.TableEvent;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.model.table.TablePersistenceRequest;
import com.yazino.platform.persistence.table.TableDAO;
import com.yazino.platform.repository.table.TableRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openspaces.core.GigaSpace;
import org.springframework.dao.DeadlockLoserDataAccessException;
import com.yazino.game.api.GameType;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TablePersistenceProcessorTest {
    private static final BigDecimal TABLE_ID = BigDecimal.valueOf(101);

    @Mock
    private TableDAO tableDAO;
    @Mock
    private TableRepository tableRepository;
    @Mock
    private QueuePublishingService<TableEvent> eventService;
    @Mock
    private GigaSpace gigaSpace;

    private TablePersistenceProcessor underTest;

    @Before
    public void setUp() {
        underTest = new TablePersistenceProcessor(tableDAO, tableRepository, gigaSpace, eventService);

        when(tableRepository.findById(TABLE_ID)).thenReturn(aTable());
    }

    @Test
    public void whenATableIsPresentTheTableIsSavedToTheDAO() {
        underTest.persist(aTableRequest());

        verify(tableDAO).save(aTable());
    }

    @Test
    public void whenATableIsPresentInTheDataStoreThenNoEventIsGenerated() {
        when(tableDAO.save(aTable())).thenReturn(false);

        underTest.persist(aTableRequest());

        verifyZeroInteractions(eventService);
    }

    @Test
    public void whenATableIsNotPresentInTheDataStoreThenAnEventIsGenerated() {
        when(tableDAO.save(aTable())).thenReturn(true);

        underTest.persist(aTableRequest());

        verify(eventService).send(aTableEvent());
    }

    @Test
    public void whenATableIsPresentAllMatchingRequestsAreRemoved() {
        underTest.persist(aTableRequest());

        verify(gigaSpace).takeMultiple(aTableRequest(), Integer.MAX_VALUE);
        verify(gigaSpace).takeMultiple(aTableRequestInErrorState(), Integer.MAX_VALUE);
    }

    @Test
    public void whenATableIsPresentThenNullIsReturned() {
        TablePersistenceRequest response = underTest.persist(aTableRequest());

        assertNull(response);
    }

    @Test
    public void whenNoTableIsThenNullIsReturned() {
        reset(tableRepository);
        when(tableRepository.findById(TABLE_ID)).thenReturn(null);

        TablePersistenceRequest response = underTest.persist(aTableRequest());

        assertNull(response);
    }

    @Test
    public void whenNoTableIsFoundThenMatchingRequestsAreRemoved() {
        reset(tableRepository);
        when(tableRepository.findById(TABLE_ID)).thenReturn(null);

        underTest.persist(aTableRequest());

        verify(gigaSpace).takeMultiple(aTableRequest(), Integer.MAX_VALUE);
        verify(gigaSpace).takeMultiple(aTableRequestInErrorState(), Integer.MAX_VALUE);
    }

    @Test
    public void exceptionWhenSavingStateCausesRequestToBeReturnedInErrorState() {
        when(tableDAO.save(aTable())).thenThrow(new RuntimeException("foo"));

        TablePersistenceRequest response = underTest.persist(aTableRequest());

        assertEquals(aTableRequestInErrorState(), response);
    }

    @Test
    public void deadlockLoserExceptionWhenSavingStateCausesRequestToBeReturnedToTheSpaceForARetry() {
        when(tableDAO.save(aTable())).thenThrow(
                new DeadlockLoserDataAccessException("aDeadlockLoserException", new MySQLTransactionRollbackException()));

        TablePersistenceRequest response = underTest.persist(aTableRequest());

        assertEquals(aTableRequest(), response);
    }

    @Test
    public void exceptionWhenGettingTableCausesRequestToBeReturnedInErrorStateAndNoSaveAttempted() {
        reset(tableRepository);
        when(tableRepository.findById(TABLE_ID)).thenThrow(new RuntimeException("foo"));

        TablePersistenceRequest response = underTest.persist(aTableRequest());

        assertEquals(aTableRequestInErrorState(), response);
    }

    private TablePersistenceRequest aTableRequest() {
        return new TablePersistenceRequest(TABLE_ID);
    }

    private TablePersistenceRequest aTableRequestInErrorState() {
        final TablePersistenceRequest request = new TablePersistenceRequest(TABLE_ID);
        request.setStatus(TablePersistenceRequest.STATUS_ERROR);
        return request;
    }

    private TableEvent aTableEvent() {
        return new TableEvent(TABLE_ID, "aGameType", BigDecimal.TEN, "templateName");
    }

    private Table aTable() {
        final Table table = new Table();
        table.setTableId(TABLE_ID);
        table.setGameType(new GameType("aGameType", "aGameTypeName", Collections.<String>emptySet()));
        table.setTemplateId(BigDecimal.TEN);
        table.setTemplateName("templateName");
        return table;
    }
}
