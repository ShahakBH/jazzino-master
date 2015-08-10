describe('FacebookExtraPermissionsService', function () {
    beforeEach(function() {
        YAZINO.configuration.set('facebookConnect', true);
        this.progressBarService = YAZINO.createProgressBarService(YAZINO.createModalDialogueService());
        this.underTest = new YAZINO.FacebookExtraPermissionsService(this.progressBarService);
    });

    it('should call runUpdate on Progress Bar Service on triggerDialogBox if user connected', function () {
        spyOn(this.progressBarService, 'runUpdate');
        spyOn(FB, 'login').andCallFake(function (callback) {
            callback(
                {
                    status:'connected',
                    authResponse: {accessToken: 'access_token'}
                });
        });

        FB.getAuthResponse = null;


        this.underTest.triggerDialogBox();
        expect(this.progressBarService.runUpdate).toHaveBeenCalled();
    });

    it('should not call runUpdate on Progress Bar Service on triggerDialogBox if user not connected', function () {
        spyOn(this.progressBarService, 'runUpdate');
        spyOn(FB, 'login').andCallFake(function (callback) {
            callback(
                {
                    status:'la la la la'
                });
        });

        this.underTest.triggerDialogBox();
        expect(this.progressBarService.runUpdate).not.toHaveBeenCalled();
    });
});