package com.yazino.bi.operations.util;

import com.yazino.platform.Platform;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import com.yazino.bi.operations.persistence.UserAdministrationDao;

import java.util.Collections;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;

@RunWith(MockitoJUnitRunner.class)
public class SecurityInformationHelperTest {
    private SecurityInformationHelper underTest;

    @Mock
    private UserAdministrationDao dao;

    @Before
    public void init() {
        underTest = new SecurityInformationHelper(dao);
    }

    @Test
    public void shouldTranslateAccessiblePlatofms() {
        // GIVEN a user name set for the authentication
        final Authentication authentication =
                new TestingAuthenticationToken("user", "blabla", Collections.<GrantedAuthority>emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // AND the DAO returns a list of platforms
        final Set<Platform> platforms = newHashSet(Platform.WEB);
        given(dao.getPlatformsForUser("user")).willReturn(platforms);

        // WHEN requesting the list for a given user
        final Set<Platform> retval = underTest.getAccessiblePlatforms();

        // THEN the returned list matches the expectations
        assertThat(retval, is(platforms));
    }

    @Test
    public void shouldReturnEmpthListByDefault() {
        // GIVEN there is a test authentication context
        SecurityContextHolder.getContext().setAuthentication(null);

        // WHEN getting the list of current access roles
        final Set<String> accessRoles = underTest.getAccessRoles();

        // THEN the role set is got back
        assertThat(accessRoles.size(), is(0));
    }

    @Test
    public void shouldReturnCorrectAuthorities() {
        // GIVEN there is a test authentication context
        final String roleName = "ROLE1";
        final Authentication authentication =
                new TestingAuthenticationToken("user", "blabla",
                        asList((GrantedAuthority) new SimpleGrantedAuthority(roleName)));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // WHEN getting the list of current access roles
        final Set<String> accessRoles = underTest.getAccessRoles();

        // THEN the role set is got back
        assertThat(accessRoles, hasItem(roleName));
    }

    @Test
    public void shouldReturnUserName() {
        // GIVEN there is a test authentication context
        final Authentication authentication =
                new TestingAuthenticationToken("user", "blabla", Collections.<GrantedAuthority>emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // WHEN getting the list of current access roles
        final String userName = underTest.getCurrentUser();

        // THEN the role set is got back
        assertThat(userName, is("user"));
    }

    @Test
    public void shouldReturnEmptyNameByDefault() {
        // GIVEN there is a test authentication context
        SecurityContextHolder.getContext().setAuthentication(null);

        // WHEN getting the list of current access roles
        final String userName = underTest.getCurrentUser();

        // THEN the role set is got back
        assertThat(userName, is(""));
    }
}
