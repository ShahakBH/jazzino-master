package com.yazino.platform.messaging.host;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openspaces.core.GigaSpace;

import java.util.Collections;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

@RunWith(MockitoJUnitRunner.class)
public class GigaspaceDocumentFactoryPublisherTest {
    @Mock
    private GigaSpace gigaSpace;
    @Mock
    private HostDocument hostDocument1;
    @Mock
    private HostDocument hostDocument2;

    private HostDocumentPublisher unit;

    @Before
    public void setUp() throws Exception {
        unit = new GigaspaceHostDocumentPublisher(gigaSpace);
    }

    @SuppressWarnings({"ConstantConditions"})
    @Test(expected = NullPointerException.class)
    public void constructorDoesNotAcceptANullGigaSpace() {
        new GigaspaceHostDocumentPublisher(null);
    }

    @SuppressWarnings({"ConstantConditions"})
    @Test(expected = NullPointerException.class)
    public void aNullDocumentFactoryListIsRejected() {
        unit.publish(null);
    }

    @Test
    public void anEmptyDocumentFactoryListIsIgnored() {
        unit.publish(Collections.<HostDocument>emptyList());

        verifyZeroInteractions(gigaSpace);
    }

    @Test
    public void documentsAreWrittenToTheSpaceInAWrapper() {
        unit.publish(asList(hostDocument1, hostDocument2));

        verify(gigaSpace).write(new HostDocumentWrapper(asList(hostDocument1, hostDocument2)));
    }
}
