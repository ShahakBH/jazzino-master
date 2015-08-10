package strata.server.test.helpers;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static strata.server.test.helpers.Matchers.*;
import static strata.server.test.helpers.Shortcuts.*;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

public class MatchersTest {
    @SuppressWarnings("unchecked")
    @Test
    public void shouldValidateListInRightOrder() {
        // GIVEN a list
        final List<Integer> intList = new ArrayList<Integer>();
        intList.add(0);
        intList.add(1);
        intList.add(2);
        intList.add(3);
        intList.add(4);

        // WHEN validating the list
        assertThat(intList, hasItemsInOrder(is(0), is(2), is(4)));

        // THEN the test should pass
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldValidateListInRightOrderNotToEnd() {
        // GIVEN a list
        final List<Integer> intList = new ArrayList<Integer>();
        intList.add(0);
        intList.add(1);
        intList.add(2);
        intList.add(3);
        intList.add(4);

        // WHEN validating the list
        assertThat(intList, hasItemsInOrder(is(1), is(2), is(3)));

        // THEN the test should pass
    }

    @SuppressWarnings("unchecked")
    @Test(expected = AssertionError.class)
    public void shouldBreakOnListInWrongOrder() {
        // GIVEN a list
        final List<Integer> intList = new ArrayList<Integer>();
        intList.add(0);
        intList.add(1);
        intList.add(2);
        intList.add(3);
        intList.add(4);

        // WHEN validating the list
        assertThat(intList, hasItemsInOrder(is(2), is(4), is(0)));

        // THEN the test should fail
    }

    @Test
    public void shouldValidateItemCouplesInRightOrder() {
        // GIVEN a list
        final List<Integer> intList = new ArrayList<Integer>();
        intList.add(0);
        intList.add(1);
        intList.add(2);
        intList.add(3);
        intList.add(4);

        // WHEN validating the list
        assertThat(is(1), inList(intList).isBefore(is(3)));

        // THEN the test should pass
    }

    @Test(expected = AssertionError.class)
    public void shouldBreakOnItemCouplesInWrongOrder() {
        // GIVEN a list
        final List<Integer> intList = new ArrayList<Integer>();
        intList.add(0);
        intList.add(1);
        intList.add(2);
        intList.add(3);
        intList.add(4);

        // WHEN validating the list
        assertThat(is(3), inList(intList).isBefore(is(1)));

        // THEN the test should fail
    }

    @Test(expected = AssertionError.class)
    public void shouldBreakOnMissingItem() {
        // GIVEN a list
        final List<Integer> intList = new ArrayList<Integer>();
        intList.add(0);
        intList.add(1);
        intList.add(2);
        intList.add(3);
        intList.add(4);

        // WHEN validating the list
        assertThat(is(3), inList(intList).isBefore(is(6)));

        // THEN the test should fail
    }

    @Test
    public void shouldPassOnSameDay() {
        // GIVEN two dates
        final Calendar cal = Calendar.getInstance();
        cal.set(2011, 9, 1, 12, 0);
        final Date dat1 = cal.getTime();
        cal.set(2011, 9, 1, 14, 0);
        final Date dat2 = cal.getTime();

        // WHEN validating this is the same day
        assertThat(dat1, isSameDateAs(dat2));

        // THEN the test should pass
    }

    @Test(expected = AssertionError.class)
    public void shouldFailOnDifferentDay() {
        // GIVEN two dates
        final Calendar cal = Calendar.getInstance();
        cal.set(2011, 9, 1, 12, 0);
        final Date dat1 = cal.getTime();
        cal.set(2011, 9, 2, 14, 0);
        final Date dat2 = cal.getTime();

        // WHEN validating this is the same day
        assertThat(dat1, isSameDateAs(dat2));

        // THEN the test should fail
    }

    @Test
    public void shouldValidateCombination() {
        // GIVEN a number
        final int ten = 10;

        // WHEN validating the list
        assertThat(ten, matches(greaterThan(5)).and(lessThan(12)));

        // THEN the test should pass
    }

    @Test(expected = AssertionError.class)
    public void shouldFailIfOneWrong() {
        // GIVEN a number
        final int ten = 10;

        // WHEN validating the list
        assertThat(ten, matches(greaterThan(5)).and(lessThan(7)));

        // THEN the test should fail
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldValidateMapSize() {
        // GIVEN a map
        final Map<String, String> map = asMap(kv("", ""));

        // WHEN validating the map size
        assertThat(map, entriesNumber(1));

        // THEN the test should pass
    }

    @SuppressWarnings("unchecked")
    @Test(expected = AssertionError.class)
    public void shouldInvalidateWrongMapSize() {
        // GIVEN a map
        final Map<String, String> map = asMap(kv("", ""));

        // WHEN validating the map size
        assertThat(map, entriesNumber(2));

        // THEN the test should fail
    }

    private static enum Tst {
        ONE, TWO
    };

    @Test
    public void shouldValidateEnumerated() throws SecurityException, NoSuchMethodException {
        // GIVEN a set containing all of the elements of Tst
        final Set<Tst> set = setOf(Tst.ONE, Tst.TWO);

        // WHEN validating the set
        assertThat(set, hasAllEnumerated(Tst.class));

        // Then the test should pass
    }

    @Test(expected = AssertionError.class)
    public void shouldInvalidateMissing() throws SecurityException, NoSuchMethodException {
        // GIVEN a set containing all of the elements of Tst
        final Set<Tst> set = setOf(Tst.ONE);

        // WHEN validating the set
        assertThat(set, hasAllEnumerated(Tst.class));

        // Then the test should pass
    }

    @Test
    public void shouldValidateEnumeratedStrings() throws SecurityException, NoSuchMethodException {
        // GIVEN a set containing all of the elements of Tst
        final Set<String> set = setOf("ONE", "TWO");

        // WHEN validating the set
        assertThat(set, hasAllEnumeratedStringValues(Tst.class));

        // Then the test should pass
    }

    @Test
    public void shouldValidateOnlyEnumeratedStrings() throws SecurityException, NoSuchMethodException {
        // GIVEN a set containing all of the elements of Tst
        final Set<String> set = setOf("ONE", "TWO");

        // WHEN validating the set
        assertThat(set, hasOnlyAllEnumeratedStringValues(Tst.class));

        // Then the test should pass
    }

    @Test(expected = AssertionError.class)
    public void shouldInvalidateMissingStrings() throws SecurityException, NoSuchMethodException {
        // GIVEN a set containing all of the elements of Tst
        final Set<String> set = setOf("ONE");

        // WHEN validating the set
        assertThat(set, hasAllEnumeratedStringValues(Tst.class));

        // Then the test should pass
    }

    @Test(expected = AssertionError.class)
    public void shouldInvalidateExtraStrings() throws SecurityException, NoSuchMethodException {
        // GIVEN a set containing all of the elements of Tst
        final Set<String> set = setOf("ONE", "TWO", "THREE");

        // WHEN validating the set
        assertThat(set, hasOnlyAllEnumeratedStringValues(Tst.class));

        // Then the test should pass
    }

    @Test
    public void shouldValidateIntegerNumber() {
        // GIVEN a number
        final int ten = 10;

        // WHEN validating the list
        assertThat(ten, approximatesTo(10D, 0.1));

        // THEN the test should pass
    }

    @Test(expected = AssertionError.class)
    public void shouldFailIfNumberMismatch() {
        // GIVEN a number
        final int ten = 10;

        // WHEN validating the list
        assertThat(ten, approximatesTo(9D, 0.1));

        // THEN the test should fail
    }

    @Test
    public void shouldValidateDoubleNumber() {
        // GIVEN a number
        final double ten = 10D;

        // WHEN validating the list
        assertThat(ten, approximatesTo(10D, 0.1));

        // THEN the test should pass
    }
}
