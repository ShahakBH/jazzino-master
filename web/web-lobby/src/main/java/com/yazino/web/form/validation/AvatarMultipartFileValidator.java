package com.yazino.web.form.validation;

import com.yazino.web.controller.profile.PlayerProfileController;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.multipart.MultipartFile;

public class AvatarMultipartFileValidator implements Validator {
    @Override
    public boolean supports(final Class<?> clazz) {
        return MultipartFile.class.equals(clazz);
    }

    @Override
    public void validate(final Object target, final Errors errors) {
        final MultipartFile multipartFile = (MultipartFile) target;

        ValidationTools.validateNotEmptyOrWhitespace(errors,
                PlayerProfileController.AVATAR_OBJECT_KEY,
                multipartFile.getOriginalFilename());

    }
}
