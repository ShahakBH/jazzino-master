package com.yazino.platform.model;

import com.google.common.base.Function;
import org.junit.Test;

import java.util.Collections;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class PagedDataTest {

    @SuppressWarnings({"ConstantConditions"})
    @Test(expected = NullPointerException.class)
    public void transformRejectsANullTransformer() {
        final PagedData<Integer> testData = new PagedData<>(10, 20, 1000, asList(1, 2, 3));

        testData.transform(null);
    }

    @Test
    public void transformCovertsTheTypeOfThePagedData() {
        final PagedData<Integer> testData = new PagedData<>(10, 20, 1000, asList(1, 2, 3));

        final PagedData<String> transformed = testData.transform(new TestTransformer());

        assertThat(transformed.getStartPosition(), is(equalTo(10)));
        assertThat(transformed.getSize(), is(equalTo(20)));
        assertThat(transformed.getTotalSize(), is(equalTo(1000)));
        assertThat(transformed.getData(), is(equalTo(asList("1", "2", "3"))));
    }

    @Test
    public void anEmptyDataCanBeConstructed() {
        final PagedData<String> empty = PagedData.empty();

        assertThat(empty.getStartPosition(), is(equalTo(0)));
        assertThat(empty.getSize(), is(equalTo(0)));
        assertThat(empty.getTotalSize(), is(equalTo(0)));
        assertThat(empty.getData(), is(equalTo(Collections.<String>emptyList())));
    }

    private class TestTransformer implements Function<Integer, String> {
        @Override
        public String apply(final Integer integer) {
            if (integer != null) {
                return integer.toString();
            }
            return "null";
        }
    }

}
