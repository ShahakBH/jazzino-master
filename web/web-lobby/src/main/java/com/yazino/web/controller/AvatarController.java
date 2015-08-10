package com.yazino.web.controller;

import com.yazino.platform.player.Avatar;
import com.yazino.web.domain.AvatarRepository;
import com.yazino.web.form.AvatarForm;
import com.yazino.web.util.WebApiResponses;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.support.ByteArrayMultipartFileEditor;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.Validate.notNull;

@Controller
public class AvatarController {
    private static final Logger LOG = LoggerFactory.getLogger(AvatarController.class);

    public static final String MAPPING = "/publicCommand/avatars";

    private final AvatarRepository avatarRepository;
    private final WebApiResponses webApiResponses;

    @Autowired(required = true)
    public AvatarController(@Qualifier("avatarRepository") final AvatarRepository avatarRepository,
                            final WebApiResponses webApiResponses) {
        notNull(avatarRepository, "avatarRepository is null");
        notNull(webApiResponses, "webApiResponses is null");

        this.avatarRepository = avatarRepository;
        this.webApiResponses = webApiResponses;
    }

    @InitBinder
    protected void initBinder(final ServletRequestDataBinder binder)
            throws ServletException {
        binder.registerCustomEditor(byte[].class, new ByteArrayMultipartFileEditor());
    }

    @ModelAttribute("publicAvatars")
    public List<Avatar> populateAvatars() {
        return avatarRepository.retrieveAvailableAvatars();
    }

    @ModelAttribute("avatar")
    public AvatarForm retrieveEmptyForm() {
        return new AvatarForm();
    }

    @RequestMapping(value = MAPPING, method = RequestMethod.GET)
    public void showAvailableAvatars(final HttpServletResponse response) throws IOException {
        webApiResponses.writeOk(response, createJsonForAvailableAvatars());
    }

    @RequestMapping(value = MAPPING, method = RequestMethod.POST)
    public void uploadAvatar(@ModelAttribute("avatar") final AvatarForm form,
                             final HttpServletResponse response)
            throws IOException {
        notNull(form, "form is required");

        if (LOG.isDebugEnabled()) {
            LOG.debug("Received data to upload {}", ToStringBuilder.reflectionToString(form.getFile()));
        }

        final MultipartFile file = form.getFile();
        String error = null;
        Avatar avatar = null;
        if (file != null && StringUtils.isNotBlank(file.getOriginalFilename())) {
            try {
                avatar = avatarRepository.storeAvatar(file.getOriginalFilename(), file.getBytes());
            } catch (Exception e) {
                LOG.error("Couldn't read file: {}", e.getMessage(), e);
                error = e.getMessage();
            }
        } else {
            error = "Couldn't read file";
        }

        final Map<String, Object> result = new HashMap<>();
        if (error != null) {
            result.put("error", error);
        } else if (avatar != null) {
            result.put("avatar", asJson(avatar));
        }
        webApiResponses.writeOk(response, result);
    }

    private Map<String, Object> createJsonForAvailableAvatars() {
        final List<Avatar> avatars = avatarRepository.retrieveAvailableAvatars();
        final List<Map> avatarsJson = new ArrayList<>();
        for (Avatar avatar : avatars) {
            avatarsJson.add(asJson(avatar));
        }

        final Map<String, Object> jsonContent = new HashMap<>();
        jsonContent.put("avatars", avatarsJson);
        return jsonContent;
    }

    private Map<String, Object> asJson(final Avatar avatar) {
        final Map<String, Object> avatarJson = new HashMap<>();
        avatarJson.put("url", avatar.getUrl());
        avatarJson.put("pictureLocation", avatar.getPictureLocation());
        return avatarJson;
    }

}
