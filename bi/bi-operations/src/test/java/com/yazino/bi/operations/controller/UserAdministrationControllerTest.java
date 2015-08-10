package com.yazino.bi.operations.controller;

import com.yazino.platform.Platform;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.web.servlet.ModelAndView;
import com.yazino.bi.operations.persistence.UserAdministrationDao;
import com.yazino.bi.operations.util.StringFormatHelper;
import com.yazino.bi.operations.security.UserAccess;
import com.yazino.bi.operations.security.UserManagementCommand;
import com.yazino.bi.operations.security.UserRole;

import java.util.*;

import static com.google.common.collect.Sets.newHashSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static com.yazino.bi.operations.controller.UserAdministrationController.*;
import static strata.server.test.helpers.Matchers.hasAllEnumeratedStringValues;

@RunWith(MockitoJUnitRunner.class)
public class UserAdministrationControllerTest {
    private UserAdministrationController underTest;

    @Mock
    private UserAdministrationDao dao;

    private UserAccess user1;

    private UserAccess user2;

    @Before
    public void init() {
        underTest = new UserAdministrationController(dao);
        user1 = new UserAccess();
        user1.setUserName("u1");
        user2 = new UserAccess();
        user2.setUserName("u2");
    }

    @Test
    public void shouldListExistingUsers() {
        // GIVEN the DAO returns a list of users
        prepareListOfTwoUsers();

        // WHEN requesting the initial data from the controller
        final ModelAndView mv = underTest.initialList();
        final UserManagementCommand command = (UserManagementCommand) mv.getModelMap().get(COMMAND_OBJECT_NAME);

        // THEN a command object with the correct user list is returned
        assertThat(command.getUsers(), hasItems(user1, user2));
        assertThat(mv.getViewName(), is(ADMIN_VIEW_NAME));
    }

    private void prepareListOfTwoUsers() {
        final List<UserAccess> users = new ArrayList<UserAccess>();
        users.add(user1);
        users.add(user2);
        given(dao.getFullUserList()).willReturn(users);
    }

    @Test
    public void shouldSetUserToUpdate() {
        // GIVEN the command contains an update request
        final UserManagementCommand command = new UserManagementCommand();
        command.setReq(UPDATE_COMMAND);
        command.setActiveId("u1");

        prepareListOfTwoUsers();

        // WHEN posting a command to the controller
        final ModelAndView mv = underTest.manageUsers(command);
        final UserManagementCommand result = (UserManagementCommand) mv.getModelMap().get(COMMAND_OBJECT_NAME);

        // THEN the filtered command contains a user selected for update
        assertThat(result.getActiveUser(), is(user1));
        assertThat(result.getActiveId(), is("u1"));

        // AND the command is ready for an update-commit
        assertThat(result.getReq(), is(UPDATE_COMMIT_COMMAND));

        // AND the resulting list contains the non-edited user only
        assertThat(result.getUsers(), hasItem(user2));
        assertThat(result.getUsers(), not(hasItem(user1)));
    }

    @Test
    public void shouldFillRightsList() {
        // GIVEN the controller

        // WHEN requesting the list of access rights
        final Map<String, String> roles = underTest.getAllRoles();

        // THEN the correct map is returned
        assertThat(roles.keySet(), hasAllEnumeratedStringValues(UserRole.class));
    }

    @Test
    public void shouldFillPlatformsList() {
        // GIVEN the controller

        // WHEN requesting the list of access rights
        final Map<String, String> pf = underTest.getAllPlatforms();

        // THEN the correct map is returned
        assertThat(pf.keySet(), hasAllEnumeratedStringValues(Platform.class));
    }

    @Test
    public void shouldAcceptSubmittedUpdate() {
        // GIVEN a user information is submitted with an update command
        final UserAccess updatedUser = new UserAccess();
        updatedUser.setUserName("u1");
        updatedUser.setRealName("user mod");

        final UserManagementCommand command = new UserManagementCommand();
        command.setReq(UPDATE_COMMIT_COMMAND);
        command.setActiveUser(updatedUser);
        command.setActiveId("u1");

        // WHEN posting a command to the controller
        final ModelAndView mv = underTest.manageUsers(command);
        final UserManagementCommand result = (UserManagementCommand) mv.getModelMap().get(COMMAND_OBJECT_NAME);

        // THEN the matching user's information is updated before getting the list again
        final InOrder order = inOrder(dao);
        order.verify(dao).updateUser("u1", "", "user mod", new HashSet<UserRole>(), new HashSet<Platform>());
        order.verify(dao).getFullUserList();

        // AND the command state goes back to "neutral"
        assertThat(result.getReq(), is(""));
        assertThat(result.getActiveId(), is(""));
    }

    @Test
    public void shouldPrepareUserInsert() {
        // GIVEN the command contains an update request
        final UserManagementCommand command = new UserManagementCommand();
        command.setReq(INSERT_COMMAND);

        prepareListOfTwoUsers();

        // WHEN posting a command to the controller
        final ModelAndView mv = underTest.manageUsers(command);
        final UserManagementCommand result = (UserManagementCommand) mv.getModelMap().get(COMMAND_OBJECT_NAME);

        // THEN the filtered command contains a user selected for update
        assertThat(result.getActiveUser(), nullValue());

        // AND the command is ready for an update-commit
        assertThat(result.getReq(), is(INSERT_COMMIT_COMMAND));

        // AND the resulting list contains both userts
        assertThat(result.getUsers(), hasItems(user1, user2));
    }

    @Test
    public void shouldAcceptSubmittedInsert() {
        // GIVEN a user information is submitted with an update command
        final UserAccess newUser = new UserAccess();
        newUser.setUserName("u5");
        newUser.setRealName("user mod");
        // AND we submit the platform information to the DAO
        final Set<Platform> platforms = newHashSet(Platform.WEB, Platform.FACEBOOK_CANVAS);
        newUser.setPlatforms(platforms);

        final UserManagementCommand command = new UserManagementCommand();
        command.setReq(INSERT_COMMIT_COMMAND);
        command.setActiveUser(newUser);

        // AND the call to the DAO returns true
        given(dao.addUser(eq("u5"), eq(""), eq("user mod"), eq(new HashSet<UserRole>()), eq(platforms))).willReturn(
                true);

        // WHEN posting a command to the controller
        final ModelAndView mv = underTest.manageUsers(command);
        final UserManagementCommand result = (UserManagementCommand) mv.getModelMap().get(COMMAND_OBJECT_NAME);

        // THEN the matching user's information is updated before getting the list again
        final InOrder order = inOrder(dao);
        order.verify(dao).addUser(eq("u5"), eq(""), eq("user mod"), eq(new HashSet<UserRole>()), eq(platforms));
        order.verify(dao).getFullUserList();

        // AND the command state goes back to "neutral"
        assertThat(result.getReq(), is(""));
        assertThat(result.getActiveId(), is(""));
    }

    @Test
    public void shouldRefuseDuplicateUser() {
        // GIVEN a user information is submitted with an update command
        final UserAccess newUser = new UserAccess();
        newUser.setUserName("u1");
        newUser.setRealName("user mod");

        final UserManagementCommand command = new UserManagementCommand();
        command.setReq(INSERT_COMMIT_COMMAND);
        command.setActiveUser(newUser);

        // AND the call to the DAO returns false
        given(dao.addUser("u1", "", "user mod", new HashSet<UserRole>(), new HashSet<Platform>())).willReturn(false);

        // WHEN posting a command to the controller
        final ModelAndView mv = underTest.manageUsers(command);
        final UserManagementCommand result = (UserManagementCommand) mv.getModelMap().get(COMMAND_OBJECT_NAME);

        // THEN the command object contains the error message
        assertThat(result.getErrorMessage(), is("Duplicate username"));

        // AND the command state remains "insert commit"
        assertThat(result.getReq(), is(INSERT_COMMIT_COMMAND));
    }

    @Test
    public void shouldClearErrorMessage() {
        // GIVEN a command containing an error message
        final UserManagementCommand command = new UserManagementCommand();
        command.setErrorMessage("Error");

        // WHEN posting a command to the controller
        final ModelAndView mv = underTest.manageUsers(command);
        final UserManagementCommand result = (UserManagementCommand) mv.getModelMap().get(COMMAND_OBJECT_NAME);

        // THEN the resulting command contains no more error messages
        assertThat(result.getErrorMessage(), is(""));
    }

    @Test
    public void shouldAcceptDeletion() {
        // GIVEN a user information is submitted with an update command
        final UserManagementCommand command = new UserManagementCommand();
        command.setReq(DELETE_COMMAND);
        command.setActiveId("u1");

        // WHEN posting a command to the controller
        final ModelAndView mv = underTest.manageUsers(command);
        final UserManagementCommand result = (UserManagementCommand) mv.getModelMap().get(COMMAND_OBJECT_NAME);

        // THEN the matching user's information is updated before getting the list again
        final InOrder order = inOrder(dao);
        order.verify(dao).removeUser("u1");
        order.verify(dao).getFullUserList();

        // AND the command state goes back to "neutral"
        assertThat(result.getReq(), is(""));
        assertThat(result.getActiveId(), is(""));
    }

    @Test
    public void shouldHaveFormatter() {
        // GIVEN the controller

        // WHEN getting the formatter
        final StringFormatHelper formatter = underTest.getFormatter();

        // THEN the result is a unique instance of the needed object
        assertThat(formatter, is(StringFormatHelper.getInstance()));
    }
}
