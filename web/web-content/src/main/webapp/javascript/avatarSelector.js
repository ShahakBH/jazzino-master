function selectAvatar(url) {
    $("#avatarURL").val(url);
    $("#selectedAvatarImg").attr("src", url);
    $("#uploadError").hide();
}

function oldUploadAvatar() {
    //noinspection JSUnusedLocalSymbols
    jQuery.ajaxFileUpload({
        url: '/publicCommand/avatars',
        secureuri: false,
        fileElementId: 'file',
        dataType: 'json',
        beforeSend:function() {
            $("#uploadError").hide();
            $("#uploading").show();
        },
        complete:function() {
            $("#uploading").hide();

        },
        success: function (data, status) {
            if (data.error) {
                $("#uploadError").text(data.error);
                $("#uploadError").show();
            } else {
                $("#uploadError").hide();
                selectAvatar(data.avatar.url);
            }
        },
        error: function (data, status, e) {
            $("#uploadError").text("Upload failed" + e);
            $("#uploadError").show();
        }
    });
    return false;
}

function uploadAvatar() {
    //noinspection JSUnusedLocalSymbols
    jQuery('#file').ajaxfileupload({
        action: '/publicCommand/avatars',
        onStart:function() {
            $("#uploadError").hide();
            $("#uploading").show();
        },
        onComplete:function(response) {
            $("#uploading").hide();
            YAZINO.logger.log(response);
        }
//            success: function (data, status) {
//                if (data.error) {
//                    $("#uploadError").text(data.error);
//                    $("#uploadError").show();
//                } else {
//                    $("#uploadError").hide();
//                    selectAvatar(data.avatar.url);
//                }
//            },
//            error: function (data, status, e) {
//                $("#uploadError").text("Upload failed" + e);
//                $("#uploadError").show();
//            }
    });
    return false;
}
