package com.yazino.yaps;

import org.junit.Test;

public class SecurityConfigTest {

    @Test(expected = NullPointerException.class)
    public void shouldThrowExceptionIfAttemptToSetNullDirectoryName() throws Exception {
        new SecurityConfig(null, "aCertificate");
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowExceptionIfAttemptToSetNullCertificateName() throws Exception {
        new SecurityConfig("aDirectory", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfAttemptToSetEmptyCertificateName_1() throws Exception {
        new SecurityConfig("aDirectory", "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfAttemptToSetEmptyCertificateName_2() throws Exception {
        new SecurityConfig("aDirectory", " ");
    }


}
