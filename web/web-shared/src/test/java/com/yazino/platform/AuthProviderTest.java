package com.yazino.platform;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@SuppressWarnings("deprecation")
public class AuthProviderTest {

    @Test
    public void shouldUseProviderNameAsToString() {
        assertThat(AuthProvider.YAHOO.toString(), equalTo("Yahoo!"));
    }

    @Test
    public void shouldParseProviderName() {
        assertThat(AuthProvider.parseProviderName("Yahoo!"), equalTo(AuthProvider.YAHOO));
    }

    @Test
    public void shouldParseProviderNameCaseInsensitive(){
        assertThat(AuthProvider.parseProviderName("YAhOO!"), equalTo(AuthProvider.YAHOO));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotParseNullProviderName(){
        AuthProvider.parseProviderName(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotParseUnknownProviderName(){
        AuthProvider.parseProviderName("unknown");
    }
}
