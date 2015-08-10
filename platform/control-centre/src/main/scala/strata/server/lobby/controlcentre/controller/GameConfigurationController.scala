package strata.server.lobby.controlcentre.controller

import org.springframework.beans.factory.annotation.Autowired
import strata.server.lobby.controlcentre.repository.GameConfigurationRepository
import org.apache.commons.lang3.Validate.notNull
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.servlet.ModelAndView
import org.springframework.stereotype.Controller
import java.math.BigDecimal
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.apache.commons.logging.LogFactory
import com.yazino.platform.table.{GameConfigurationProperty, GameConfiguration, TableConfigurationUpdateService}
import strata.server.lobby.controlcentre.util.JavaBigDecimalPropertyEditorSupport
import strata.server.lobby.controlcentre.form.{UploadedFileForm, GameConfigurationPropertyForm, GameConfigurationForm}
import org.springframework.web.bind.annotation._
import java.io._
import java.util
import org.apache.commons.lang3.exception.ExceptionUtils

@Controller
class GameConfigurationController @Autowired()(val gameConfigurationRepository: GameConfigurationRepository,
                                               val tableConfigurationUpdateService: TableConfigurationUpdateService) {
    notNull(gameConfigurationRepository, "gameConfigurationRepository may not be null")
    val LOG = LogFactory.getLog(classOf[GameConfigurationController])

    @InitBinder def initBinder(binder: WebDataBinder) {
        binder.registerCustomEditor(classOf[BigDecimal], new JavaBigDecimalPropertyEditorSupport())
    }

    @RequestMapping(Array("game/configuration", "game/configuration/list"))
    def list(): ModelAndView = {
        new ModelAndView("game/configuration/list")
            .addObject("gameConfigurations", gameConfigurationRepository.findAllGames())
    }

    @RequestMapping(Array("game/configuration/show/{gameId}"))
    def show(@PathVariable("gameId") gameId: String, response: HttpServletResponse): ModelAndView = {
        val gameConfiguration = gameConfigurationRepository.findGameById(gameId)
        if (gameConfiguration.isDefined) {
            new ModelAndView("game/configuration/show")
                .addObject("gameConfiguration", new GameConfigurationForm(gameConfiguration.get))
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND)
            null
        }
    }

    @RequestMapping(Array("game/configuration/edit/{gameId}"))
    def edit(@PathVariable("gameId") gameId: String, response: HttpServletResponse): ModelAndView = {
        val gameConfiguration = gameConfigurationRepository.findGameById(gameId)
        if (gameConfiguration.isDefined) {
            new ModelAndView("game/configuration/edit")
                .addObject("gameConfiguration", new GameConfigurationForm(gameConfiguration.get))
                .addObject("gameConfigurationProperty", new GameConfigurationPropertyForm())
                .addObject("uploadedFileForm", new UploadedFileForm())
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND)
            null
        }
    }

    @RequestMapping(value = Array("game/configuration/save"), method = Array(RequestMethod.POST))
    def save(@ModelAttribute("gameConfiguration") form: GameConfigurationForm,
             request: HttpServletRequest): ModelAndView = {
        notNull(form, "form may not be null")
        var gameConfiguration: GameConfiguration = form.toGameConfiguration
        if (LOG.isDebugEnabled) {
            LOG.debug("Saving game with game configuration %s".format(gameConfiguration))
        }
        gameConfiguration = updateShortName(convertToCamelCase(gameConfiguration.getDisplayName), gameConfiguration)
        gameConfiguration = gameConfigurationRepository.save(gameConfiguration)
        tableConfigurationUpdateService.asyncRefreshGameConfigurations()

        new ModelAndView("redirect:/game/configuration/show/%s".format(gameConfiguration.getGameId))
    }

    @RequestMapping(Array("game/configuration/create"))
    def create: ModelAndView = {
        new ModelAndView("game/configuration/create")
            .addObject("gameConfiguration", new GameConfigurationForm())
            .addObject("gameConfigurationProperty", new GameConfigurationPropertyForm())
    }

    @RequestMapping(Array("game/configuration/delete/{gameId}"))
    def delete(@PathVariable("gameId") gameId: String): ModelAndView = {
        gameConfigurationRepository.delete(gameId)
        tableConfigurationUpdateService.asyncRefreshGameConfigurations()

        new ModelAndView("redirect:/game/configuration/list")
    }

    @RequestMapping(Array("game/configuration/property/add"))
    def addProperty(@ModelAttribute("gameConfiguration") form: GameConfigurationForm,
                    @ModelAttribute("gameConfigurationProperty") propertyForm: GameConfigurationPropertyForm,
                    response: HttpServletResponse): ModelAndView = {
        notNull(form, "form may not be null")
        notNull(propertyForm, "propertyForm may not be null")

        if (LOG.isDebugEnabled) {
            LOG.debug("Adding property %s".format(propertyForm))
        }

        val configurationProperty = propertyForm.toGameConfigurationProperty
        var gameConfiguration = gameConfigurationRepository.findGameById(form.gameId).get
        if (!configurationProperty.getPropertyName.isEmpty && !configurationProperty.getPropertyValue.isEmpty) {
            val updatedProperties = new util.ArrayList[GameConfigurationProperty](gameConfiguration.getProperties)
            updatedProperties.add(configurationProperty)
            gameConfiguration = gameConfiguration.withProperties(updatedProperties)
            gameConfiguration = gameConfigurationRepository.save(gameConfiguration)
        }
        tableConfigurationUpdateService.asyncRefreshGameConfigurations()

        edit(gameConfiguration.getGameId, response)
    }

    @RequestMapping(Array("game/configuration/property/delete/{propertyId}"))
    def deleteProperty(@PathVariable("propertyId") propertyId: Int,
                       @ModelAttribute("gameConfiguration") form: GameConfigurationForm,
                       response: HttpServletResponse): ModelAndView = {
        notNull(propertyId, "propertyId may not be null")
        notNull(form, "form may not be null")
        val gameId = form.getGameId
        if (LOG.isDebugEnabled) {
            LOG.debug("Deleting property with ID %s from game with ID %s".format(propertyId, gameId))
        }
        gameConfigurationRepository.deleteProperty(gameId, BigDecimal.valueOf(propertyId))
        val gameConfiguration = gameConfigurationRepository.findGameById(gameId).get
        tableConfigurationUpdateService.asyncRefreshGameConfigurations()

        edit(gameConfiguration.getGameId, response)
    }

    @RequestMapping(value = Array("game/upload"), method = Array(RequestMethod.POST))
    def uploadGame(@ModelAttribute("uploadedFileForm") uploadItemForm: UploadedFileForm, response: HttpServletResponse): ModelAndView = {
        notNull(uploadItemForm, "upload form may not be null")
        notNull(uploadItemForm.name, "file name may not be null")
        LOG.debug("File uploaded with name %s for game".format(uploadItemForm.name))
        LOG.debug("Original file name is: %s".format(uploadItemForm.fileData.getOriginalFilename))
        try {
            val filename = String.format("%s%s.jar", "/var/yazino-games/", uploadItemForm.name)
            val outputStream = new FileOutputStream(filename)
            outputStream.write(uploadItemForm.fileData.getBytes)
            outputStream.close()
            tableConfigurationUpdateService.publishGame(filename)

            edit(uploadItemForm.name.toLowerCase, response)
                .addObject("message", "Upload successful.")
        } catch {
            case e: IOException => {
                val errorMessage: String = "Problem publishing game %s".format(uploadItemForm.name)
                LOG.error(errorMessage, e)
                new ModelAndView("game/configuration/error")
                    .addObject("message", errorMessage)
                    .addObject("exception", ExceptionUtils.getStackTrace(e))
            }
        }
    }

    protected[controller] def convertToCamelCase(displayName: String) = {
        val words = """([a-zA-Z]+)""".r
        var camelCase = words.replaceAllIn(displayName, m => m.matched.toLowerCase.capitalize)
        camelCase = """([^a-zA-Z]+)""".r.replaceAllIn(camelCase, "")
        if (camelCase.size > 1) {
            camelCase = camelCase.charAt(0).toLower + camelCase.substring(1, camelCase.size)
        }
        camelCase
    }

    private def updateShortName(shortName: String, gameConfiguration: GameConfiguration): GameConfiguration = {
        new GameConfiguration(gameConfiguration.getGameId, shortName, gameConfiguration.getDisplayName,
            gameConfiguration.getAliases, gameConfiguration.getOrder)
            .withProperties(gameConfiguration.getProperties)
    }
}
