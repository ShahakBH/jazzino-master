package com.yazino.web.api;

import com.yazino.platform.Platform;
import com.yazino.web.data.GameTypeRepository;
import org.apache.commons.lang3.Validate;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import static com.yazino.web.api.ValidationTools.rejectIfEmptyOrWhitespace;
import static com.yazino.web.api.ValidationTools.rejectUnsupportedValue;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Validates an {@link InvitationSentRecord}.
 */
public class InvitationSentRecordValidator implements Validator {

    private static final String PLATFORM = "platform";
    private static final String GAME_TYPE = "gameType";
    private static final String SOURCE_IDS = "sourceIds";

    private final GameTypeRepository gameTypeRepository;

    public InvitationSentRecordValidator(GameTypeRepository gameTypeRepository) {
        Validate.notNull(gameTypeRepository);
        this.gameTypeRepository = gameTypeRepository;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return clazz.equals(InvitationSentRecord.class);
    }

    @Override
    public void validate(Object target, Errors errors) {
        InvitationSentRecord record = (InvitationSentRecord) target;
        validateGameType(errors, record);
        validatePlatform(errors, record);
        validateSourceIds(errors, record);
    }

    private void validateSourceIds(Errors errors, InvitationSentRecord record) {
        if (record.getSourceIds() == null || record.getSourceIds().trim().length() == 0) {
            errors.rejectValue(SOURCE_IDS, "empty", SOURCE_IDS + " must be present");
            return;
        }
        String[] sourceIds = record.getSourceIds().split(",");
        for (String sourceId : sourceIds) {
            if (isBlank(sourceId)) {
                errors.rejectValue(SOURCE_IDS, "nullOrBlankSourceId", SOURCE_IDS + " cannot be blank");
                break;
            }
        }
    }

    private void validatePlatform(Errors errors, InvitationSentRecord record) {
        String platformInput = record.getPlatform();
        rejectIfEmptyOrWhitespace(errors, PLATFORM, platformInput);
        if (platformInput != null) {
            try {
                Platform.valueOf(platformInput);
            } catch (Exception e) {
                rejectUnsupportedValue(errors, PLATFORM);
            }
        }
    }

    private void validateGameType(Errors errors, InvitationSentRecord record) {
        String gameType = record.getGameType();
        rejectIfEmptyOrWhitespace(errors, GAME_TYPE, gameType);
        if (!gameTypeRepository.getGameTypes().containsKey(gameType)) {
            rejectUnsupportedValue(errors, GAME_TYPE);
        }
    }

}
