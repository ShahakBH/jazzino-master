package com.yazino.platform.processor.community;

import com.yazino.platform.model.community.TableInvite;
import com.yazino.platform.model.community.TableInvitePersistenceRequest;
import com.yazino.platform.persistence.community.TableInviteDAO;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class TableInvitePersistenceProcessorTest {

    private final TableInvitePersistenceProcessor processor = new TableInvitePersistenceProcessor();
    private final TableInviteDAO dao = mock(TableInviteDAO.class);

    @Before
    public void setup() {
        processor.setTableInviteDAO(dao);
    }


    @Test(expected = NullPointerException.class)
    public void ensureCannotProcessNullRequest() throws Exception {
        processor.processTableInvitePersistenceRequest(null);
    }

    @Test
    public void ensureTableInviteWrittenToDAO() throws Exception {
        BigDecimal playerId = BigDecimal.valueOf(40);
        BigDecimal tableId = BigDecimal.valueOf(24);
        DateTime dateTime = new DateTime();
        TableInvite tableInvite = new TableInvite(playerId, tableId, dateTime);

        TableInvitePersistenceRequest request = new TableInvitePersistenceRequest();
        request.setTableInvite(tableInvite);
        processor.processTableInvitePersistenceRequest(request);
        verify(dao).save(tableInvite);
    }
}
