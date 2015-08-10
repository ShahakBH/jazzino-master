package com.yazino.bi.operations.persistence;

import com.yazino.platform.Platform;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;
import com.yazino.bi.operations.security.UserAccess;
import com.yazino.bi.operations.security.UserRole;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.ImmutableSet.of;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static com.yazino.bi.operations.security.UserRole.*;
import static strata.server.test.helpers.Matchers.hasAllEnumerated;
import static strata.server.test.helpers.Matchers.inList;

@ContextConfiguration
@DirtiesContext
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@TransactionConfiguration
public class JdbcUserAdministrationDaoIntegrationTest {
    @Autowired
    @Qualifier("authJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserAdministrationDao underTest;

    private Set<UserRole> rights3;
    private Set<UserRole> rights1;
    private Set<UserRole> rights2;
    private Set<Platform> pf1;

    @Before
    public void init() {
        cleanupTestUsers();

        rights3 = of(ROLE_ROOT, ROLE_MANAGEMENT);
        underTest.addUser("u3", "pw3", "user 3", rights3, null);
        rights1 = of(ROLE_SUPPORT);
        pf1 = of(Platform.FACEBOOK_CANVAS, Platform.WEB);
        underTest.addUser("u1", "pw1", "user 1", rights1, pf1);
        rights2 = new HashSet<UserRole>();
        underTest.addUser("u2", "pw2", "user 2", rights2, null);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldListUsers() throws SQLException {
        // GIVEN we have three users added to the database

        // WHEN querying for the user list
        final List<UserAccess> result = underTest.getFullUserList();

        // THEN each user object in the list is correctly filled
        final UserAccess u1 = new UserAccess();
        u1.setUserName("u1");
        u1.setRealName("user 1");
        final UserAccess u2 = new UserAccess();
        u2.setUserName("u2");
        u2.setRealName("user 2");
        final UserAccess u3 = new UserAccess();
        u3.setUserName("u3");
        u3.setRealName("user 3");

        final Matcher<Iterable<UserAccess>> resultMatcher = hasItems(
                Matchers.allOf(UserAccessMatchers.hasSameNames(u1), UserAccessMatchers.rightsMatch(rights1),
                        UserAccessMatchers.platformsMatchForUser(pf1)),
                Matchers.allOf(UserAccessMatchers.hasSameNames(u2), UserAccessMatchers.rightsMatch(rights2)),
                Matchers.allOf(UserAccessMatchers.hasSameNames(u3), UserAccessMatchers.rightsMatch(rights3)));
        assertThat(result, resultMatcher);
        MatcherAssert.assertThat(UserAccessMatchers.hasId("u1"), inList(result).isBefore(UserAccessMatchers.hasId("u3")));

        // AND the cleanup is correctly done for these users
        shouldCleanUpUserList();
    }

    @SuppressWarnings("unchecked")
    private void shouldCleanUpUserList() {
        // GIVEN users are removed from the database
        cleanupTestUsers();

        // WHEN getting the list of users
        final List<UserAccess> result = underTest.getFullUserList();

        // THEN the resulting list doesn't contain any of these users
        assertThat(result,
                not(Matchers.containsInAnyOrder(UserAccessMatchers.hasId("u1"), UserAccessMatchers.hasId("u2"), UserAccessMatchers.hasId("u3"))));
    }

    @After
    public void cleanupTestUsers() {
        try {
            jdbcTemplate
                    .update("ALTER TABLE OPERATIONS_USER DROP COLUMN ROLES");
        } catch (final BadSqlGrammarException x) {
            // No action, this is the schema hotfix
        }

        underTest.removeUser("u1");
        underTest.removeUser("u2");
        underTest.removeUser("u3");
    }

    @Test
    public void shouldBeAbleToVerifyPasswords() {
        // GIVEN the initial users

        // WHEN verifying the UID and password
        final boolean result = underTest.checkUser("u1", "pw1");

        // THEN the result matches the expectations
        assertThat(result, is(true));
    }

    @Test
    public void shouldRefuseWrongPasswords() {
        // GIVEN the initial users

        // WHEN verifying the UID and password
        final boolean result = underTest.checkUser("u1", "pw2");

        // THEN the result matches the expectations
        assertThat(result, is(false));
    }

    @Test
    public void shouldRefuseWrongUids() {
        // GIVEN the initial users

        // WHEN verifying the UID and password
        final boolean result = underTest.checkUser("u5", "pw1");

        // THEN the result matches the expectations
        assertThat(result, is(false));
    }

    @Test
    public void shouldNotAllowDuplicateUserIds() throws SQLException {
        // GIVEN we have three users added to the database

        // WHEN querying for the user list
        final boolean result = underTest.addUser("u1", "wrong", "noname",
                rights1, null);

        // THEN each user object in the list is correctly filled
        assertThat(result, is(false));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldUpdateUsers() throws SQLException {
        // GIVEN an updated user record
        final Set<UserRole> newRights = new HashSet<UserRole>();
        newRights.add(ROLE_MANAGEMENT);
        final Set<Platform> newPlatforms = of(Platform.IOS);
        underTest.updateUser("u1", "Password", "New name", newRights,
                newPlatforms);

        // WHEN querying for the user list
        final List<UserAccess> result = underTest.getFullUserList();

        // THEN each user object in the list is correctly filled
        final UserAccess testUser = new UserAccess();
        testUser.setUserName("u1");
        testUser.setRealName("New name");
        final Matcher<Iterable<UserAccess>> resultMatcher = hasItems(Matchers.allOf(
                UserAccessMatchers.hasSameNames(testUser), UserAccessMatchers.rightsMatch(newRights),
                UserAccessMatchers.platformsMatchForUser(newPlatforms)));
        assertThat(result, resultMatcher);
        assertThat(underTest.checkUser("u1", "Password"), is(true));
    }

    @Test
    public void shouldNotUpdatePasswordWithEmptyOne() throws SQLException {
        // GIVEN an updated user record
        underTest.updateUser("u1", "", "New name", rights1, pf1);

        // WHEN checking the password
        final boolean result = underTest.checkUser("u1", "pw1");

        // THEN the check is correct
        assertThat(result, is(true));
    }

    @Test
    public void shouldGetPlatformsForUser() {
        // GIVEN a user in the database

        // WHEN checking the password
        final Set<Platform> platforms = underTest.getPlatformsForUser("u1");

        // THEN the check is correct
        MatcherAssert.assertThat(platforms, UserAccessMatchers.platformsMatch(pf1));
    }

    @Test
    public void shouldGetPlatformsForUserWithNoRestrictions() {
        // GIVEN a user in the database

        // WHEN checking the password
        final Set<Platform> platforms = underTest.getPlatformsForUser("u2");

        // THEN the check is correct
        assertThat(platforms, hasAllEnumerated(Platform.class));
    }

    @Test
    public void shouldUpdateUserWithEmptyPlatformsList() {
        // GIVEN a user created without platforms in the list
        underTest.addUser("test", "pwd", "rn", of(ROLE_ROOT, ROLE_MANAGEMENT),
                of(Platform.WEB));

        // WHEN modifying the user
        underTest.updateUser("test", "pwd", "rn",
                of(ROLE_ROOT, ROLE_MANAGEMENT), null);

        // THEN the modification should be successful with the correct platforms
        // listed
        final Set<Platform> platforms = underTest.getPlatformsForUser("test");
        assertThat(platforms, hasAllEnumerated(Platform.class));

        // AND we clean up
        underTest.removeUser("test");
    }
}
