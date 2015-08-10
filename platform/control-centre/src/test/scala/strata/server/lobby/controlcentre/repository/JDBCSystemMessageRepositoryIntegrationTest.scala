package strata.server.lobby.controlcentre.repository

import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.transaction.TransactionConfiguration
import org.scalatest.junit.AssertionsForJUnit
import org.scalatest.matchers.ShouldMatchers
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import strata.server.lobby.controlcentre.model.SystemMessage
import org.joda.time.DateTime
import org.junit.Test
import org.springframework.transaction.annotation.Transactional

@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration
@TransactionConfiguration
class JDBCSystemMessageRepositoryIntegrationTest extends AssertionsForJUnit with ShouldMatchers {

    @Autowired private var underTest: SystemMessageRepository = null
    @Autowired private val jdbcTemplate: JdbcTemplate = null

    @Transactional
    @Test def savingASystemMessageShouldWriteItToTheRepository() {
        val message = aSystemMessage()

        val savedMessage = underTest.save(message)

        val systemMessageMap = jdbcTemplate.queryForMap("SELECT * FROM SYSTEM_MESSAGE WHERE SYSTEM_MESSAGE_ID=?",
                savedMessage.id.underlying())
        systemMessageMap.get("SYSTEM_MESSAGE_ID") should equal(savedMessage.id)
        systemMessageMap.get("MESSAGE") should equal(message.message)
        new DateTime(systemMessageMap.get("VALID_FROM")) should equal(message.validFrom)
        new DateTime(systemMessageMap.get("VALID_TO")) should equal(message.validTo)
    }

    @Transactional
    @Test def updateASystemMessageShouldWriteTheUpdatesToTheRepository() {
        val savedMessage = underTest.save(aSystemMessage())
        val messageToUpdate = new SystemMessage(savedMessage.id, "anUpdatedMessage",
                new DateTime(2011, 10, 26, 10, 0, 0, 0),
                new DateTime(2011, 10, 26, 12, 0, 0, 0))

        underTest.save(messageToUpdate)

        val systemMessageMap = jdbcTemplate.queryForMap("SELECT * FROM SYSTEM_MESSAGE WHERE SYSTEM_MESSAGE_ID=?",
                savedMessage.id.underlying())
        systemMessageMap.get("SYSTEM_MESSAGE_ID") should equal(savedMessage.id)
        systemMessageMap.get("MESSAGE") should equal(messageToUpdate.message)
        new DateTime(systemMessageMap.get("VALID_FROM")) should equal(messageToUpdate.validFrom)
        new DateTime(systemMessageMap.get("VALID_TO")) should equal(messageToUpdate.validTo)
    }

    @Transactional
    @Test def deletingAMessageShouldRemoveItFromTheRepository() {
        val savedMessage = underTest.save(aSystemMessage())

        underTest.delete(savedMessage.id)

        val messageCount = jdbcTemplate.queryForInt("SELECT count(*) FROM SYSTEM_MESSAGE WHERE SYSTEM_MESSAGE_ID=?",
                savedMessage.id.underlying())

        messageCount should equal(0)
    }

    @Transactional
    @Test def deletingANonExistentMessageShouldNotCauseAnError() {
        underTest.delete(BigDecimal(-100))
    }

    @Transactional
    @Test def findingANonExistentSystemMessageReturnsNone() {
        val systemMessage = underTest.findById(BigDecimal(12))

        systemMessage should equal(None)
    }

    @Transactional
    @Test def findingAnExistingSystemMessageReturnsTheMessage() {
        val messageToFind = underTest.save(aSystemMessage())

        val systemMessage = underTest.findById(messageToFind.id)

        systemMessage.get should equal(messageToFind)
    }

    @Transactional
    @Test def findingAllMessageShouldIncludeKnownMessages() {
        val messageToFind = underTest.save(aSystemMessage())

        val systemMessages = underTest.findAll(0, Integer.MAX_VALUE)

        systemMessages.getData should contain(messageToFind)
    }

    private def aSystemMessage() =
            new SystemMessage(null, "aMessage",
                new DateTime(2011, 10, 25, 10, 0, 0, 0),
                new DateTime(2011, 10, 25, 12, 0, 0, 0))

}
