package com.yazino.platform.processor.chat;

import com.yazino.platform.community.ProfanityFilter;
import com.yazino.platform.messaging.DocumentDispatcher;
import com.yazino.platform.repository.chat.ChatRepository;
import com.yazino.platform.repository.community.PlayerRepository;
import com.yazino.platform.repository.session.PlayerSessionRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class ChatMessageWorkerTest {
    private ChatMessageWorker underTest;

    @Mock
    private PlayerRepository playerRepository;
    @Mock
    private PlayerSessionRepository playerSessionRepository;
    @Mock
    private ChatRepository gigaspacesChatRepository;
    @Mock
    private ProfanityFilter profanityFilter;
    @Mock
    private DocumentDispatcher documentDispatcher;
    @Mock
    private ChatChannelWorker chatChannelWorker;

    @Before
    public void init() {
        underTest = new ChatMessageWorker(playerRepository, playerSessionRepository, gigaspacesChatRepository, profanityFilter, documentDispatcher, chatChannelWorker);
    }

    @Test
    public void shouldFilerMessageAndEscapeForHTML() {
        String messageTextWithHtml = "<html><body><title>Test text</title><img src=\"http://bob\" alt=alternative text\"> Bob's cash is < than Jim's, <b>is</b> it? <a href=\"/account/signup\">Yes, really!</a>";
        String expectedMessageText = "&lt;html&gt;&lt;body&gt;&lt;title&gt;Test text&lt;/title&gt;&lt;img src=&quot;http://bob&quot; alt=alternative text&quot;&gt; Bob's cash is &lt; than Jim's, &lt;b&gt;is&lt;/b&gt; it? &lt;a href=&quot;/account/signup&quot;&gt;Yes, really!&lt;/a&gt;";
        String filteredMessageText = underTest.filterMessage(messageTextWithHtml);
        assertEquals(expectedMessageText, filteredMessageText);

        messageTextWithHtml = "Bob's cash is < than Jim's";
        expectedMessageText = "Bob's cash is &lt; than Jim's";
        filteredMessageText = underTest.filterMessage(messageTextWithHtml);
        assertEquals(expectedMessageText, filteredMessageText);

        messageTextWithHtml = "<a>Bob's <! cash is</a> > than Jim's";
        expectedMessageText = "&lt;a&gt;Bob's &lt;! cash is&lt;/a&gt; &gt; than Jim's";
        filteredMessageText = underTest.filterMessage(messageTextWithHtml);
        assertEquals(expectedMessageText, filteredMessageText);

        messageTextWithHtml = "<>dsfgd<dfv";
        expectedMessageText = "&lt;&gt;dsfgd&lt;dfv";
        filteredMessageText = underTest.filterMessage(messageTextWithHtml);
        assertEquals(expectedMessageText, filteredMessageText);
    }

    @Test
    public void shouldFilerMessageForHTMLEntities() {
        final String messageWithEntities = "Hello &gt; my baby, hello my &lt; honey, hello my &amp; ragtime gal!";
        final String expectedText = "Hello &amp;gt; my baby, hello my &amp;lt; honey, hello my &amp;amp; ragtime gal!";

        assertEquals(expectedText, underTest.filterMessage(messageWithEntities));
    }
}
