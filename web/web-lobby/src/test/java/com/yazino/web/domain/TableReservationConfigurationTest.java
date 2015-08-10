package com.yazino.web.domain;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TableReservationConfigurationTest {

    @Test
    public void shouldCreateWithListOfGameTypesWithNoReservationSupport() {
        TableReservationConfiguration underTest = new TableReservationConfiguration("ROULETTE, SLOTS");
        assertTrue(underTest.supportsReservation("BLACKJACK"));
        assertFalse(underTest.supportsReservation("SLOTS"));
        assertFalse(underTest.supportsReservation("ROULETTE"));
    }

    @Test
    public void shouldCreateWithEmptyString() {
        TableReservationConfiguration underTest = new TableReservationConfiguration("");
        assertTrue(underTest.supportsReservation("BLACKJACK"));
    }

    @Test
    public void shouldCreateWithNullString() {
        TableReservationConfiguration underTest = new TableReservationConfiguration(null);
        assertTrue(underTest.supportsReservation("BLACKJACK"));
    }
}
