package com.yazino.web.form;

public interface RegistrationForm {
    boolean getTermsAndConditions();

    String getEmail();

    String getPassword();

    String getDisplayName();

    String getAvatarURL();

    Boolean getOptIn();


}
