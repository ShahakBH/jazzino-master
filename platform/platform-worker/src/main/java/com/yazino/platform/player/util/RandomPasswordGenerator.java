package com.yazino.platform.player.util;

import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.UUID;

@Service("passwordGenerator")
public class RandomPasswordGenerator implements PasswordGenerator {
    private static final int MINIMUM_CHARACTERS = 7;
    private static final int MAX_ADDITIONAL_CHARACTERS = 5;

    private Random random = new Random();
    private volatile String oldPassword = "";

    public String generatePassword() {
        String newPassword;
        do {
            newPassword = UUID.randomUUID().toString().replaceAll("-", "");
            newPassword = newPassword.substring(0, MINIMUM_CHARACTERS
                    + random.nextInt(MAX_ADDITIONAL_CHARACTERS));
        }
        while (newPassword.equals(oldPassword));
        oldPassword = newPassword;
        return newPassword;
    }
}
