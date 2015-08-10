package com.yazino.platform.repository.community;

import com.yazino.platform.model.community.SystemMessage;
import com.yazino.platform.persistence.community.SystemMessageDAO;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openspaces.core.GigaSpace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TransactionConfiguration(defaultRollback = true)
public class GigaspaceSystemMessageRepositoryIntegrationTest {
    private SystemMessage message1;
    private SystemMessage message2;
    private SystemMessage invalidMessage;

    @Autowired(required = true)
    private GigaSpace gigaSpace;

    private SystemMessageDAO systemMessageDAO;
    private GigaspaceSystemMessageRepository underTest;

    @Before
    @Transactional
    public void setUp() {
        final Calendar startDateCal = Calendar.getInstance();
        startDateCal.set(Calendar.MILLISECOND, 0);
        startDateCal.add(Calendar.DAY_OF_MONTH, -1);
        final Calendar endDateCal = Calendar.getInstance();
        endDateCal.set(Calendar.MILLISECOND, 0);
        endDateCal.add(Calendar.DAY_OF_MONTH, 3);

        message1 = new SystemMessage(BigDecimal.valueOf(1), "Test message 1", startDateCal.getTime(), endDateCal.getTime());

        startDateCal.add(Calendar.DAY_OF_MONTH, -2);
        endDateCal.add(Calendar.DAY_OF_MONTH, 7);
        message2 = new SystemMessage(BigDecimal.valueOf(2), "Test message 2", startDateCal.getTime(), endDateCal.getTime());

        startDateCal.add(Calendar.DAY_OF_MONTH, -30);
        endDateCal.add(Calendar.DAY_OF_MONTH, -30);
        invalidMessage = new SystemMessage(BigDecimal.valueOf(3), "Invalid Message", startDateCal.getTime(), endDateCal.getTime());

        gigaSpace.clear(null);

        gigaSpace.write(message1);
        gigaSpace.write(message2);
        gigaSpace.write(invalidMessage);

        systemMessageDAO = mock(SystemMessageDAO.class);
        when(systemMessageDAO.findValid()).thenReturn(Arrays.asList(message1, message2));

        underTest = new GigaspaceSystemMessageRepository(gigaSpace, systemMessageDAO);
    }

    @Test
    public void findValidReadsAllValidObjectsInSpace() {
        final Collection<SystemMessage> systemMessages = underTest.findValid();

        assertThat(systemMessages, is(not(nullValue())));
        assertThat(systemMessages.size(), is(equalTo(2)));
        assertThat(systemMessages, hasItem(message1));
        assertThat(systemMessages, hasItem(message2));
        assertThat(systemMessages, not(hasItem(invalidMessage)));
    }

    @Test
    public void loadIntoSpaceReadsObjectFromDatabaseAndWritesToSpace() {
        gigaSpace.clear(null);

        underTest.refreshSystemMessages();

        verify(systemMessageDAO).findValid();

        assertThat(gigaSpace.read(message1), is(not(nullValue())));
        assertThat(gigaSpace.read(message1), is(not(nullValue())));
    }

    @Test
    public void loadIntoSpaceSucceedsWhenNoMessagesAreAvailable() {
        gigaSpace.clear(null);
        reset(systemMessageDAO);

        underTest.refreshSystemMessages();

        verify(systemMessageDAO).findValid();

        assertThat(gigaSpace.readMultiple(new SystemMessage()).length, is(equalTo(0)));
    }
}
