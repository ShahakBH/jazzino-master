package strata.server.test.helpers.classes;

import static org.junit.Assert.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

/**
 * Tests some basic class functionality, like hidden constructors or equality
 */
public final class ClassStructureTestSupport {
    /**
     * No default constructor
     */
    private ClassStructureTestSupport() {
    }

    /**
     * Tests the presence of a default class constructor
     * @param classToTest Class to test
     * @throws InvocationTargetException .
     * @throws IllegalAccessException .
     * @return True if the constructor is present and callable 
     * @throws InstantiationException .
     */
    public static boolean testDefaultConstructor(final Class < ? > classToTest) throws IllegalAccessException,
            InvocationTargetException, InstantiationException {
        Constructor < ? > declaredConstructor = null;
        try {
            declaredConstructor = classToTest.getDeclaredConstructor();
        } catch (final NoSuchMethodException e) {
            System.out.println("No constructor found");
            return false;
        }
        try {
            declaredConstructor.setAccessible(true);
            declaredConstructor.newInstance();
        } catch (final InvocationTargetException e) {
            System.out.println("Cannot instantiate class with the default constructor");
            return false;
        }
        return true;
    }

    /**
     * Tests the "equals"
     * @param classToTest Class that we test equals and hashCode for
     * @param excludes Fields for exclude
     * @throws NoSuchMethodException .
     * @throws InvocationTargetException .
     * @throws IllegalAccessException .
     * @throws InstantiationException .
     * @return True if the test succeeds
     */
    public static boolean testEquality(final Class < ? > classToTest, final String... excludes)
            throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        final Constructor < ? > constructor1 = classToTest.getDeclaredConstructor();
        constructor1.setAccessible(true);
        final Object item1 = constructor1.newInstance();
        final Constructor < ? > constructor2 = classToTest.getDeclaredConstructor();
        constructor2.setAccessible(true);
        final Object item2 = constructor2.newInstance();
        if (!item1.equals(item1)) {
            return false;
        }
        if (item1.equals(null)) {
            return false;
        }
        if (item1.equals(new Object())) {
            return false;
        }
        for (final Field field : classToTest.getDeclaredFields()) {
            if (TestSupportHelper.checkExcludes(field.getName(), excludes)) {
                continue;
            }
            if (field.getName().startsWith("$")) {
                continue;
            }
            if ((field.getModifiers() & Modifier.FINAL) != 0) {
                continue;
            }

            field.setAccessible(true);
            if (Map.class.isAssignableFrom(field.getType())) {
                continue;
            }

            instantiateNewField(item2, field, false);
            field.set(item1, null);
            if (item1.equals(item2)) {
                fail("False equality to null for the field " + field.getName());
            }

            instantiateNewField(item2, field, false);
            instantiateNewField(item1, field, true);
            if (item1.equals(item2)) {
                fail("False equality for the field " + field.getName());
            }
            instantiateNewField(item1, field, false);
        }
        if (!item1.equals(item2)) {
            fail("Two equal objects are declared inequal by mistake");
        }
        return true;
    }

    /**
     * Creates a new instance for the field of any type
     * @param object Object to set the field for
     * @param field Field to instantiate
     * @param alternateValue If true, assign a value different from default
     * @throws InstantiationException Thrown if we cannot instantiate the field
     * @throws IllegalAccessException Never thrown;
     * @throws InvocationTargetException .
     * @throws NoSuchMethodException .
     */
    private static void instantiateNewField(final Object object, final Field field, final boolean alternateValue)
            throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        final Class < ? > fieldType = field.getType();
        if (Number.class.isAssignableFrom(fieldType)) {
            final Constructor < ? > stringConstructor = fieldType.getConstructor(String.class);
            if (alternateValue) {
                field.set(object, stringConstructor.newInstance("1"));
            } else {
                field.set(object, stringConstructor.newInstance("0"));
            }
        } else if (Date.class.isAssignableFrom(fieldType)) {
            final Constructor < ? > longConstructor = fieldType.getConstructor(long.class);
            final Calendar cal = Calendar.getInstance();
            if (alternateValue) {
                cal.set(1984, 8, 29, 12, 0, 0);
            } else {
                cal.set(1974, 8, 29, 12, 0, 1);
            }
            cal.set(Calendar.MILLISECOND, 0);
            field.set(object, longConstructor.newInstance(cal.getTime().getTime()));
        } else if (fieldType.isEnum()) {
            final Object[] constants = fieldType.getEnumConstants();
            if (alternateValue) {
                field.set(object, constants[1]);
            } else {
                field.set(object, constants[0]);
            }
        } else if (Boolean.class.isAssignableFrom(fieldType)) {
            final Constructor < ? > stringConstructor = fieldType.getConstructor(String.class);
            if (alternateValue) {
                field.set(object, stringConstructor.newInstance(Boolean.TRUE.toString()));
            } else {
                field.set(object, stringConstructor.newInstance(Boolean.FALSE.toString()));
            }
        } else {
            final Constructor < ? > stringConstructor = fieldType.getConstructor(String.class);
            if (alternateValue) {
                field.set(object, stringConstructor.newInstance("*"));
            } else {
                field.set(object, stringConstructor.newInstance(""));
            }
        }
    }

    /**
     * Checks the hashCode function for the class. 
     * forceNull is false by default
     * @param classToTest Class to test
     * @param excludes Fields for exclude
     * @return true if the hash code is tested correctly
     * @throws NoSuchMethodException .
     * @throws InvocationTargetException .
     * @throws IllegalAccessException .
     * @throws InstantiationException .
     */
    public static boolean testHashCode(final Class < ? > classToTest, final String... excludes)
            throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        return testHashCode(classToTest, false, excludes);
    }

    /**
     * Checks the hashCode function for the class
     * @param classToTest Class to test
     * @param forceNulls Tries to force the null values to all fields
     * @param excludes Fields for exclude
     * @return true if the hash code is tested correctly
     * @throws NoSuchMethodException .
     * @throws InvocationTargetException .
     * @throws IllegalAccessException .
     * @throws InstantiationException .
     */
    public static boolean testHashCode(final Class < ? > classToTest, final boolean forceNulls,
            final String... excludes) throws InstantiationException, IllegalAccessException, InvocationTargetException,
            NoSuchMethodException {
        final Constructor < ? > constructor = classToTest.getDeclaredConstructor();
        constructor.setAccessible(true);
        final Object item = constructor.newInstance();
        if (item.hashCode() == 0) {
            return false;
        }
        for (final Field field : classToTest.getDeclaredFields()) {

            if (TestSupportHelper.checkExcludes(field.getName(), excludes)) {
                continue;
            }

            if ((field.getModifiers() & Modifier.FINAL) != 0) {
                continue;
            }

            field.setAccessible(true);
            if (Map.class.isAssignableFrom(field.getType())) {
                continue;
            }

            if (forceNulls && !field.getType().isPrimitive()) {
                field.set(item, null);
                if (item.hashCode() == 0) {
                    return false;
                }
            }
            instantiateNewField(item, field, false);
            if (item.hashCode() == 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tests both equals and hashCode functions
     * @param classToTest Class to test
     * @param excludes Fields for exclude
     * @return True if both tests are successful
     * @throws InstantiationException .
     * @throws IllegalAccessException .
     * @throws InvocationTargetException .
     * @throws NoSuchMethodException .
     */
    public static boolean testEqualityAndHashCode(final Class < ? > classToTest, final String... excludes)
            throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        if (!testHashCode(classToTest, excludes)) {
            return false;
        }
        return testEquality(classToTest, excludes);
    }
}
