package strata.server.test.helpers;

import static java.util.Calendar.*;
import static org.hamcrest.Matchers.*;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

/**
 * Miscellaneous Hamcrest-like matchers
 */
public final class Matchers {
    /**
     * No public constructor
     */
    private Matchers() {
    }

    public static Matcher<Date> isSameDateAs(final Date dat) {
        return new IsSameDateAs(dat);
    }

    private static class IsSameDateAs extends BaseMatcher<Date> {
        private final Calendar cal;

        public IsSameDateAs(final Date dat) {
            this.cal = Calendar.getInstance();
            this.cal.setTime(dat);
        }

        @Override
        public boolean matches(final Object item) {
            final Calendar compare = Calendar.getInstance();
            compare.setTime((Date) item);
            return compare.get(YEAR) == cal.get(YEAR) && compare.get(MONTH) == cal.get(MONTH)
                    && compare.get(DAY_OF_MONTH) == cal.get(DAY_OF_MONTH);
        }

        @Override
        public void describeTo(final Description description) {
            description.appendText("should be equal to ").appendText(cal.getTime().toString());
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes" })
    public static <T> Matcher<List<T>> hasItemsInOrder(final Matcher<T>... items) {
        return new HasItemsInOrder(items);
    }

    private static class HasItemsInOrder<T> extends BaseMatcher<List<T>> {
        private final Matcher<T>[] itemMatchers;

        public HasItemsInOrder(final Matcher<T>... itemMatchers) {
            this.itemMatchers = itemMatchers;
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean matches(final Object otherObject) {
            final List<T> list = (List<T>) otherObject;
            int lastFound = -1;
            boolean foundInIteration = false;
            for (final Matcher<T> matcher : itemMatchers) {
                foundInIteration = false;
                for (int i = lastFound + 1; i < list.size(); i++) {
                    if (matcher.matches(list.get(i))) {
                        lastFound = i;
                        foundInIteration = true;
                        break;
                    }
                }
                if (!foundInIteration) {
                    break;
                }
            }
            return foundInIteration;
        }

        @Override
        public void describeTo(final Description description) {
            for (final Matcher<T> matcher : itemMatchers) {
                description.appendDescriptionOf(matcher).appendText("\n");
            }
            description.appendText(" should be in that order");
        }

    }

    public static <T> InList<T> inList(final List<T> list) {
        return new InList<T>(list);
    }

    public static final class InList<T> {
        private final List<T> list;

        private InList(final List<T> list) {
            this.list = list;
        }

        public Matcher<Matcher<T>> isBefore(final Matcher<T> item) {
            return new IsBeforeInList<T>(list, item);
        }
    }

    private static class IsBeforeInList<T> extends BaseMatcher<Matcher<T>> {
        private final List<T> list;
        private final Matcher<T> itemMatcher;

        public IsBeforeInList(final List<T> list, final Matcher<T> itemMatcher) {
            this.itemMatcher = itemMatcher;
            this.list = list;
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean matches(final Object otherObject) {
            final Matcher<T> otherMatcher = (Matcher<T>) otherObject;
            boolean firstFound = false, secondFound = false;
            for (final T item : list) {
                if (otherMatcher.matches(item)) {
                    if (secondFound) {
                        return false;
                    }
                    firstFound = true;
                }
                if (itemMatcher.matches(item)) {
                    secondFound = true;
                }
            }
            return firstFound && secondFound;
        }

        @Override
        public void describeTo(final Description description) {
            description.appendDescriptionOf(itemMatcher).appendText(" should come after");
        }

    }

    public static <T> CombinationMatcher<T> matches(final Matcher<T> matcher) {
        return new CombinationMatcher<T>(matcher);
    }

    public static class CombinationMatcher<T> extends BaseMatcher<T> {
        private final List<Matcher<? extends T>> matchers = new ArrayList<Matcher<? extends T>>();

        public CombinationMatcher(final Matcher<T> matcher) {
            and(matcher);
        }

        public CombinationMatcher<T> and(final Matcher<? extends T> matcher) {
            matchers.add(matcher);
            return this;
        }

        @SuppressWarnings({"unchecked", "rawtypes" })
        @Override
        public boolean matches(final Object item) {
            return allOf((List) matchers).matches(item);
        }

        @Override
        public void describeTo(final Description description) {
            description.appendList("Matches all of {", ",\n", "\n}", matchers);
        }
    }

    @SuppressWarnings("rawtypes")
    public static Matcher entriesNumber(final int num) {
        return new EntriesNumber(num);
    }

    @SuppressWarnings("rawtypes")
    public static class EntriesNumber extends BaseMatcher {

        private final int size;

        public EntriesNumber(final int size) {
            this.size = size;
        }

        @Override
        public boolean matches(final Object item) {
            final Map<?, ?> castObject = (Map) item;
            return castObject.size() == size;
        }

        @Override
        public void describeTo(final Description description) {
            description.appendText("Should have the size of " + size);
        }

    }

    public static <T extends Enum<?>> Matcher<Iterable<T>> hasAllEnumerated(final Class<T> ofClass) {
        return new HasAllEnumerated<T>(ofClass);
    }

    private static class HasAllEnumerated<T extends Enum<?>> extends BaseMatcher<Iterable<T>> {
        private final EnumAccessor<T> accessor;

        public HasAllEnumerated(final Class<T> clazz) {
            super();
            this.accessor = new EnumAccessor<T>(clazz);
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean matches(final Object item) {
            final Iterable<T> collection = (Iterable<T>) item;
            for (final T value : accessor.values()) {
                boolean found = false;
                for (final T it : collection) {
                    if (value.equals(it)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public void describeTo(final Description description) {
            description.appendText("Expected all the values of " + accessor.getClassName());
        }
    }

    public static <T extends Enum<?>> Matcher<Iterable<String>>
            hasAllEnumeratedStringValues(final Class<T> ofClass) {
        return new HasAllEnumeratedStringValues<T>(ofClass, false);
    }

    public static <T extends Enum<?>> Matcher<Iterable<String>>
            hasOnlyAllEnumeratedStringValues(final Class<T> ofClass) {
        return new HasAllEnumeratedStringValues<T>(ofClass, true);
    }

    private static class HasAllEnumeratedStringValues<T extends Enum<?>> extends
            BaseMatcher<Iterable<String>> {
        private final EnumAccessor<T> accessor;
        private final boolean strict;

        public HasAllEnumeratedStringValues(final Class<T> clazz, final boolean strict) {
            super();
            this.accessor = new EnumAccessor<T>(clazz);
            this.strict = strict;
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean matches(final Object item) {
            final Iterable<String> collection = (Iterable<String>) item;
            for (final T value : accessor.values()) {
                boolean found = false;
                for (final String str : collection) {
                    if (accessor.name(value).equals(str)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    return false;
                }
            }
            if (strict) {
                int count = 0;
                final Iterator<String> iterator = collection.iterator();
                while (iterator.hasNext()) {
                    count++;
                    iterator.next();
                }
                if (count != accessor.values().length) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public void describeTo(final Description description) {
            description.appendText("Expected all the values of " + accessor.getClassName());
        }
    }

    public static Matcher<Number> approximatesTo(final double num, final double delta) {
        return new ApproximatesTo(num, delta);
    }

    private static class ApproximatesTo extends BaseMatcher<Number> {
        private final double num;
        private final double delta;

        public ApproximatesTo(final double num, final double delta) {
            this.num = num;
            this.delta = delta;
        }

        @Override
        public boolean matches(final Object item) {
            final Double number = ((Number) item).doubleValue();
            return number > num - delta && number < num + delta;
        }

        @Override
        public void describeTo(final Description description) {
            description.appendText("should be approximately equal to ").appendText("" + num);
        }
    }
}
