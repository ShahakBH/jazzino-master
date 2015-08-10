package strata.server.lobby.controlcentre.controller

import org.springframework.stereotype.Controller
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.servlet.ModelAndView
import javax.servlet.http.HttpServletResponse
import org.springframework.web.bind.annotation._
import org.apache.commons.lang3.Validate.notNull
import strata.server.lobby.controlcentre.repository.SystemMessageRepository
import strata.server.lobby.controlcentre.form.SystemMessageForm
import scala.Array
import strata.server.lobby.controlcentre.validation.SystemMessageValidator
import org.springframework.validation.BindingResult
import org.springframework.web.bind.WebDataBinder
import strata.server.lobby.controlcentre.util.BigDecimalPropertyEditorSupport
import com.yazino.platform.community.CommunityConfigurationUpdateService
import org.slf4j.LoggerFactory
import java.text.SimpleDateFormat
import org.springframework.beans.propertyeditors.CustomDateEditor
import java.util

@Controller
class SystemMessageController @Autowired()(val systemMessageRepository: SystemMessageRepository,
                                           val systemMessageValidator: SystemMessageValidator,
                                           val communityConfigurationUpdateService: CommunityConfigurationUpdateService) {
    notNull(systemMessageRepository, "systemMessageRepository may not be null")
    notNull(systemMessageValidator, "systemMessageValidator may not be null")
    notNull(communityConfigurationUpdateService, "communityConfigurationUpdateService may not be null")

    private val logger = LoggerFactory.getLogger(classOf[SystemMessageController])

    @InitBinder def initBinder(binder: WebDataBinder) {
        binder.registerCustomEditor(classOf[BigDecimal], new BigDecimalPropertyEditorSupport())

        val editor = new CustomDateEditor(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm"), true)
        binder.registerCustomEditor(classOf[util.Date], editor)
    }

    @RequestMapping(Array("maintenance/messages/list"))
    def list: ModelAndView = listAtPage(1)

    @RequestMapping(Array("maintenance/messages/list/{page}"))
    def listAtPage(@PathVariable("page") page: Int = 1): ModelAndView = {
        new ModelAndView("maintenance/messages/list")
            .addObject("systemMessages", systemMessageRepository.findAll(page - 1, 20))
    }

    @RequestMapping(Array("maintenance/messages/show/{systemMessageId}"))
    def show(@PathVariable("systemMessageId") systemMessageId: Int,
             response: HttpServletResponse): ModelAndView = {
        val user = systemMessageRepository.findById(BigDecimal(systemMessageId))

        if (user.isDefined) {
            new ModelAndView("maintenance/messages/show")
                .addObject("systemMessage", new SystemMessageForm(user.get))
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND)
            null
        }
    }

    @RequestMapping(Array("maintenance/messages/edit/{systemMessageId}"))
    def edit(@PathVariable("systemMessageId") systemMessageId: Int,
             response: HttpServletResponse): ModelAndView = {
        val modelAndView = show(systemMessageId, response)
        if (modelAndView != null) {
            modelAndView.setViewName("maintenance/messages/edit")
        }
        modelAndView
    }

    @RequestMapping(Array("maintenance/messages/create"))
    def create(): ModelAndView =
        new ModelAndView("maintenance/messages/create")
            .addObject("systemMessage", new SystemMessageForm())

    @RequestMapping(value = Array("maintenance/messages/save"), method = Array(RequestMethod.POST))
    def save(@ModelAttribute("systemMessage") form: SystemMessageForm,
             bindingResult: BindingResult): ModelAndView = {
        notNull(form, "form may not be null")

        systemMessageValidator.validate(form, bindingResult)
        if (bindingResult.hasErrors) {
            val source = if (form.id != null) {
                "edit"
            } else {
                "create"
            }
            new ModelAndView("maintenance/messages/%s".format(source))
                .addObject("systemMessage", form)

        } else {
            val savedSystemMessage = systemMessageRepository.save(form.toSystemMessage)

            refreshSystemMessagesInGrid()

            new ModelAndView("redirect:/maintenance/messages/show/%s".format(savedSystemMessage.id))
        }
    }

    @RequestMapping(Array("maintenance/messages/delete/{systemMessageId}"))
    def delete(@PathVariable("systemMessageId") systemMessageId: Int): ModelAndView = {
        systemMessageRepository.delete(BigDecimal(systemMessageId))

        refreshSystemMessagesInGrid()

        list.addObject("message", "System Message %s has been deleted.".format(systemMessageId))
    }

    private def refreshSystemMessagesInGrid() {
        logger.debug("Refreshing system messages in grid")

        communityConfigurationUpdateService.refreshSystemMessages()
    }

}
