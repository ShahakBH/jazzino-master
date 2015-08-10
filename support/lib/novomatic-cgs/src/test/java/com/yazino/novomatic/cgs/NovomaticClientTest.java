package com.yazino.novomatic.cgs;

import com.yazino.novomatic.cgs.message.NovomaticGameDefinition;
import com.yazino.novomatic.cgs.message.RequestGameList;
import com.yazino.novomatic.cgs.msgpack.MessagePackMapper;
import com.yazino.novomatic.cgs.transport.ClientTransport;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
@RunWith(MockitoJUnitRunner.class)
public class NovomaticClientTest {

    private static final byte[] REQUEST_BYTES = new byte[1];
    private static final byte[] RESPONSE_BYTES = new byte[2];

    @Mock
    private ClientTransport tranport;
    @Mock
    private MessagePackMapper mapper;
    private NovomaticClient underTest;

    @Before
    public void setUp() {
        underTest = new NovomaticClient(tranport, mapper);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NovomaticError.class)
    public void shouldHandleGameEngineError() throws NovomaticError, IOException {
        when(mapper.write(new RequestGameList().toMap())).thenReturn(REQUEST_BYTES);
        when(tranport.sendRequest(REQUEST_BYTES)).thenReturn(RESPONSE_BYTES);
        final Map errorResponse = new HashMap();
        errorResponse.put("type", "rsp_gmengine_error");
        when(mapper.read(RESPONSE_BYTES, Map.class, "gmstate")).thenReturn(errorResponse);
        underTest.getAvailableGames();
    }

    @Test
    public void shouldGetGameList() throws IOException, NovomaticError {
        //{type="rsp_gmengine_list_descr", games=[{"type":"game_descr","id":90101,"version":"1.0","name":"Just Jewels","pop":96.0}]}
        when(mapper.write(new RequestGameList().toMap())).thenReturn(REQUEST_BYTES);
        when(tranport.sendRequest(REQUEST_BYTES)).thenReturn(RESPONSE_BYTES);
        final Map serverResponse = new HashMap();
        serverResponse.put("type", "rsp_gmengine_list_descr");
        Map aGame = new HashMap();
        aGame.put("type", "game_descr");
        aGame.put("id", 90101l);
        aGame.put("name", "Just Jewels");
        serverResponse.put("games", Arrays.asList(aGame));
        when(mapper.read(RESPONSE_BYTES, Map.class, "gmstate")).thenReturn(serverResponse);
        final List<NovomaticGameDefinition> expected = Arrays.asList(new NovomaticGameDefinition(90101l, "Just Jewels"));
        final List<NovomaticGameDefinition> actual = underTest.getAvailableGames();
        assertThat(actual, is(equalTo(expected)));
    }
}
