package com.yazino.bi.operations.persistence;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ReferenceTest {

    @Test
    public void shouldSetEmptyStringsWithNullReferenceString() {
        Reference underTest = new Reference(null);
        assertEquals("", underTest.getTableId());
        assertEquals("", underTest.getGameId());
        assertEquals("", underTest.getReference());
    }

    @Test
    public void shouldSetEmptyStringsWithEmptyReferenceString() {
        Reference underTest = new Reference("");
        assertEquals("", underTest.getTableId());
        assertEquals("", underTest.getGameId());
        assertEquals("", underTest.getReference());
    }

    @Test
    public void shouldSetReferenceOnly() {
        Reference underTest = new Reference("look no table and game");
        assertEquals("", underTest.getTableId());
        assertEquals("", underTest.getGameId());
        assertEquals("look no table and game", underTest.getReference());
    }

    @Test
    public void shouldSetTableAndGameIdsOnly() {
        Reference underTest = new Reference("654365|67");
        assertEquals("654365", underTest.getTableId());
        assertEquals("67", underTest.getGameId());
        assertEquals("", underTest.getReference());
    }

    @Test
    public void shouldSetTableIdGameIdAndRef() {
        Reference underTest = new Reference("654365|67|BonusPayout");
        assertEquals("654365", underTest.getTableId());
        assertEquals("67", underTest.getGameId());
        assertEquals("BonusPayout", underTest.getReference());
    }
}
