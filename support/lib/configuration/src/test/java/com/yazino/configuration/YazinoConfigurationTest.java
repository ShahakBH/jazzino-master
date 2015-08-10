package com.yazino.configuration;

import org.junit.Test;

import java.util.Collections;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class YazinoConfigurationTest {

    private final YazinoConfiguration underTest = new YazinoConfiguration();

    @Test
    public void aCommaSeparatedPropertyIsNotParsedAsAList() {
        assertThat(underTest.getString("property.one"), is(equalTo("a,b,c")));
    }

    @Test
    public void anInterpolatedCommaSeparatedPropertyIsNotParsedAsAList() {
        assertThat(underTest.getString("property.two"), is(equalTo("a,b,c")));
    }

    @Test
    public void aCommaSeparatedPropertyMayBeRetrievedAsAStringArray() {
        assertThat(underTest.getStringArray("property.one"), is(equalTo(new String[]{"a", "b", "c"})));
    }

    @Test
    public void anInterpolatedCommaSeparatedPropertyMayBeRetrievedAsAStringArray() {
        assertThat(underTest.getStringArray("property.two"), is(equalTo(new String[]{"a", "b", "c"})));
    }

    @Test
    public void aNonExistentPropertyMayBeRetrievedAsAStringArray() {
        assertThat(underTest.getStringArray("property.none"), is(nullValue()));
    }

    @Test
    public void aCommaSeparatedPropertyMayBeRetrievedAsAList() {
        assertThat(underTest.getList("property.one"), is(equalTo(asList((Object) "a", "b", "c"))));
    }

    @Test
    public void anInterpolatedCommaSeparatedPropertyMayBeRetrievedAsAList() {
        assertThat(underTest.getList("property.two"), is(equalTo(asList((Object) "a", "b", "c"))));
    }

    @Test
    public void aNonExistentPropertyMayBeRetrievedAsAList() {
        assertThat(underTest.getList("property.none"), is(nullValue()));
    }

    @Test
    public void aNonExistentPropertyMayBeRetrievedAsADefaultedList() {
        assertThat(underTest.getList("property.none", Collections.emptyList()), is(equalTo(Collections.emptyList())));
    }
}
