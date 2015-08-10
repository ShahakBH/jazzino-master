package com.yazino.web.form;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TableLocatorFormTest {
    private TableLocatorForm underTest = new TableLocatorForm();

    @Test
    public void validationPassesSuccessfulyAsAllValuesAreProvided() {
        underTest.setGameType("BLACKJACK");
        underTest.setVariationName("Atlantic City");
        underTest.setClientId("Red Blackjack");

        assertNull(underTest.validate());
    }

    @Test
    public void validatonFailsAsSoonAsBlankFieldIsFound() {
        underTest.setGameType(null);
        assertEquals(TableLocatorForm.GAME_TYPE_BLANK, underTest.validate());
    }
}
