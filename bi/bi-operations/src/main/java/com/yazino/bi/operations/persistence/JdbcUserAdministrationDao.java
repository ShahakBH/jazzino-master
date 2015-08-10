package com.yazino.bi.operations.persistence;

import com.yazino.platform.Platform;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.authentication.encoding.ShaPasswordEncoder;
import org.springframework.stereotype.Repository;
import com.yazino.bi.operations.security.UserAccess;
import com.yazino.bi.operations.security.UserRole;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * JDBC implementation of the user administration DAO
 */
@Repository
public class JdbcUserAdministrationDao implements UserAdministrationDao {

    private final JdbcTemplate jdbcTemplate;

    private static final String USER_LIST_QUERY = "SELECT USERNAME,REAL_NAME FROM OPERATIONS_USER ORDER BY USERNAME";

    private static final String ROLES_LIST_QUERY = "SELECT ROLE FROM OPERATIONS_USER_ROLE WHERE USERNAME=?";

    private static final String USER_ADD_QUERY = "INSERT INTO OPERATIONS_USER(USERNAME,PASSWORD,REAL_NAME) VALUES(?,?,?)";

    private static final String USER_UPDATE_QUERY
            = "UPDATE OPERATIONS_USER SET PASSWORD = ?,REAL_NAME = ? WHERE USERNAME = ?";
    private static final String USER_UPDATE_QUERY_NO_PASSWORD
            = "UPDATE OPERATIONS_USER SET REAL_NAME = ? WHERE USERNAME = ?";

    private static final String ROLES_ADD_QUERY = "INSERT INTO OPERATIONS_USER_ROLE(USERNAME,ROLE) VALUES(?,?)";

    private static final String USER_DELETE_QUERY = "DELETE FROM OPERATIONS_USER WHERE USERNAME = ?";

    private static final String ROLES_DELETE_QUERY = "DELETE FROM OPERATIONS_USER_ROLE WHERE USERNAME = ?";

    private static final String PASSWORD_QUERY = "SELECT PASSWORD FROM OPERATIONS_USER WHERE USERNAME = ?";

    private static final String PLATFORMS_LIST_QUERY
            = "SELECT PLATFORM FROM OPERATIONS_PLATFORM WHERE USERNAME = ? ORDER BY PLATFORM";

    private static final String PLATFORMS_ADD_QUERY
            = "INSERT INTO OPERATIONS_PLATFORM(USERNAME,PLATFORM) VALUES(?,?)";

    private static final String PLATFORMS_DELETE_QUERY = "DELETE FROM OPERATIONS_PLATFORM WHERE USERNAME = ?";

    private final ShaPasswordEncoder encoder = new ShaPasswordEncoder();

    /**
     * Creates a DAO connected to persistence
     *
     * @param jdbcTemplate JDBC template connecting the DAO
     */
    @Autowired
    public JdbcUserAdministrationDao(@Qualifier("authJdbcTemplate") final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<UserAccess> getFullUserList() {
        final List<UserAccess> retval = jdbcTemplate.query(USER_LIST_QUERY,
                new RowMapper<UserAccess>() {
                    @Override
                    public UserAccess mapRow(final ResultSet rs,
                                             final int rowNum) throws SQLException {
                        final UserAccess user = new UserAccess();
                        user.setUserName(rs.getString("USERNAME"));
                        user.setRealName(rs.getString("REAL_NAME"));
                        return user;
                    }
                });
        for (final UserAccess user : retval) {
            final List<UserRole> roles = jdbcTemplate.query(ROLES_LIST_QUERY,
                    new Object[]{user.getUserName()},
                    new RowMapper<UserRole>() {
                        @Override
                        public UserRole mapRow(final ResultSet rs,
                                               final int rowNum) throws SQLException {
                            return UserRole.valueOf(rs.getString("ROLE"));
                        }
                    });
            for (final UserRole role : roles) {
                user.getRoles().add(role);
            }
            user.setPlatforms(getPlatformsForUser(user.getUserName()));
        }
        return retval;
    }

    @Override
    public boolean addUser(final String userName, final String password,
                           final String realName, final Set<UserRole> roles,
                           final Set<Platform> platforms) {
        try {
            jdbcTemplate.update(USER_ADD_QUERY, userName,
                    encoder.encodePassword(password, null), realName);
            addRolesForUser(userName, roles);
            if (platforms != null) {
                addPlatformsForUser(userName, platforms);
            }
        } catch (final DuplicateKeyException x) {
            return false;
        }
        return true;
    }

    private void addPlatformsForUser(final String userName,
                                     final Set<Platform> platforms) {
        for (final Platform platform : platforms) {
            jdbcTemplate.update(PLATFORMS_ADD_QUERY, userName, platform.name());
        }
    }

    /**
     * Persists the set of roles for a given user
     *
     * @param userName User name
     * @param roles    Roles to persist
     */
    private void addRolesForUser(final String userName,
                                 final Set<UserRole> roles) {
        for (final UserRole role : roles) {
            jdbcTemplate.update(ROLES_ADD_QUERY, userName, role.name());
        }
    }

    @Override
    public void removeUser(final String id) {
        removeUserRoles(id);
        removeUserPlatforms(id);
        jdbcTemplate.update(USER_DELETE_QUERY, id);
    }

    private void removeUserPlatforms(final String userName) {
        jdbcTemplate.update(PLATFORMS_DELETE_QUERY, userName);
    }

    @Override
    public boolean checkUser(final String uid, final String pw) {
        final String pwHash;
        try {
            pwHash = jdbcTemplate.queryForObject(PASSWORD_QUERY, String.class,
                    uid);
        } catch (final EmptyResultDataAccessException x) {
            return false;
        }
        final String checkedHash = encoder.encodePassword(pw, null);
        return checkedHash.equals(pwHash);
    }

    @Override
    public void updateUser(final String userName, final String password,
                           final String realName, final Set<UserRole> rights,
                           final Set<Platform> platforms) {
        if ("".equals(password)) {
            jdbcTemplate.update(USER_UPDATE_QUERY_NO_PASSWORD, realName,
                    userName);
        } else {
            jdbcTemplate.update(USER_UPDATE_QUERY,
                    encoder.encodePassword(password, null), realName, userName);
        }
        removeUserRoles(userName);
        removeUserPlatforms(userName);
        addRolesForUser(userName, rights);
        if (platforms == null) {
            return;
        }
        addPlatformsForUser(userName, platforms);
    }

    /**
     * Removes roles for a given user from the persistence
     *
     * @param userName Name of the user
     */
    private void removeUserRoles(final String userName) {
        jdbcTemplate.update(ROLES_DELETE_QUERY, userName);
    }

    @Override
    public Set<Platform> getPlatformsForUser(final String currentUser) {
        return jdbcTemplate.query(PLATFORMS_LIST_QUERY,
                new Object[]{currentUser},
                new ResultSetExtractor<Set<Platform>>() {
                    @Override
                    public Set<Platform> extractData(final ResultSet rs)
                            throws SQLException, DataAccessException {
                        final Set<Platform> platforms = new HashSet<Platform>();
                        while (rs.next()) {
                            final Platform platform = Platform
                                    .valueOf(rs.getString("PLATFORM"));
                            if (platform != null) {
                                platforms.add(platform);
                            }
                        }
                        if (platforms.size() > 0) {
                            return platforms;
                        }
                        for (final Platform pf : Platform.values()) {
                            platforms.add(pf);
                        }
                        return platforms;
                    }
                });
    }

}
