package strata.server.test.helpers;

import java.lang.reflect.Method;

public class EnumAccessor<T extends Enum<?>> {
    private final Class<T> clazz;
    private final Method valuesMethod;
    private final Method nameMethod;

    public EnumAccessor(final Class<T> enumClass) {
        super();
        this.clazz = enumClass;
        try {
            this.valuesMethod = clazz.getMethod("values");
            this.nameMethod = clazz.getMethod("name");
        } catch (final Exception e) {
            // We are in presence of an enum, this should never happen
            throw new RuntimeException(
                    "Check the Java compiler, we start having enums without value() or name() method!");
        }
    }

    @SuppressWarnings("unchecked")
    public T[] values() {
        try {
            return (T[]) valuesMethod.invoke(null);
        } catch (final Exception e) {
            // There is no standard way to make it fail
            throw new RuntimeException(
                    "Check the Java compiler, we start having enums without value() or name() method!");
        }
    }

    public String name(final T object) {
        try {
            return (String) nameMethod.invoke(object);
        } catch (final Exception e) {
            // There is no standard way to make it fail
            throw new RuntimeException(
                    "Check the Java compiler, we start having enums without value() or name() method!");
        }
    }

    public String getClassName() {
        return clazz.getCanonicalName();
    }
}
