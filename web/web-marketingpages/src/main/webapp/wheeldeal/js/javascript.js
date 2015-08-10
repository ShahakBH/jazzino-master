$(document).ready(function () {

    $("#email").focus(function() {
        if ($(this).attr("value") == "Email") $(this).attr("value","");
    });
    $("#email").blur(function() {
        if ($(this).attr("value") == "") $(this).attr("value","Email");
    });

    $("#displayName").focus(function() {
        if ($(this).attr("value") == "Display Name") $(this).attr("value","");
      });
    $("#displayName").blur(function() {
        if ($(this).attr("value") == "") $(this).attr("value","Display Name");
    });

    $("#password").focus(function() {
        if ($(this).attr("value") == "Password") $(this).attr("value","");
    });
    $("#password").blur(function() {
        if ($(this).attr("value") == "") $(this).attr("value","Password");
    });

});