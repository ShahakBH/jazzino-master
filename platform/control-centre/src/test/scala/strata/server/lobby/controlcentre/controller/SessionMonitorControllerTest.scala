package strata.server.lobby.controlcentre.controller

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito.{verify, when}
import com.yazino.platform.model.PagedData
import java.math.{BigDecimal => JavaBigDecimal}
import java.util.Arrays.asList
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import com.yazino.platform.community.{PlayerService, BasicProfileInformation}
import com.yazino.platform.session.{Session, SessionService}
import java.util.Collections
import org.joda.time.DateTime
import org.scalatest.{BeforeAndAfterEach, OneInstancePerTest, FlatSpec}
import com.yazino.test.ThreadLocalDateTimeUtils
import com.yazino.platform.Platform
import com.yazino.platform.Partner

@RunWith(classOf[JUnitRunner])
class SessionMonitorControllerTest extends FlatSpec
        with ShouldMatchers
        with MockitoSugar
        with OneInstancePerTest
        with BeforeAndAfterEach {

    private val sessionService = mock[SessionService]
    private val playerService = mock[PlayerService]
    private val request = mock[HttpServletRequest]
    private val response = mock[HttpServletResponse]

    private val player = new BasicProfileInformation(
        JavaBigDecimal.valueOf(3428), "aName", "aPicture", JavaBigDecimal.valueOf(235))

    private val underTest = new SessionMonitorController(sessionService, playerService)

    override def beforeEach() {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(100000)
    }

    override def afterEach() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem()
    }

    "The Controller" should "show the list view for the list action" in {
        val modelAndView = underTest.list(10)

        modelAndView.getViewName should equal("monitor/session/list")
    }

    it should "fetch the given page of sessions from the session service for the list action" in {
        underTest.list(17)

        verify(sessionService).findSessions(17)
    }

    it should "include the found sessions in the model for the list action" in {
        val expectedPagedData = new PagedData[Session](10, 3, 17, asList(aSession))
        when(sessionService.findSessions(13)).thenReturn(expectedPagedData)

        val modelAndView = underTest.list(13)

        modelAndView.getModel.get("sessions") should equal(expectedPagedData)
    }

    it should "show the list view for the list at page action" in {
        val modelAndView = underTest.listAtPage(10)

        modelAndView.getViewName should equal("monitor/session/list")
    }

    it should "fetch the given page of sessions from the session service for the list at page action" in {
        underTest.listAtPage(4)

        verify(sessionService).findSessions(3)
    }

    it should "include the found sessions in the model for the list at page action" in {
        val expectedPagedData = new PagedData[Session](10, 3, 17, asList(aSession))
        when(sessionService.findSessions(9)).thenReturn(expectedPagedData)

        val modelAndView = underTest.listAtPage(10)

        modelAndView.getModel.get("sessions") should equal(expectedPagedData)
    }

    it should "redirect to the list view after an unload all action" in {
        val modelAndView = underTest.unloadAll(3423, request)

        modelAndView.getViewName should equal("redirect:/monitor/session/list")
    }

    it should "unload the requested player for an unload all action" in {
        underTest.unloadAll(3423, request)

        verify(sessionService).invalidateAllByPlayer(JavaBigDecimal.valueOf(3423))
    }

    it should "unload the requested player for an unload action" in {
        underTest.unload(3423, "aSessionKey", request)

        verify(sessionService).invalidateByPlayerAndSessionKey(JavaBigDecimal.valueOf(3423), "aSessionKey")
    }

    it should "show the details view for the details action" in {
        when(playerService.getBasicProfileInformation(JavaBigDecimal.valueOf(3432)))
            .thenReturn(player)
        when(sessionService.findByPlayerAndSessionKey(JavaBigDecimal.valueOf(3432), "aSessionKey")).thenReturn(aSession)

        val modelAndView = underTest.details(3432, "aSessionKey", response)

        modelAndView.getViewName should equal("monitor/session/details")
    }

    it should "send a 404 for a non-existent player for the details action" in {
        underTest.details(3432, "aSessionKey", response)

        verify(response).sendError(404)
    }

    it should "fetch the details for a player from the Player Details Service for the details action" in {
        underTest.details(3432, "aSessionKey", response)

        verify(playerService).getBasicProfileInformation(JavaBigDecimal.valueOf(3432))
    }

    it should "add the details for a player to the response for the details action" in {
        when(playerService.getBasicProfileInformation(JavaBigDecimal.valueOf(3438)))
            .thenReturn(player)

        val modelAndView = underTest.details(3438, "aSessionKey", response)

        modelAndView.getModel.get("player") should equal(player)
    }

    it should "fetch the session for a player from the Session Service for the details action" in {
        when(playerService.getBasicProfileInformation(JavaBigDecimal.valueOf(3432)))
            .thenReturn(player)

        underTest.details(3432, "aSessionKey", response)

        verify(sessionService).findByPlayerAndSessionKey(JavaBigDecimal.valueOf(3432), "aSessionKey")
    }

    it should "add the session for a player to the response for the details action" in {
        when(playerService.getBasicProfileInformation(JavaBigDecimal.valueOf(3438)))
            .thenReturn(player)
        when(sessionService.findByPlayerAndSessionKey(JavaBigDecimal.valueOf(3438), "aSessionKey")).thenReturn(aSession)

        val modelAndView = underTest.details(3438, "aSessionKey", response)

        modelAndView.getModel.get("session") should equal(aSession)
    }

    private def aSession =
        new Session(
            new JavaBigDecimal(5454), new JavaBigDecimal(3141592), Partner.YAZINO, Platform.WEB, "ipAddress",
            "aLocalSessionKey", "aNickname", "anEmail", "aPictureUrl", JavaBigDecimal.TEN, new DateTime(),
            Collections.emptySet(), Collections.emptySet())

}
