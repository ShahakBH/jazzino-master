package com.yazino.web.controller;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.http.HttpServletRequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ErrorControllerTest {
    @Mock
    private HttpServletRequest request;

    private ErrorController underTest;

    @Before
    public void setUp() {
        when(request.getHeader("Accept")).thenReturn("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");

        underTest = new ErrorController();
    }

    @Test
    public void aNotFoundErrorRedirectsToTheNotFoundViewWhenTheClientAcceptsHTML() {
        assertThat(underTest.processFileNotFound(request), is(equalTo("error404")));
    }

    @Test
    public void aNotFoundErrorRedirectsToTheNotFoundViewWhenTheClientHasNoAcceptHeader() {
        when(request.getHeader("Accept")).thenReturn(null);

        assertThat(underTest.processFileNotFound(request), is(equalTo("error404")));
    }

    @Test
    public void aNotFoundErrorRedirectsToTheNotFoundViewWhenTheClientAcceptsWildcardTypes() {
        when(request.getHeader("Accept")).thenReturn("*/*");

        assertThat(underTest.processFileNotFound(request), is(equalTo("error404")));
    }

    @Test
    public void aNotFoundErrorReturnsNoContentWhenTheClientDoesNotAcceptHTMLOrWildcards() {
        when(request.getHeader("Accept")).thenReturn("application/json");

        assertThat(underTest.processFileNotFound(request), is(nullValue()));
    }

    @Test
    public void aSessionExpiredErrorRedirectsToTheUnauthorisedViewWhenTheClientAcceptsHTML() {
        assertThat(underTest.processSessionExpired(request), is(equalTo("error401")));
    }

    @Test
    public void aSessionExpiredErrorRedirectsToTheUnauthorisedViewWhenTheClientHasNoAcceptHeader() {
        when(request.getHeader("Accept")).thenReturn(null);

        assertThat(underTest.processSessionExpired(request), is(equalTo("error401")));
    }

    @Test
    public void aSessionExpiredErrorRedirectsToTheUnauthorisedViewWhenTheClientAcceptsWildcardTypes() {
        when(request.getHeader("Accept")).thenReturn("*/*");

        assertThat(underTest.processSessionExpired(request), is(equalTo("error401")));
    }

    @Test
    public void aSessionExpiredErrorReturnsNoContentWhenTheClientDoesNotAcceptHTMLOrWildcards() {
        when(request.getHeader("Accept")).thenReturn("application/json");

        assertThat(underTest.processSessionExpired(request), is(nullValue()));
    }

    @Test
    public void anInternalErrorRedirectsToTheInternalErrorViewWhenTheClientAcceptsHTML() {
        assertThat(underTest.processInternalError(request), is(equalTo("error500")));
    }

    @Test
    public void anInternalErrorRedirectsToTheInternalErrorViewWhenTheClientHasNoAcceptHeader() {
        when(request.getHeader("Accept")).thenReturn(null);

        assertThat(underTest.processInternalError(request), is(equalTo("error500")));
    }

    @Test
    public void anInternalErrorRedirectsToTheInternalErrorViewWhenTheClientAcceptsWildcardTypes() {
        when(request.getHeader("Accept")).thenReturn("*/*");

        assertThat(underTest.processInternalError(request), is(equalTo("error500")));
    }

    @Test
    public void anInternalErrorReturnsNoContentWhenTheClientDoesNotAcceptHTMLOrWildcards() {
        when(request.getHeader("Accept")).thenReturn("application/json");

        assertThat(underTest.processInternalError(request), is(nullValue()));
    }

}
