package com.yazino.platform.player.util;

import org.junit.Assert;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RandomPasswordGeneratorTest {
    RandomPasswordGenerator underTest = new RandomPasswordGenerator();

    @Test
    public void shouldGenerateRandomPassword_5_to_20_characters_alphanumeric_no_spaces() {
        String newPassword = underTest.generatePassword();
        assertTrue(newPassword.length() >= 5);
        assertTrue(newPassword.length() <= 20);
        assertFalse(findUnwantedCharacters(newPassword));
    }

    @Test
    public void subsequentPasswordsAreDifferent() {
        String oldPassword = underTest.generatePassword();
        String newPassword = underTest.generatePassword();
        Assert.assertFalse(newPassword.equals(oldPassword));
    }

    private boolean findUnwantedCharacters(final String value) {
        Pattern p = Pattern.compile("\\W");
        Matcher m = p.matcher(value);
        if (m.find()) {
            return true;
        }
        return false;
    }
}
