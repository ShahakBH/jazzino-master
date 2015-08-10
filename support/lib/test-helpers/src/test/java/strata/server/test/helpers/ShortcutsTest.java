package strata.server.test.helpers;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static strata.server.test.helpers.Shortcuts.*;

import java.util.Map;
import java.util.Set;

import org.junit.Test;

public class ShortcutsTest {
    @SuppressWarnings("unchecked")
    @Test
    public void shouldCreateMap() {
        // GIVEN a set of key-value pairs
        final String key1 = "str1";
        final Integer val1 = 1;
        final String key2 = "str2";
        final Integer val2 = 2;

        // WHEN building the matching map
        final Map<String, Integer> map = asMap(kv(key1, val1), kv(key2, val2));

        // THEN the result is a map containing all these elements
        assertThat(map, allOf(hasEntry(key1, val1), hasEntry(key2, val2)));
    }

    @Test
    public void shouldCreateSet() {
        // GIVEN a set numbers
        final int one = 1;
        final int two = 2;
        final int three = 3;

        // WHEN building the matching map
        final Set<Integer> set = setOf(one, two, three);

        // THEN the result is a map containing all these elements
        assertThat(set, hasItems(one, two, three));
    }
}
