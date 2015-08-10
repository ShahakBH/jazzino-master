package strata.server.lobby.controlcentre.controller

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.servlet.ModelAndView

@Controller
class Home {

    @RequestMapping(Array("/", "/home"))
    def home(): ModelAndView = {
        new ModelAndView("home")
                .addObject("hello", "world")
    }

}
