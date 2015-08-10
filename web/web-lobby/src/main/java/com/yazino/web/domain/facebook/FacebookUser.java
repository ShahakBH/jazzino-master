package com.yazino.web.domain.facebook;


import com.restfb.Facebook;
import com.restfb.types.User;

public class FacebookUser extends User {

    @Facebook
    private String locale;

    @Facebook
    private String first_name;

    @Facebook
    private String last_name;

    public String getLocale() {
        return locale;
    }

    public String getFirstName() {
        return first_name;
    }

    public String getLastName() {
        return last_name;
    }
}
