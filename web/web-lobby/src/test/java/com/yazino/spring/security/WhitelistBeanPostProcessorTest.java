package com.yazino.spring.security;

import com.yazino.web.security.WhiteListDomain;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class WhitelistBeanPostProcessorTest {

    @Mock
    private WhiteListDomain whiteListDomain;

    private WhitelistBeanPostProcessor underTest;

    @Before
    public void setUp() {
        underTest = new WhitelistBeanPostProcessor(whiteListDomain);
    }

    @Test(expected = NullPointerException.class)
    public void aNullPointerExceptionIsThrownWhenCreatedWithANullInterceptor() {
        new WhitelistBeanPostProcessor(null);
    }

    @Test
    public void postProcessBeforeInitialisationReturnsTheSameBean() {
        final Object unprocessedBean = "aBean".intern();

        final Object processedBean = underTest.postProcessBeforeInitialization(unprocessedBean, "aBeanName");

        assertThat(processedBean, is(sameInstance(unprocessedBean)));
    }

    @Test
    public void postProcessBeforeInitialisationTakesNoActions() {
        underTest.postProcessBeforeInitialization(new TestBean(), "aBeanName");

        verifyZeroInteractions(whiteListDomain);
    }

    @Test
    public void postProcessAfterInitialisationReturnsTheSameBean() {
        final Object unprocessedBean = "aBean".intern();

        final Object processedBean = underTest.postProcessAfterInitialization(unprocessedBean, "aBeanName");

        assertThat(processedBean, is(sameInstance(unprocessedBean)));
    }


    @Test
    public void postProcessAfterInitialisationTakesNoActionsIfNoAnnotationsArePresent() {
        underTest.postProcessAfterInitialization("aBean", "aBeanName");

        verifyZeroInteractions(whiteListDomain);
    }

    @Test
    public void postProcessAfterInitialisationAddsAllPublicAccessPathsOnMethodsToTheInterceptor() {
        underTest.postProcessAfterInitialization(new TestBean(), "aBeanName");

        verify(whiteListDomain).addWhiteListedUrl("path1");
        verify(whiteListDomain).addWhiteListedUrl("path2/**/*");
        verify(whiteListDomain).addWhiteListedUrl("aPath/of/sorts/*.do");
        verify(whiteListDomain).addWhiteListedUrl("aRequest/path");
        verifyNoMoreInteractions(whiteListDomain);
    }

    @Test
    public void postProcessAfterInitialisationAddsAllPublicAccessPathsOnClassesToTheInterceptor() {
        underTest.postProcessAfterInitialization(new AnnotatedTestBean(), "aBeanName");

        verify(whiteListDomain).addWhiteListedUrl("something/**/*");
        verify(whiteListDomain).addWhiteListedUrl("path1");
        verify(whiteListDomain).addWhiteListedUrl("path2/**/*");
        verifyNoMoreInteractions(whiteListDomain);
    }

    @Test(expected = IllegalStateException.class)
    public void postProcessAfterInitialisationRejectsAnnotationsWithNoValueAndNoRequestMapping() {
        underTest.postProcessAfterInitialization(new InvalidTestBean(), "aBeanName");
    }

    private static class TestBean {
        public void anUnannotatedMethod() {

        }

        @AllowPublicAccess({"path1", "path2/**/*"})
        public void anAnnotatedMethod() {

        }

        @AllowPublicAccess("aPath/of/sorts/*.do")
        public void anotherAnnotatedMethod() {

        }

        @RequestMapping("aRequest/path")
        @AllowPublicAccess
        public void aBrieflyAnnotatedMethod() {

        }
    }

    @AllowPublicAccess("something/**/*")
    private static class AnnotatedTestBean {
        public void anUnannotatedMethod() {

        }

        @AllowPublicAccess({"path1", "path2/**/*"})
        public void anAnnotatedMethod() {

        }
    }

    private static class InvalidTestBean {
        public void anUnannotatedMethod() {

        }

        @AllowPublicAccess
        public void anAnnotatedMethod() {

        }
    }

}
