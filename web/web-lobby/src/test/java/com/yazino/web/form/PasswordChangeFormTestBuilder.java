package com.yazino.web.form;

import com.yazino.platform.player.PasswordChangeRequest;

import java.math.BigDecimal;

public class PasswordChangeFormTestBuilder {

    public static final String CONFIRM_PASSWORD = "confirmPassword";
    public static final String NEW_PASSWORD = "newPassword";
    public static final String CURRENT_PASSWORD = "currentPassword";
    public static final BigDecimal PLAYER_ID = BigDecimal.ONE;

    private String currentPassword;
    private String newPassword;
    private String confirmNewPassword;
    private BigDecimal playerId;

    public PasswordChangeFormTestBuilder() {
        this(CURRENT_PASSWORD, NEW_PASSWORD, CONFIRM_PASSWORD, PLAYER_ID);
    }

    public PasswordChangeFormTestBuilder(final String currentPassword, final String newPassword, final String confirmNewPassword, final BigDecimal playerId) {
        this.currentPassword = currentPassword;
        this.newPassword = newPassword;
        this.confirmNewPassword = confirmNewPassword;
        this.playerId = playerId;
    }

    public PasswordChangeFormTestBuilder(final PasswordChangeRequest passwordChangeForm) {
        this(passwordChangeForm.getCurrentPassword(),
                passwordChangeForm.getNewPassword(),
                passwordChangeForm.getConfirmNewPassword(),
                passwordChangeForm.getPlayerId());
    }

    public PasswordChangeFormTestBuilder withCurrentPassword(final String newCurrentPassword) {
        this.currentPassword = newCurrentPassword;
        return this;
    }

    public PasswordChangeFormTestBuilder withNewPassword(final String newNewPassword) {
        this.newPassword = newNewPassword;
        return this;
    }

    public PasswordChangeFormTestBuilder withConfirmNewPassword(final String newConfirmNewPassword) {
        this.confirmNewPassword = newConfirmNewPassword;
        return this;
    }

    public PasswordChangeFormTestBuilder withPlayerId(final BigDecimal playerId) {
        this.playerId = playerId;
        return this;
    }

    public PasswordChangeRequest build() {
        return new PasswordChangeRequest(currentPassword, newPassword, confirmNewPassword, playerId);
    }
}
