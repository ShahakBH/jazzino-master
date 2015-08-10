package strata.server.lobby.controlcentre.controller

import org.scalatest.junit.AssertionsForJUnit
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar
import javax.servlet.http.HttpServletResponse
import strata.server.lobby.controlcentre.repository.JDBCSystemMessageRepository
import org.springframework.validation.BindingResult
import strata.server.lobby.controlcentre.validation.SystemMessageValidator
import org.junit.Test
import org.mockito.Mockito._
import com.yazino.platform.model.PagedData
import strata.server.lobby.controlcentre.model.SystemMessage
import org.joda.time.DateTime
import scala.collection.JavaConversions._
import strata.server.lobby.controlcentre.form.SystemMessageForm
import com.yazino.platform.community.CommunityConfigurationUpdateService

class SystemMessageControllerTest extends AssertionsForJUnit with ShouldMatchers with MockitoSugar {

    private val response = mock[HttpServletResponse]
    private val systemMessageRepository = mock[JDBCSystemMessageRepository]
    private val systemMessageValidator = mock[SystemMessageValidator]
    private val bindingResult = mock[BindingResult]
    private val communityConfigurationUpdateService = mock[CommunityConfigurationUpdateService]

    private val underTest = new SystemMessageController(
            systemMessageRepository, systemMessageValidator, communityConfigurationUpdateService)

    @Test def listingReturnsAllSystemMessages() {
        val expectedData = new PagedData[SystemMessage](0, 2, 2, List(aSystemMessage(1), aSystemMessage(2)))
        when(systemMessageRepository.findAll(0, 20)).thenReturn(expectedData)

        val model = underTest.list

        model.getModel.get("systemMessages") should equal(expectedData)
    }

    @Test def listingAtPageReturnsTheAppropriatePage() {
        val expectedData = new PagedData[SystemMessage](0, 2, 2, List(aSystemMessage(1), aSystemMessage(2)))
        when(systemMessageRepository.findAll(3, 20)).thenReturn(expectedData)

        underTest.listAtPage(4)

        verify(systemMessageRepository).findAll(3, 20)
    }

    @Test def listingUsesTheListView() {
        when(systemMessageRepository.findAll(0, 20)).thenReturn(
                new PagedData[SystemMessage](0, 2, 2, List(aSystemMessage(1), aSystemMessage(2))))

        val model = underTest.list

        model.getViewName should equal("maintenance/messages/list")
    }

    @Test def showingANonExistentSystemMessageReturnsAFileNotFoundStatus() {
        when(systemMessageRepository.findById(BigDecimal(100))).thenReturn(None)

        val model = underTest.show(100, response)

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND)
        model should equal(null)
    }

    @Test def showingAnExistingSystemMessageReturnsTheSystemMessage() {
        when(systemMessageRepository.findById(101)).thenReturn(Some(aSystemMessage(101)))

        val model = underTest.show(101, response)

        model.getModel.get("systemMessage") should equal(new SystemMessageForm(aSystemMessage(101)))
        verifyZeroInteractions(response)
    }

    @Test def showingAnExistingSystemMessageUsesTheShowView() {
        when(systemMessageRepository.findById(101)).thenReturn(Some(aSystemMessage(101)))

        val model = underTest.show(101, response)

        model.getViewName should equal("maintenance/messages/show")
    }

    @Test def creatingASystemMessageUsesTheCreateView() {
        val model = underTest.create()

        model.getViewName should equal("maintenance/messages/create")
    }

    @Test def creatingASystemMessageAddsAnEmptyFormToTheModel() {
        val model = underTest.create()

        model.getModel.get("systemMessage") should equal(new SystemMessageForm())
    }

    @Test def editingASystemMessageUsesTheEditView() {
        when(systemMessageRepository.findById(101)).thenReturn(Some(aSystemMessage(101)))

        val model = underTest.edit(101, response)

        model.getViewName should equal("maintenance/messages/edit")
    }

    @Test def editingAnExistingSystemMessageAddsTheAppropriateFormToTheModel() {
        when(systemMessageRepository.findById(101)).thenReturn(Some(aSystemMessage(101)))

        val model = underTest.edit(101, response)

        model.getModel.get("systemMessage") should equal(new SystemMessageForm(aSystemMessage(101)))
    }

    @Test def editingANonExistentSystemMessageReturnsANotFoundError() {
        when(systemMessageRepository.findById(101)).thenReturn(None)

        val model = underTest.edit(101, response)

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND)
        model should equal(null)
    }

    @Test def deletingASystemMessageDeletesTheSystemMessageViaTheRepository() {
        underTest.delete(101)

        verify(systemMessageRepository).delete(BigDecimal(101))
    }

    @Test def deletingASystemMessageRefreshesTheMessagesInTheGrid() {
        underTest.delete(101)

        verify(communityConfigurationUpdateService).refreshSystemMessages()
    }

    @Test def deletingASystemMessageAddsAMessageToTheModel() {
        val model = underTest.delete(101)

        model.getModel.get("message") should equal("System Message 101 has been deleted.")
    }

    @Test def deletingASystemMessageDirectsTheUserToTheListView() {
        val expectedData = new PagedData[SystemMessage](0, 2, 2, List(aSystemMessage(1), aSystemMessage(2)))
        when(systemMessageRepository.findAll(0, 20)).thenReturn(expectedData)

        val model = underTest.delete(101)

        model.getViewName should equal("maintenance/messages/list")
        model.getModel.get("systemMessages") should equal(expectedData)
    }

    @Test def savingASystemMessageRedirectsTheUserToTheShowView() {
        val form = new SystemMessageForm(aSystemMessage(100))
        when(systemMessageRepository.save(form.toSystemMessage)).thenReturn(aSystemMessage(100))

        val model = underTest.save(form, bindingResult)

        model.getViewName should equal("redirect:/maintenance/messages/show/100")
    }

    @Test def savingAnExistingSystemMessageSavesTheSystemMessageToTheRepository() {
        val form = new SystemMessageForm(aSystemMessage(100))
        when(systemMessageRepository.save(form.toSystemMessage)).thenReturn(aSystemMessage(100))

        underTest.save(form, bindingResult)

        verify(systemMessageRepository).save(form.toSystemMessage)
    }

    @Test def savingANewSystemMessageSavesTheSystemMessageToTheRepository() {
        val form = new SystemMessageForm(aSystemMessage(-1))
        when(systemMessageRepository.save(form.toSystemMessage)).thenReturn(aSystemMessage(100))

        underTest.save(form, bindingResult)

        verify(systemMessageRepository).save(form.toSystemMessage)
    }

    @Test def savingANewSystemMessageRefreshesMessagesInTheGrid() {
        val form = new SystemMessageForm(aSystemMessage(-1))
        when(systemMessageRepository.save(form.toSystemMessage)).thenReturn(aSystemMessage(100))

        underTest.save(form, bindingResult)

        verify(communityConfigurationUpdateService).refreshSystemMessages()
    }

    @Test def savingANewSystemMessageThatFailsValidationRedirectsTheUserToTheCreatePage() {
        val form = new SystemMessageForm(aSystemMessage())
        when(bindingResult.hasErrors).thenReturn(true)

        val model = underTest.save(form, bindingResult)

        verify(systemMessageValidator).validate(form, bindingResult)
        model.getViewName should equal("maintenance/messages/create")
        model.getModel.get("systemMessage") should equal(form)
    }

    @Test def savingAnExistingSystemMessageThatFailsValidationRedirectsTheUserToTheCreatePage() {
        val form = new SystemMessageForm(aSystemMessage(100))
        when(bindingResult.hasErrors).thenReturn(true)
        when(bindingResult.hasErrors).thenReturn(true)

        val model = underTest.save(form, bindingResult)

        verify(systemMessageValidator).validate(form, bindingResult)
        model.getViewName should equal("maintenance/messages/edit")
        model.getModel.get("systemMessage") should equal(form)
    }

    private def aSystemMessage(id: Long = -1) = {
        val systemMessageId = if (id >= 0) { BigDecimal(id) } else { null }
        new SystemMessage(systemMessageId, "aMessage" + id,
            new DateTime(2012, 10, 1, 0, 0, 0, 0),
            new DateTime(2012, 10, 2, 0, 0, 0, 0))
    }

}
