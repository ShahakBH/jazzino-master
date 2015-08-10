package com.yazino.bi.opengraph.diagnostic;

import com.yazino.bi.opengraph.AccessTokenStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Map;

@Controller
public class AccessTokenStoreViewer {

    @Autowired
    private AccessTokenStore accessTokenStore;

    @RequestMapping(value = {"/opengraph/accesstokens"}, method = RequestMethod.GET)
    public void updateCredentials(
            @RequestParam(value = "playerId", required = false) final BigInteger playerId,
            @RequestParam(value = "gameType", required = false) final String gameType,
            final HttpServletResponse response) throws IOException {

        response.setContentType(MediaType.TEXT_PLAIN.toString());
        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        final StringBuilder stringBuilder = new StringBuilder();

        final Map<AccessTokenStore.Key, AccessTokenStore.AccessToken> accessTokensByKey = accessTokenStore.getAccessTokensByKey();

        if (playerId != null && gameType != null) {
            final AccessTokenStore.AccessToken entry = accessTokensByKey.get(new AccessTokenStore.Key(playerId, gameType));
            stringBuilder.append("Entry for: playerId ").append(playerId)
                    .append(" gameType ").append(gameType).append("\n");
            stringBuilder.append(entry);
        } else {
            for (Map.Entry<AccessTokenStore.Key, AccessTokenStore.AccessToken> entry : accessTokensByKey.entrySet()) {
                stringBuilder.append(entry.getKey()).append(" ").append(entry.getValue()).append("\n");
            }
        }

        response.getWriter().write(stringBuilder.toString());
    }

}
