package com.yazino.web.security;

import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class ProtectedResourceClassifierTest {

    private final Domain publicDomain1 = mock(Domain.class);
    private final Domain publicDomain2 = mock(Domain.class);
    private final Set<Domain> publicDomains = newHashSet(publicDomain1, publicDomain2);

    private ProtectedResourceClassifier underTest;

    @Before
    public void setUp() throws Exception {
        underTest = new ProtectedResourceClassifier(publicDomains);
    }

    @Test
    public void shouldNotRequiresAuthorisationWhenUrlIsPartOfOneOrMorePublicDomains() {
        String url = "/whitelisted";
        when(publicDomain1.includesUrl(url)).thenReturn(false);        // TODO can we indicate that order is irrelevant?
        when(publicDomain2.includesUrl(url)).thenReturn(true);

        assertFalse(underTest.requiresAuthorisation(url));
    }

    @Test
    public void requiresAuthorisationWhenUrlIsNotPartOfAnyPublicDomain() {
        String url = "/whitelisted";
        when(publicDomain1.includesUrl(url)).thenReturn(false);
        when(publicDomain2.includesUrl(url)).thenReturn(false);

        assertTrue(underTest.requiresAuthorisation(url));
    }
}
