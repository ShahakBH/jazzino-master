package com.yazino.platform.messaging;

import com.yazino.platform.messaging.host.HostDocument;
import com.yazino.platform.messaging.host.HostDocumentWrapper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openspaces.events.EventTemplate;
import org.openspaces.events.adapter.SpaceDataEvent;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;

public class HostDocumentProcessorTest {

    @Mock
    private DocumentDispatcher documentDispatcher;
    @Mock
    private HostDocument hostDocument1;
    @Mock
    private HostDocument hostDocument2;

    private HostDocumentProcessor unit;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        unit = new HostDocumentProcessor(documentDispatcher);
    }

    @Test
    public void processMethodIsAnnotated() throws NoSuchMethodException {
        final SpaceDataEvent annotation = unit.getClass().getMethod("process", HostDocumentWrapper.class)
                .getAnnotation(SpaceDataEvent.class);

        assertThat(annotation, is(not(nullValue())));
    }

    @Test
    public void templateMethodIsAnnotated() throws NoSuchMethodException {
        final EventTemplate annotation = unit.getClass().getMethod("template").getAnnotation(EventTemplate.class);

        assertThat(annotation, is(not(nullValue())));
    }

    @Test
    public void templateIsAnEmptyWrapper() {
        assertThat(unit.template(), is(equalTo(new HostDocumentWrapper())));
    }

    @SuppressWarnings({"ConstantConditions"})
    @Test(expected = NullPointerException.class)
    public void cannotBeConstructedWithANullContext() {
        new HostDocumentProcessor(null);
    }

    @Test
    public void eachDocumentFactoryIsSentWhereMultipleFactoriesAreSupplied() {
        unit.process(wrap(hostDocument1, hostDocument2));

        verify(hostDocument1).send(documentDispatcher);
        verify(hostDocument2).send(documentDispatcher);
    }

    @Test
    public void documentFactoriesAreSentForASingleValue() {
        unit.process(wrap(hostDocument1));

        verify(hostDocument1).send(documentDispatcher);
    }

    private HostDocumentWrapper wrap(final HostDocument... hostDocuments) {
        return new HostDocumentWrapper(Arrays.asList(hostDocuments));
    }

}
