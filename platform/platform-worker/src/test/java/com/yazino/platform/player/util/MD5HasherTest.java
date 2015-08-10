package com.yazino.platform.player.util;

import com.yazino.platform.player.PasswordType;
import org.junit.Assert;
import org.junit.Test;

import java.io.UnsupportedEncodingException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class MD5HasherTest {
    private MD5Hasher underTest = new MD5Hasher();

    @Test
    public void hashMatchesAnExternalGeneratedHash() {
        final String result = underTest.hash("foo", null);

        assertThat(result, is(equalTo("rL0Y20zC+Fzt72VPzMSk2A==")));
    }

    @Test
    public void hashIgnoresTheSalt() throws UnsupportedEncodingException {
        final String result = underTest.hash("foo", "aFakeSalt".getBytes("UTF-8"));

        Assert.assertEquals("rL0Y20zC+Fzt72VPzMSk2A==", result);
    }

    @Test
    public void generateSaltReturnsNull() {
        assertThat(underTest.generateSalt(), is(nullValue()));
    }

    @Test
    public void hasherIsOfTypeMD5() {
        assertThat(underTest.getType(), is(equalTo(PasswordType.MD5)));
    }
}
