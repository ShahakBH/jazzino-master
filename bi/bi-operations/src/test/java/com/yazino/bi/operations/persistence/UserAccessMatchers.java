package com.yazino.bi.operations.persistence;

import com.yazino.platform.Platform;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import com.yazino.bi.operations.security.UserAccess;
import com.yazino.bi.operations.security.UserRole;

import java.util.Set;

public final class UserAccessMatchers {
    private UserAccessMatchers() {
    }

    public static Matcher<UserAccess> rightsMatch(final Set<UserRole> roles) {
        return new RightsMatch(roles);
    }

    private static class RightsMatch extends BaseMatcher<UserAccess> {
        private Set<UserRole> roles;

        RightsMatch(final Set<UserRole> roles) {
            this.roles = roles;
        }

        @Override
        public boolean matches(final Object other) {
            if (!(other instanceof UserAccess)) {
                return false;
            }
            final UserAccess castOther = (UserAccess) other;
            return this.roles.equals(castOther.getRoles());
        }

        @Override
        public void describeTo(final Description description) {
            description.appendText("Roles should match ").appendText(roles.toString());
        }
    }

    public static Matcher<Set<Platform>> platformsMatch(final Set<Platform> platforms) {
        return new PlatformsMatch(platforms);
    }

    private static class PlatformsMatch extends BaseMatcher<Set<Platform>> {
        private Set<Platform> platforms;

        PlatformsMatch(final Set<Platform> platforms) {
            this.platforms = platforms;
        }

        @Override
        public boolean matches(final Object other) {
            if (!(other instanceof Set)) {
                return false;
            }
            @SuppressWarnings("unchecked")
            final Set<Platform> castOther = (Set<Platform>) other;
            return this.platforms.equals(castOther);
        }

        @Override
        public void describeTo(final Description description) {
            description.appendText("Platforms should match ").appendText(platforms.toString());
        }
    }

    public static Matcher<UserAccess> platformsMatchForUser(final Set<Platform> platforms) {
        return new PlatformsMatchForUser(platforms);
    }

    private static class PlatformsMatchForUser extends BaseMatcher<UserAccess> {
        private Set<Platform> platforms;

        PlatformsMatchForUser(final Set<Platform> platforms) {
            this.platforms = platforms;
        }

        @Override
        public boolean matches(final Object other) {
            if (!(other instanceof UserAccess)) {
                return false;
            }
            final UserAccess castOther = (UserAccess) other;
            return this.platforms.equals(castOther.getPlatforms());
        }

        @Override
        public void describeTo(final Description description) {
            description.appendText("Platforms should match ").appendText(platforms.toString());
        }
    }

    public static Matcher<UserAccess> hasSameNames(final UserAccess user) {
        return new UserAccessHasSameNames(user);
    }

    private static class UserAccessHasSameNames extends BaseMatcher<UserAccess> {
        private final UserAccess user;

        UserAccessHasSameNames(final UserAccess user) {
            this.user = user;
        }

        @Override
        public boolean matches(final Object other) {
            if (!(other instanceof UserAccess)) {
                return false;
            }
            final UserAccess castOther = (UserAccess) other;
            return new EqualsBuilder().append(user.getUserName(), castOther.getUserName())
                    .append(user.getRealName(), castOther.getRealName()).isEquals();
        }

        @Override
        public void describeTo(final Description description) {
            description.appendText(user.toString());
        }
    }

    public static Matcher<UserAccess> hasId(final String id) {
        return new UserAccessHasId(id);
    }

    private static class UserAccessHasId extends BaseMatcher<UserAccess> {
        private final String id;

        UserAccessHasId(final String id) {
            this.id = id;
        }

        @Override
        public boolean matches(final Object other) {
            if (!(other instanceof UserAccess)) {
                return false;
            }
            final UserAccess castOther = (UserAccess) other;
            return id.equals(castOther.getUserName());
        }

        @Override
        public void describeTo(final Description description) {
            description.appendText("userId = ").appendText(id);
        }
    }
}
