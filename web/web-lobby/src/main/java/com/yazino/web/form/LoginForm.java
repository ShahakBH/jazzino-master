package com.yazino.web.form;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.Serializable;


public class LoginForm implements Serializable {
    private static final long serialVersionUID = 7241530510766766625L;
    private String email;
    private String password;
    private String _spring_security_remember_me = "checked";

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public String get_spring_security_remember_me() {
        return _spring_security_remember_me;
    }

    public void set_spring_security_remember_me(final String _spring_security_remember_me) {
        this._spring_security_remember_me = _spring_security_remember_me;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }

        if (this == obj) {
            return true;
        }

        if (obj.getClass() != getClass()) {
            return false;
        }

        final LoginForm rhs = (LoginForm) obj;
        return new EqualsBuilder()
                .append(email, rhs.email)
                .append(password, rhs.password)
                .append(_spring_security_remember_me, rhs._spring_security_remember_me)
                .isEquals();
    }


    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(email)
                .append(password)
                .append(_spring_security_remember_me)
                .toHashCode();
    }

}
