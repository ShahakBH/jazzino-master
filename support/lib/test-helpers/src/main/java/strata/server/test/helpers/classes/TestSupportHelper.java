package strata.server.test.helpers.classes;

/**
 * Helpers for test support
 */
public final class TestSupportHelper {

    /**
     * .
     */
    private TestSupportHelper() {
    }

    /**
     * See if a particular field is in the exclusion list
     * @param fieldName Field name
     * @param excludes Exclusions list
     * @return true is the field is excluded
     */
    public static boolean checkExcludes(final String fieldName, final String... excludes) {
        boolean stop = false;
        for (final String exclude : excludes) {
            if (exclude.equalsIgnoreCase(fieldName)) {
                stop = true;
                break;
            }
        }
        return stop;
    }

}
