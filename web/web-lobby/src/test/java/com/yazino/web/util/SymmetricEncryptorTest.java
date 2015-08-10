package com.yazino.web.util;

import com.yazino.configuration.YazinoConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.security.GeneralSecurityException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SymmetricEncryptorTest {
    @Mock
    private YazinoConfiguration yazinoConfiguration;

    private SymmetricEncryptor underTest;

    @Before
    public void setUp() {
        underTest = new SymmetricEncryptor(yazinoConfiguration);

        when(yazinoConfiguration.getString("client.auth.key")).thenReturn("ZNEF4AjQbJoQhh7B");
        when(yazinoConfiguration.getString("client.auth.iv")).thenReturn("scpaLGE6LCjiD3nZ");
    }

    @Test(expected = NullPointerException.class)
    public void encryptorCannotBeCreatedWithANullConfiguration() {
        new SymmetricEncryptor(null);
    }

    @Test
    public void aPlainTextStringCanBeEncrypted() throws GeneralSecurityException {
        assertThat(underTest.encrypt("my super secret text"), is(equalTo("ASK78n77MiNd1UmV8A0lfeBO6J6+hgY7sz3JEppBfRw=")));
    }

    @Test
    public void aNullStringIsEncryptedToNull() throws GeneralSecurityException {
        assertThat(underTest.encrypt(null), is(nullValue()));
    }

    @Test
    public void anEncryptedStringCanBeDecrypted() throws GeneralSecurityException {
        assertThat(underTest.decrypt("ASK78n77MiNd1UmV8A0lfeBO6J6+hgY7sz3JEppBfRw="), is(equalTo("my super secret text")));
    }

    @Test
    public void aNullStringIsDecryptedToNull() throws GeneralSecurityException {
        assertThat(underTest.decrypt(null), is(nullValue()));
    }
}
