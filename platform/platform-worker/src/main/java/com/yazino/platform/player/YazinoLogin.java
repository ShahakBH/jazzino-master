package com.yazino.platform.player;

import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notBlank;
import static org.apache.commons.lang3.Validate.notNull;

public class YazinoLogin implements Serializable {
    private static final long serialVersionUID = 7241530510766766625L;

    private final BigDecimal playerId;
    private final String email;
    private final String passwordHash;
    private final PasswordType passwordType;
    private final byte[] salt;
    private final int loginAttempts;

    public YazinoLogin(final BigDecimal playerId,
                       final String email,
                       final String passwordHash,
                       final PasswordType passwordType,
                       final byte[] salt,
                       final int loginAttempts) {
        notBlank(email, "email may not be null/blank");
        notBlank(passwordHash, "passwordHash may not be null/blank");
        notNull(passwordType, "passwordType may not be null");

        this.playerId = playerId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.passwordType = passwordType;
        this.salt = salt;
        this.loginAttempts = loginAttempts;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public PasswordType getPasswordType() {
        return passwordType;
    }

    public int getLoginAttempts() {
        return loginAttempts;
    }

    public static YazinoLoginBuilder withPlayerId(final BigDecimal playerId) {
        return new YazinoLoginBuilder(playerId);
    }

    public static YazinoLoginBuilder copy(final YazinoLogin yazinoLogin) {
        notNull(yazinoLogin, "yazinoLogin may not be null");
        return new YazinoLoginBuilder(yazinoLogin);
    }

    public byte[] getSalt() {
        return salt;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        final YazinoLogin rhs = (YazinoLogin) obj;

        return new EqualsBuilder()
                .append(email, rhs.email)
                .append(passwordHash, rhs.passwordHash)
                .append(passwordType, rhs.passwordType)
                .append(salt, rhs.salt)
                .append(loginAttempts, rhs.loginAttempts)
                .isEquals()
                && BigDecimals.equalByComparison(playerId, rhs.playerId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(BigDecimals.strip(playerId))
                .append(email)
                .append(passwordHash)
                .append(passwordType)
                .append(salt)
                .append(loginAttempts)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(playerId)
                .append(email)
                .append(passwordHash)
                .append(passwordType)
                .append(salt)
                .append(loginAttempts)
                .toString();
    }

    public static class YazinoLoginBuilder {
        private BigDecimal playerId;
        private String email;
        private String passwordHash;
        private PasswordType passwordType = PasswordType.MD5;
        private byte[] salt;
        private int loginAttempts = 0;

        public YazinoLoginBuilder(final BigDecimal playerId) {
            this.playerId = playerId;
        }

        public YazinoLoginBuilder(final YazinoLogin yazinoLogin) {
            this.playerId = yazinoLogin.getPlayerId();
            this.email = yazinoLogin.getEmail();
            this.passwordHash = yazinoLogin.getPasswordHash();
            this.passwordType = yazinoLogin.getPasswordType();
            this.salt = yazinoLogin.getSalt();
            this.loginAttempts = yazinoLogin.getLoginAttempts();
        }

        public YazinoLoginBuilder withEmail(final String emailToAdd) {
            this.email = emailToAdd;
            return this;
        }

        public YazinoLoginBuilder withPasswordHash(final String passwordHashToAdd) {
            this.passwordHash = passwordHashToAdd;
            return this;
        }

        public YazinoLoginBuilder withPasswordType(final PasswordType passwordTypeToAdd) {
            this.passwordType = passwordTypeToAdd;
            return this;
        }

        public YazinoLoginBuilder withLoginAttempts(final int loginAttemptsToAdd) {
            this.loginAttempts = loginAttemptsToAdd;
            return this;
        }

        public YazinoLoginBuilder withSalt(final byte[] saltToAdd) {
            this.salt = saltToAdd;
            return this;
        }

        public YazinoLogin asLogin() {
            return new YazinoLogin(playerId, email, passwordHash, passwordType, salt, loginAttempts);
        }
    }
}
