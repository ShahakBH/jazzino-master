package com.yazino.spring.mvc.velocity;

import org.junit.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class ErrorsToolTest {

    private final ErrorsTool tool = new ErrorsTool();

    @Test
    public void shouldReturnAllFieldNames() throws Exception {
        Errors errors = new BeanPropertyBindingResult(new TestClass(), "test");
        errors.rejectValue("name", "empty");
        errors.rejectValue("inner", "invalid");
        errors.reject("service.failure");
        Set<String> fields = tool.fieldsWithErrors(errors);
        assertEquals(2, fields.size());
        assertContains(fields, "name", "inner");
    }

    @Test
    public void shouldReturnErrorsAssociatedWithField() throws Exception {
        Errors errors = new BeanPropertyBindingResult(new TestClass(), "test");
        errors.rejectValue("name", "empty");
        errors.rejectValue("name", "invalid");
        errors.rejectValue("name", "unknown");
        errors.rejectValue("inner", "invalid");
        Set<FieldError> fieldErrors = tool.fieldErrors("name", errors);
        assertEquals(3, fieldErrors.size());
        Set<String> codes = new HashSet<String>();
        for (FieldError fieldError : fieldErrors) {
            assertEquals("name", fieldError.getField());
            codes.add(fieldError.getCode());
        }
        assertContains(codes, "empty", "invalid", "unknown");
    }

    @Test
    public void shouldReturnTrueIfFieldHasErrors() throws Exception {
        Errors errors = new BeanPropertyBindingResult(new TestClass(), "test");
        errors.rejectValue("name", "empty");
        errors.rejectValue("name", "invalid");
        errors.rejectValue("name", "unknown");
        errors.rejectValue("inner", "invalid");
        assertTrue(tool.hasFieldErrors("name", errors));
        assertTrue(tool.hasFieldErrors("inner", errors));
    }

    @Test
    public void shouldReturnFalseIfFieldHasNoErrors() throws Exception {
        Errors errors = new BeanPropertyBindingResult(new TestClass(), "test");
        errors.rejectValue("name", "empty");
        errors.rejectValue("name", "invalid");
        errors.rejectValue("name", "unknown");
        assertTrue(tool.hasFieldErrors("name", errors));
        assertFalse(tool.hasFieldErrors("inner", errors));
    }

    @Test
    public void shouldReturnTrueIfHasGlobalErrors() throws Exception {
        Errors errors = new BeanPropertyBindingResult(new TestClass(), "test");
        errors.reject("foo");
        assertTrue(tool.hasGlobalErrors(errors));
    }

    @Test
    public void shouldReturnFalseIfNoGlobalErrors() throws Exception {
        Errors errors = new BeanPropertyBindingResult(new TestClass(), "test");
        assertFalse(tool.hasGlobalErrors(errors));
    }

    @Test
    public void shouldReturnAllGlobalErrors() throws Exception {
        Errors errors = new BeanPropertyBindingResult(new TestClass(), "test");
        errors.reject("service.failure");
        errors.reject("service.balls.up");
        Set<ObjectError> globalErrors = tool.globalErrors(errors);
        assertEquals(2, globalErrors.size());
        Set<String> codes = new HashSet<String>();
        for (ObjectError error : globalErrors) {
            codes.add(error.getCode());
        }
        assertContains(codes, "service.failure", "service.balls.up");
    }

    private static void assertContains(Set<String> set, String... values) {
        for (String value : values) {
            assertTrue(set.contains(value));
        }
    }

    public static class TestClass {

        private String name = "";
        private TestInnerClass inner;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public TestInnerClass getInner() {
            return inner;
        }

        public void setInner(TestInnerClass inner) {
            this.inner = inner;
        }
    }

    public static class TestInnerClass {
        private int count = 0;

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }
    }
}
