package com.yazino.bi.operations.persistence;

import com.yazino.platform.Platform;
import com.yazino.bi.operations.security.UserAccess;
import com.yazino.bi.operations.security.UserRole;

import java.util.List;
import java.util.Set;

/**
 * Managing the user access data
 */
public interface UserAdministrationDao {

    /**
     * Gets the full list of users present in the system
     *
     * @return List of user access information
     */
    List<UserAccess> getFullUserList();

    /**
     * Creates a new user in the database
     *
     * @param userName User name
     * @param password User's password
     * @param realName Human-readable user name
     * @param rights   Set of security roles for the user
     * @return True if the user can be created, false on error (dupllicate user)
     */
    boolean addUser(String userName,
            String password,
            String realName,
            Set<UserRole> rights,
            Set<Platform> platforms);

    /**
     * Removes a user with the given ID
     *
     * @param id User's ID
     */
    void removeUser(String id);

    /**
     * Checks the user ID/password pair
     *
     * @param uid User ID
     * @param pw  Password, unencrypted
     * @return True if the uid/password pair exists in the database
     */
    boolean checkUser(String uid, String pw);

    /**
     * Updates a user with the given ID in the database
     *
     * @param userName User name
     * @param password User's password
     * @param realName Human-readable user name
     * @param rights   Set of security roles for the user
     * @param newPlatforms Set of platforms allowed to the user
     */
    void updateUser(String userName,
            String password,
            String realName,
            Set<UserRole> rights,
            Set<Platform> newPlatforms);

    Set<Platform> getPlatformsForUser(String currentUser);

}
