package com.yazino.platform.player.util;

import com.yazino.platform.player.PasswordType;

public interface Hasher {

    /**
     * The hash type.
     *
     * @return the hash type.
     */
    PasswordType getType();

    /**
     * Hash a value.
     *
     * @param value the value.
     * @param salt  the salt for the value.
     * @return the BASE64 encoded hashed value.
     */
    String hash(String value, byte[] salt);

    /**
     * Generate a salt, if required by the hash function.
     * <p/>
     * This must be 64bits or null. If its not 64bits it'll get padded
     * during save and this'll lead to fun.
     *
     * @return a 64bit hash or null.
     */
    byte[] generateSalt();

}
