package com.yazino.bi.operations.controller;

import com.yazino.platform.Platform;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import com.yazino.bi.operations.persistence.UserAdministrationDao;
import com.yazino.bi.operations.util.StringFormatHelper;
import com.yazino.bi.operations.security.UserAccess;
import com.yazino.bi.operations.security.UserManagementCommand;
import com.yazino.bi.operations.security.UserRole;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Controls the user access administration
 */
@Controller
public class UserAdministrationController {

    static final String COMMAND_OBJECT_NAME = "command";
    static final String ADMIN_VIEW_NAME = "userAdmin";

    static final String UPDATE_COMMAND = "UPDATE";
    static final String UPDATE_COMMIT_COMMAND = "UPDATE_COMMIT";

    static final String INSERT_COMMAND = "INSERT";
    static final String INSERT_COMMIT_COMMAND = "INSERT_COMMIT";

    static final String DELETE_COMMAND = "DELETE";

    private final UserAdministrationDao dao;

    /**
     * Creates the controller
     *
     * @param dao      DAO to use
     */
    @Autowired(required = true)
    public UserAdministrationController(final UserAdministrationDao dao) {
        this.dao = dao;
    }

    /**
     * Returns the map of available user roles
     *
     * @return Map to be used in the group controls
     */
    @ModelAttribute("allRoles")
    public Map<String, String> getAllRoles() {
        final Map<String, String> roles = new LinkedHashMap<String, String>();
        for (final UserRole role : UserRole.values()) {
            roles.put(role.name(), role.getRoleName());
        }
        return roles;
    }

    @ModelAttribute("allPlatforms")
    public Map<String, String> getAllPlatforms() {
        final Map<String, String> roles = new LinkedHashMap<String, String>();
        for (final Platform platform : Platform.values()) {
            roles.put(platform.name(), platform.name());
        }
        return roles;
    }

    /**
     * Does the initial load of the users list
     *
     * @return Model and view of the user admin
     */
    @RequestMapping(value = {"/" + ADMIN_VIEW_NAME, ADMIN_VIEW_NAME, "*/" + ADMIN_VIEW_NAME}, method = {RequestMethod.GET})
    public ModelAndView initialList() {
        final UserManagementCommand command = new UserManagementCommand();
        fillCommandWithReloadedUsers(command);
        return new ModelAndView(ADMIN_VIEW_NAME, COMMAND_OBJECT_NAME, command);
    }

    /**
     * Respond to posting the user form
     *
     * @param command Command to treat
     * @return Model and view of the user admin
     */
    @RequestMapping(value = {"/" + ADMIN_VIEW_NAME, ADMIN_VIEW_NAME, "*/" + ADMIN_VIEW_NAME}, method = {RequestMethod.POST})
    public ModelAndView manageUsers(final UserManagementCommand command) {
        command.setErrorMessage("");
        final String req = command.getReq();
        if (UPDATE_COMMIT_COMMAND.equals(req)) {
            updateUser(command);
        } else if (INSERT_COMMIT_COMMAND.equals(req)) {
            insertUser(command);
        } else if (DELETE_COMMAND.equals(req)) {
            deleteUser(command);
        }
        fillCommandWithReloadedUsers(command);
        if (UPDATE_COMMAND.equals(req)) {
            prepareUserForUpdate(command);
        } else if (INSERT_COMMAND.equals(req)) {
            prepareUserForInsert(command);
        }

        return new ModelAndView(ADMIN_VIEW_NAME, COMMAND_OBJECT_NAME, command);
    }

    /**
     * Prepares the form for the insert operation
     *
     * @param command Managed form
     */
    private void prepareUserForInsert(final UserManagementCommand command) {
        command.setReq(INSERT_COMMIT_COMMAND);
    }

    /**
     * Update an existing user's data
     *
     * @param command Data source for update
     */
    private void updateUser(final UserManagementCommand command) {
        final UserAccess activeUser = command.getActiveUser();
        dao.updateUser(command.getActiveId(), activeUser.getPassword(), activeUser.getRealName(),
                activeUser.getRoles(), activeUser.getPlatforms());
        command.setReq("");
        command.setActiveId("");
    }

    /**
     * Insert a new user's data
     *
     * @param command Data source for insert
     */
    private void insertUser(final UserManagementCommand command) {
        final UserAccess activeUser = command.getActiveUser();
        if (dao.addUser(activeUser.getUserName(), activeUser.getPassword(), activeUser.getRealName(),
                activeUser.getRoles(), activeUser.getPlatforms())) {
            command.setReq("");
            command.setActiveId("");
        } else {
            command.setErrorMessage("Duplicate username");
        }
    }

    /**
     * Remove a user's data
     *
     * @param command Data source for insert
     */
    private void deleteUser(final UserManagementCommand command) {
        dao.removeUser(command.getActiveId());
        command.setReq("");
        command.setActiveId("");
    }

    /**
     * Prepares the form for the update operation
     *
     * @param command Managed form
     */
    private void prepareUserForUpdate(final UserManagementCommand command) {
        command.setReq(UPDATE_COMMIT_COMMAND);
        UserAccess userToActivate = null;
        for (final UserAccess user : command.getUsers()) {
            if (command.getActiveId().equals(user.getUserName())) {
                userToActivate = user;
                break;
            }
        }
        command.setActiveUser(userToActivate);
        command.getUsers().remove(userToActivate);
    }

    /**
     * Builds the command object by reloading the full user list
     *
     * @param command Command object to fill
     */
    private void fillCommandWithReloadedUsers(final UserManagementCommand command) {
        command.setUsers(dao.getFullUserList());
    }

    /**
     * Puts the formatter to the model
     *
     * @return Format helper object
     */
    @ModelAttribute("formatter")
    public StringFormatHelper getFormatter() {
        return StringFormatHelper.getInstance();
    }
}
