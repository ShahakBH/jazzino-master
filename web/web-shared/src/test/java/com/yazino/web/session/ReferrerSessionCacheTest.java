package com.yazino.web.session;

import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ReferrerSessionCacheTest {

    private HttpServletRequest request;
    private ReferrerSessionCache underTest;

    @Before
    public void setUp() {
        request = mock(HttpServletRequest.class);
        underTest = new ReferrerSessionCache("bookmarks");
    }

    @Test
    public void shouldResolveFromRef() {
        when(request.getParameter("ref")).thenReturn("abc");
        underTest.resolveReferrerFrom(request);
        assertEquals("abc", underTest.getReferrer());
    }

    @Test
    public void shouldIgnoreEmptyRef() {
        when(request.getParameter("ref")).thenReturn("");
        underTest.resolveReferrerFrom(request);
        assertNull(underTest.getReferrer());
    }

    @Test
    public void shouldIgnoreInvalidRef() {
        // this is an example taken from prod, when %%% is passed as a ref
        when(request.getParameter("ref")).thenThrow(new IllegalArgumentException("!hex:25"));

        underTest.resolveReferrerFrom(request);

        assertNull(underTest.getReferrer());
    }

    @Test
    public void shouldResolveFromFbRefIgnoringFacebookKnownRefs() {
        when(request.getParameter("ref")).thenReturn("bookmarks");
        when(request.getParameter("fb_ref")).thenReturn("abc");
        underTest.resolveReferrerFrom(request);
        assertEquals("abc", underTest.getReferrer());
    }

    @Test
    public void shouldResolveFromFbRef() {
        when(request.getParameter("fb_ref")).thenReturn("abc");
        underTest.resolveReferrerFrom(request);
        assertEquals("abc", underTest.getReferrer());
    }

    @Test
    public void shouldIgnoreEmptyFbRef() {
        when(request.getParameter("fb_ref")).thenReturn("");
        underTest.resolveReferrerFrom(request);
        assertNull(underTest.getReferrer());
    }

    @Test
    public void shouldResolveFromSourceId() {
        when(request.getParameter("sourceId")).thenReturn("abc");
        underTest.resolveReferrerFrom(request);
        assertEquals("abc", underTest.getReferrer());
    }

    @Test
    public void shouldResolveFromSourceIdIgnoringFacebookKnownRefs() {
        when(request.getParameter("ref")).thenReturn("bookmarks");
        when(request.getParameter("sourceId")).thenReturn("abc");
        underTest.resolveReferrerFrom(request);
        assertEquals("abc", underTest.getReferrer());
    }

    @Test
    public void shouldIgnoreEmptySourceId() {
        when(request.getParameter("sourceId")).thenReturn("");
        underTest.resolveReferrerFrom(request);
        assertNull(underTest.getReferrer());
    }

    @Test
    public void shouldReturnNullIfNoRelevantParameterPresent() {
        underTest.resolveReferrerFrom(request);
        assertNull(underTest.getReferrer());
    }

    @Test
    public void shouldInvalidateExistingReferrer() {
        underTest.setReferrer("aReferrer");

        underTest.invalidate();

        assertNull(underTest.getReferrer());
    }

    @Test
    public void shouldResolveFromFbSource() {
        when(request.getParameter("fb_source")).thenReturn("appcenter");
        underTest.resolveReferrerFrom(request);
        assertEquals("appcenter", underTest.getReferrer());
    }

    @Test
    public void shouldIgnoreEmptyFbSource() {
        when(request.getParameter("fb_source")).thenReturn("");
        underTest.resolveReferrerFrom(request);
        assertNull(underTest.getReferrer());
    }

    @Test
    public void shouldReturnFacebookRefIfNothingElseIsPresent() {
        when(request.getParameter("ref")).thenReturn("bookmarks");
        when(request.getParameter("fb_ref")).thenReturn("");
        when(request.getParameter("sourceId")).thenReturn("");
        when(request.getParameter("fb_source")).thenReturn("");
        underTest.resolveReferrerFrom(request);
        assertEquals("bookmarks", underTest.getReferrer());
    }
}
