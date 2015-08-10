package com.yazino.novomatic.cgs.message;

import com.yazino.novomatic.cgs.NovomaticEventType;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class RecordTypeTest {

    @Test
    public void shouldRecogniseFromNovomaticType() {
        assertEquals(NovomaticEventType.EventReelsRotate, NovomaticEventType.fromNovomaticType(NovomaticEventType.EventReelsRotate.getNovomaticEventType()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldBreakIfNovomaticTypeIsNotRecognised() {
        NovomaticEventType.fromNovomaticType("something weird");
    }
}
