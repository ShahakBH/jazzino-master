package com.yazino.web.form.validation;

import com.yazino.web.controller.profile.PlayerProfileController;
import org.junit.Test;
import org.springframework.validation.Validator;
import org.springframework.web.multipart.MultipartFile;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AvatarMultipartFileValidatorTest extends AbstractValidatorTest {
    @Override
    protected Validator getUnderTest() {
        return new AvatarMultipartFileValidator();
    }

    @Override
    protected Class getSupportedClass() {
        return MultipartFile.class;
    }

    @Test
    public void shouldErrorIfFileNameBlank() {
        MultipartFile multipartFile = mock(MultipartFile.class);
        when(multipartFile.getOriginalFilename()).thenReturn("");
        assertErrorCodeEmpty(PlayerProfileController.AVATAR_OBJECT_KEY, multipartFile);
    }
}
