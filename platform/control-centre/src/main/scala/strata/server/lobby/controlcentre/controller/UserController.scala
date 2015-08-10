package strata.server.lobby.controlcentre.controller

import org.springframework.stereotype.Controller
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.servlet.ModelAndView
import javax.servlet.http.HttpServletResponse
import org.springframework.web.bind.annotation._
import org.apache.commons.lang3.Validate.notNull
import strata.server.lobby.controlcentre.repository.UserRepository
import strata.server.lobby.controlcentre.form.UserForm
import java.util
import org.apache.commons.lang3.text.WordUtils
import org.springframework.security.authentication.encoding.PasswordEncoder
import scala.Array
import strata.server.lobby.controlcentre.validation.UserValidator
import org.springframework.validation.BindingResult
import org.springframework.web.bind.WebDataBinder
import strata.server.lobby.controlcentre.util.BigDecimalPropertyEditorSupport

@Controller
class UserController @Autowired()(val userRepository: UserRepository,
                                  val passwordEncoder: PasswordEncoder,
                                  val userValidator: UserValidator) {
    notNull(userRepository, "userRepository may not be null")
    notNull(passwordEncoder, "passwordEncoder may not be null")
    notNull(userValidator, "userValidator may not be null")

    @InitBinder def initBinder(binder: WebDataBinder) {
        binder.registerCustomEditor(classOf[BigDecimal], new BigDecimalPropertyEditorSupport())
    }

    @RequestMapping(Array("admin/user/list"))
    def list: ModelAndView = listAtPage(1)

    @RequestMapping(Array("admin/user/list/{page}"))
    def listAtPage(@PathVariable("page") page: Int = 1): ModelAndView = {
        new ModelAndView("admin/user/list")
            .addObject("users", userRepository.findAll(page - 1, 20))
    }

    @RequestMapping(Array("admin/user/show/{userName:.+}"))
    def show(@PathVariable("userName") userName: String,
             response: HttpServletResponse): ModelAndView = {
        val user = userRepository.findById(userName)

        if (user.isDefined) {
            new ModelAndView("admin/user/show")
                .addObject("user", new UserForm(user.get))
                .addObject("roles", asRoleMap(userRepository.findAllRoles))
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND)
            null
        }
    }

    @RequestMapping(Array("admin/user/edit/{userName:.+}"))
    def edit(@PathVariable("userName") userName: String,
             response: HttpServletResponse): ModelAndView = {
        val modelAndView = show(userName, response)
        if (modelAndView != null) {
            modelAndView.setViewName("admin/user/edit")
        }
        modelAndView
    }

    @RequestMapping(Array("admin/user/create"))
    def create(): ModelAndView =
        new ModelAndView("admin/user/create")
            .addObject("user", new UserForm())
            .addObject("roles", asRoleMap(userRepository.findAllRoles))

    @RequestMapping(value = Array("admin/user/save"), method = Array(RequestMethod.POST))
    def save(@ModelAttribute("user") form: UserForm,
             bindingResult: BindingResult): ModelAndView = {
        notNull(form, "form may not be null")

        userValidator.validate(form, bindingResult)
        if (bindingResult.hasErrors) {
            val source = if (form.isNew) {
                "create"
            } else {
                "edit"
            }
            new ModelAndView("admin/user/%s".format(source))
                .addObject("user", form)
                .addObject("roles", asRoleMap(userRepository.findAllRoles))

        } else {
            if ((form.password == null || form.password.trim().length == 0) && form.userName != null) {
                val currentUser = userRepository.findById(form.userName).get
                form.password = currentUser.password
            } else {
                form.password = passwordEncoder.encodePassword(form.password, null)
            }

            val savedUser = userRepository.save(form.toUser)
            new ModelAndView("redirect:/admin/user/show/%s".format(savedUser.userName))
        }
    }

    @RequestMapping(Array("admin/user/delete/{userName:.+}"))
    def delete(@PathVariable("userName") userName: String): ModelAndView = {
        userRepository.delete(userName)

        list.addObject("message", "User %s has been deleted.".format(userName))
    }

    private def asRoleMap(items: Iterable[String]) = {
        val map = new util.HashMap[String, String]()
        items.foreach(item => map.put(item, WordUtils.capitalize(
            item.replaceAll("ROLE_", "").replaceAll("_", " ").toLowerCase)))
        map
    }

}
