package com.yazino.platform.persistence.session;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import com.yazino.game.api.NewsEvent;
import com.yazino.game.api.NewsEventType;
import com.yazino.game.api.ParameterisedMessage;

import java.io.*;
import java.math.BigDecimal;

public class NewsEventSerializer {
    private static final String UTF8 = "UTF-8";
    private static final int PLAYER_ID_INDEX = 0;
    private static final int TYPE_INDEX = 1;
    private static final int MESSAGE_INDEX = 2;
    private static final int SHORT_MESSAGE_INDEX = 3;
    private static final int IMAGE_INDEX = 4;
    private static final int DELAY_INDEX = 5;
    private static final int GAME_TYPE_INDEX = 6;
    private static final int LEGACY_FIELD_COUNT = 7;

    public String serialize(final NewsEvent news) {
        if (news == null) {
            return "";
        }

        ObjectOutputStream objectData = null;

        try {
            final ByteArrayOutputStream serialisedBytes = new ByteArrayOutputStream();
            objectData = new ObjectOutputStream(serialisedBytes);
            objectData.writeObject(news);
            return new String(Base64.encodeBase64(serialisedBytes.toByteArray()), UTF8);

        } catch (IOException e) {
            throw new RuntimeException("Serialisation failed for " + news, e);

        } finally {
            IOUtils.closeQuietly(objectData);
        }
    }

    public NewsEvent deserialize(final String serialisedNews) {
        if (StringUtils.isBlank(serialisedNews)) {
            return null;
        }

        ObjectInputStream objectData = null;
        try {
            final byte[] serialisedNewsEvent = Base64.decodeBase64(serialisedNews.getBytes(UTF8));
            objectData = new ObjectInputStream(new ByteArrayInputStream(serialisedNewsEvent));

            return (NewsEvent) objectData.readObject();

        } catch (Exception e) {
            if (isLegacyFormat(serialisedNews)) {
                return decodeLegacyFormat(serialisedNews);
            }
            throw new RuntimeException("Deserialisation failed", e);

        } finally {
            IOUtils.closeQuietly(objectData);
        }
    }

    private boolean isLegacyFormat(final String serialisedNews) {
        return serialisedNews.split("\n").length >= LEGACY_FIELD_COUNT;
    }

    private NewsEvent decodeLegacyFormat(final String news) {
        final String[] lines = news.split("\n");
        final BigDecimal playerId = new BigDecimal(lines[PLAYER_ID_INDEX]);
        final NewsEventType type = NewsEventType.valueOf(lines[TYPE_INDEX]);
        final ParameterisedMessage message = deserializeParameterisedMessage(lines[MESSAGE_INDEX]);
        final ParameterisedMessage shortMessage = deserializeParameterisedMessage(lines[SHORT_MESSAGE_INDEX]);
        final String image = lines[IMAGE_INDEX];
        final long delay = Long.valueOf(lines[DELAY_INDEX]);
        final String gameType = getNullableString(lines[GAME_TYPE_INDEX]);
        return new NewsEvent.Builder(playerId, message)
                .setType(type)
                .setShortDescription(shortMessage)
                .setImage(image)
                .setDelay(delay)
                .setGameType(gameType)
                .build();
    }

    private String getNullableString(final String line) {
        if ("null".equals(line)) {
            return null;
        } else {
            return line;
        }
    }

    private ParameterisedMessage deserializeParameterisedMessage(final String line) {
        if (StringUtils.isBlank(line)) {
            return null;
        }
        final String[] tokens = line.split("\t");
        if (tokens.length == 0) {
            return null;
        }
        final String message = tokens[0];
        final Object[] parameters;
        if (tokens.length > 0) {
            parameters = ArrayUtils.subarray(tokens, 1, tokens.length);
        } else {
            parameters = new Object[0];
        }
        return new ParameterisedMessage(message, parameters);
    }
}
