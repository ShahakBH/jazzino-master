package com.yazino.platform.model;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import org.apache.commons.beanutils.PropertyUtils;
import org.junit.Test;

import java.beans.PropertyDescriptor;
import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public abstract class BaseSpaceObjectTest {

    private final Class spaceClass;

    protected BaseSpaceObjectTest(final Class spaceClass) {
        this.spaceClass = spaceClass;
    }

    @Test
    public void objectHasAnEmptyConstructor() throws NoSuchMethodException {
        assertThat(spaceClass.getConstructor(), is(not(nullValue())));
    }

    @Test
    public void emptyConstructorCanBeInvoked()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
        spaceClass.newInstance();
    }

    @SuppressWarnings({"unchecked"})
    @Test
    public void objectIsAnnotatedAsASpaceClass() {
        final Annotation annotation = spaceClass.getAnnotation(SpaceClass.class);

        assertThat(annotation, is(not(nullValue())));
    }

    @Test
    public void objectIsSerialisable()
            throws IOException, IllegalAccessException, InstantiationException, ClassNotFoundException {
        final Object originalObject = spaceClass.newInstance();

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final ObjectOutputStream out = new ObjectOutputStream(baos);
        out.writeObject(originalObject);
        out.close();

        final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        final ObjectInputStream in = new ObjectInputStream(bais);
        final Object reserialisedObject = in.readObject();

        assertThat(reserialisedObject, is(equalTo(originalObject)));
    }

    @Test
    public void objectHasNoPrimitiveProperties() {
        for (PropertyDescriptor descriptor : PropertyUtils.getPropertyDescriptors(spaceClass)) {
            if (descriptor.getPropertyType().isPrimitive()) {
                fail("Property " + descriptor.getName() + " is primitive");
            }
        }
    }

    @Test
    public void propertiesAreReadWrite() {
        for (PropertyDescriptor descriptor : PropertyUtils.getPropertyDescriptors(spaceClass)) {
            if (isSystemProperty(descriptor.getName())) {
                continue;
            }

            if (descriptor.getReadMethod() == null) {
                fail("Property " + descriptor.getName() + " is write-only");
            }
            if (descriptor.getWriteMethod() == null) {
                fail("Property " + descriptor.getName() + " is read-only");
            }
        }
    }

    @Test
    public void propertiesAreNullWhenEmptyConstructorIsUsed()
            throws IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
        final Map propertyValues = PropertyUtils.describe(spaceClass.newInstance());
        for (Object propertyName : propertyValues.keySet()) {
            if (isSystemProperty(propertyName.toString())) {
                continue;
            }

            if (propertyValues.get(propertyName) != null) {
                fail("Property value is not null: " + propertyName);
            }
        }
    }

    @Test
    public void objectHasAnIdDefined() {
        for (Method method : spaceClass.getMethods()) {
            for (Annotation annotation : method.getAnnotations()) {
                if (annotation.annotationType().equals(SpaceId.class)) {
                    return;
                }
            }
        }

        fail("No method was annotated with SpaceId");
    }

    @Test
    public void objectHasEqualityCheck() throws IllegalAccessException, InstantiationException {
        final Object firstObject = spaceClass.newInstance();
        final Object secondObject = spaceClass.newInstance();

        assertThat(firstObject, is(equalTo(secondObject)));
    }

    @Test
    public void objectHasHashCode() throws IllegalAccessException, InstantiationException {
        final Object firstObject = spaceClass.newInstance();
        final Object secondObject = spaceClass.newInstance();

        assertThat(firstObject.hashCode(), is(equalTo(secondObject.hashCode())));
    }

    private boolean isSystemProperty(final String propertyName) {
        return propertyName.equals("class");
    }

}
