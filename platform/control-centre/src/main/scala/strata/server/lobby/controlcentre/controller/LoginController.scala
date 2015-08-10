package strata.server.lobby.controlcentre.controller

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.servlet.ModelAndView
import javax.servlet.http.HttpSession

@Controller
class LoginController {

    @RequestMapping(Array("login"))
    def login(): String = "login"

    @RequestMapping(Array("login/error"))
    def home(session: HttpSession): ModelAndView = {
        new ModelAndView("login")
                .addObject("error", lastSpringSecurityError(session))
    }

    private def lastSpringSecurityError(session: HttpSession): String = {
        val lastException: Exception = session.getAttribute("SPRING_SECURITY_LAST_EXCEPTION").asInstanceOf[Exception]
        if (lastException != null) {
            lastException.getMessage
        } else {
            null
        }
    }
}
