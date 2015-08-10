/*global YAZINO, it, spyOn, FB, expect, describe, jasmine, beforeEach, afterEach, jQuery, strictEqual, $, document, localStorageMock, window, swfobject, alert, clearInterval, clearTimeout, console, setInterval, setTimeout, window, parent, StrataPartnerApi, FB, waitsFor, runs */

describe('Login and Registration Form', function () {

    function getLoginForm() {
        var loginForm = $('.loginForm');
        return loginForm;
    }

    function selectNewUserRadioButton() {
        getLoginForm().find('input[value="new-user"]').attr('selected', 'selected').click().click();
    }

    function selectExistingUserRadioButton() {
        getLoginForm().find('input[value="existing-user"]').attr('selected', 'selected').click().click();
    }

    it('Should exist in spec runner', function () {
        expect(getLoginForm().length).toBeGreaterThan(0);
    });

    it('Should exist in jquery as a widget', function () {
        expect(getLoginForm().yazinoLoginAndRegistrationForm).toBeDefined();
    });

//    it('Shouldcontain at least one only-active-for-new-users class', function () {
//        expect(getLoginForm().find('.only-active-for-new-users').length).toBeGreaterThan(0);
//    });

    it('Should contain at least one only-active-for-existing-users class', function () {
        expect(getLoginForm().find('.only-active-for-existing-users').length).toBeGreaterThan(0);
    });

    it('Should contain at least one only-shown-for-new-users class', function () {
//        selectNewUserRadioButton();
        expect(getLoginForm().find('.only-shown-for-new-users').length).toBeGreaterThan(0);
    });

    it('Should start in existing user mode', function () {

        expect(getLoginForm().find('#displayName').is(':hidden')).toBeTruthy();
        expect(getLoginForm().find('#password').is(':hidden')).toBeTruthy();
        expect(getLoginForm().find('#registeredPassword').is('[disabled]')).toBeFalsy();
    });

    it('should hide only-shown-for-new-users for existing users', function () {
        selectExistingUserRadioButton();
        expect(getLoginForm().find('.only-shown-for-new-users').is(':hidden')).toBeTruthy();
    });

    it('should show only-shown-for-new-users for new users', function () {
        selectNewUserRadioButton();
        expect(getLoginForm().find('.only-shown-for-new-users').is(':hidden')).toBeFalsy();
    });

    it('should show new-user-password for new users', function () {
        selectNewUserRadioButton();
        expect(getLoginForm().find('#password').is(':hidden')).toBeFalsy();
    });

    it('should hide new-user-password for existing users', function () {
        selectExistingUserRadioButton();
        expect(getLoginForm().find('#password').is(':hidden')).toBeTruthy();
    });

    it('should disable existing-user-password for new users', function () {
        selectNewUserRadioButton();
        expect(getLoginForm().find('#registeredPassword').is('[disabled]')).toBeTruthy();
    });

    it('should enable existing-user-password for existing users', function () {
        selectExistingUserRadioButton();
        expect(getLoginForm().find('#registeredPassword').is('[disabled]')).toBeFalsy();
    });

    it('should enable existing-user-password for existing users', function () {
        selectExistingUserRadioButton();
        expect(getLoginForm().find('#registeredPassword').is('[disabled]')).toBeFalsy();
    });

    it('should add disabled class to only-active-for-existing-users for new users', function () {
        selectNewUserRadioButton();
        expect(getLoginForm().find('.only-active-for-existing-users').hasClass('disabled')).toBeTruthy();
    });

    it('should remove disabled class to only-active-for-existing-users for existing users', function () {
        selectNewUserRadioButton();
        selectExistingUserRadioButton();
        expect(getLoginForm().find('.only-active-for-existing-users').hasClass('disabled')).toBeFalsy();
    });

    it('should make email all required', function () {
        expect(getLoginForm().find('#email').attr('required')).toBeTruthy();
        selectNewUserRadioButton();
        expect(getLoginForm().find('#email').attr('required')).toBeTruthy();
        selectExistingUserRadioButton();
        expect(getLoginForm().find('#email').attr('required')).toBeTruthy();
    });

    it('should make registration fields required when enabled', function () {
        expect(getLoginForm().find('#displayName').hasClass('required-when-enabled')).toBeTruthy();
        expect(getLoginForm().find('#password').hasClass('required-when-enabled')).toBeTruthy();
        expect(getLoginForm().find('#termsAndConditions').hasClass('required-when-enabled')).toBeTruthy();
        expect(getLoginForm().find('#registeredPassword').hasClass('required-when-enabled')).toBeTruthy();
    });


    it('should make registration fields required when enabled', function () {
        selectNewUserRadioButton();
        expect(getLoginForm().find('#displayName').attr('required')).toBeTruthy();
        expect(getLoginForm().find('#password').attr('required')).toBeTruthy();
        expect(getLoginForm().find('#termsAndConditions').attr('required')).toBeTruthy();
    });

    it('should make login fields required when enabled', function () {
        selectExistingUserRadioButton();
        expect(getLoginForm().find('#registeredPassword').attr('required')).toBeTruthy();
    });

    it('should make registration fields not required when disabled', function () {
        selectExistingUserRadioButton();

        expect(getLoginForm().find('#password').attr('required')).toBeFalsy();
        expect(getLoginForm().find('#termsAndConditions').attr('required')).toBeFalsy();
        expect(getLoginForm().find('#displayName').attr('required')).toBeFalsy();
    });

    it('should make login fields not required when disabled', function () {
        selectNewUserRadioButton();

        expect(getLoginForm().find('#registeredPassword').attr('required')).toBeFalsy();
    });

});
