package com.yazino.web.domain;


/**
 * Maps GameTypes to friendly display names.
 */
public class GameTypeMapper {

    public String getViewName(final String gameType) {
        if (gameType == null) {
            return null;
        }

        if ("SLOTS".equals(gameType)) {
            return "wheelDeal";
        }
        return toCamelCase(gameType);
    }

    public String fromViewName(final String viewName) {
        if (viewName == null) {
            return null;
        }

        if ("wheelDeal".equals(viewName)) {
            return "SLOTS";
        }
        return fromCamelCase(viewName);
    }

    private String fromCamelCase(final String viewName) {
        if (viewName == null) {
            return null;
        }

        final StringBuilder gameTypeName = new StringBuilder();

        for (char character : viewName.toCharArray()) {
            if (Character.isUpperCase(character)) {
                gameTypeName.append('_');
            }

            gameTypeName.append(Character.toUpperCase(character));
        }

        return gameTypeName.toString();
    }

    private String toCamelCase(final String gameType) {
        if (gameType == null) {
            return null;
        }

        final StringBuilder camelCaseName = new StringBuilder();

        boolean caseSwitchNext = false;
        for (char character : gameType.toCharArray()) {
            if (character == '_') {
                caseSwitchNext = true;
                continue;
            }

            if (caseSwitchNext) {
                camelCaseName.append(Character.toUpperCase(character));
                caseSwitchNext = false;
            } else {
                camelCaseName.append(Character.toLowerCase(character));
            }
        }

        return camelCaseName.toString();
    }

}
