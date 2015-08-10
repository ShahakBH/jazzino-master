package com.yazino.web.domain;

import com.yazino.platform.player.Avatar;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.apache.commons.lang3.Validate.notNull;

@Service("avatarRepository")
public class AvatarRepository {
    static final String PUBLIC_SUFFIX = "public";
    static final String PRIVATE_SUFFIX = "private";
    private static final int MAX_WIDTH = 50;
    private static final int MAX_HEIGHT = 50;

    private static final Logger LOG = LoggerFactory.getLogger(AvatarRepository.class);
    private static final String[] ACCEPTED_EXTENSIONS = new String[]{"gif", "png", "jpg", "jpeg"};
    private static final Avatar EMPTY_AVATAR = new Avatar("#", "#");

    private String path;
    private String url;

    @Value("${senet.path.avatars}")
    public void setPath(final String path) {
        this.path = path;
    }

    @Value("${senet.web.avatars}")
    public void setUrl(final String url) {
        this.url = url;
        if (this.url != null && !this.url.endsWith("/")) {
            this.url = this.url + "/";
        }
    }

    public Avatar retrieveDefaultAvatar() {
        final List<Avatar> avatars = retrieveAvailableAvatars();
        if (!avatars.isEmpty()) {
            return avatars.get(0);
        }
        return EMPTY_AVATAR;
    }


    public List<Avatar> retrieveAvailableAvatars() {
        final String avatarPath = this.path + "/" + PUBLIC_SUFFIX;
        if (LOG.isDebugEnabled()) {
            LOG.debug("Retrieving public avatars from: " + avatarPath);
        }
        final File publicAvatarsDir = new File(avatarPath);
        if (!publicAvatarsDir.exists() || !publicAvatarsDir.canRead() && !publicAvatarsDir.isDirectory()) {
            LOG.error("Invalid public avatar path: " + avatarPath);
            return Collections.emptyList();
        }
        final List<Avatar> result = new ArrayList<Avatar>();
        for (File file : publicAvatarsDir.listFiles()) {
            if (!file.isHidden()) {
                final String fullFileName = PUBLIC_SUFFIX + "/" + file.getName();
                result.add(new Avatar(fullFileName, url + fullFileName));
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("URLs for public avatars:" + result);
        }
        Collections.sort(result);
        return result;
    }

    public Avatar storeAvatar(final String originalFileName,
                              final byte[] sourceBytes) {
        notNull(originalFileName, "File name is required");
        notNull(sourceBytes, "Content is required");
        final String avatarPath = this.path + "/" + PRIVATE_SUFFIX;
        if (LOG.isDebugEnabled()) {
            LOG.debug("Storing new avatar in: " + avatarPath);
        }
        final String uuid = UUID.randomUUID().toString();
        final String extension = getFileExtension(originalFileName);
        final String avatarName = uuid + "." + extension;
        final String fullPath = avatarPath + "/" + avatarName;

        final byte[] bytes = resizeImageIfRequired(extension, sourceBytes);

        BufferedOutputStream out = null;
        try {
            out = new BufferedOutputStream(new FileOutputStream(fullPath));
            out.write(bytes);
            out.close();
        } catch (IOException e) {
            LOG.error("Couldn't write " + fullPath);
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("Couldnt't write %s (filename=%s, path=%s)",
                        fullPath, originalFileName, avatarPath), e);
            }
            IOUtils.closeQuietly(out);
        }
        final String fullAvatarName = PRIVATE_SUFFIX + "/" + avatarName;
        final String avatarURL = url + fullAvatarName;
        if (LOG.isDebugEnabled()) {
            LOG.debug("New avatar should be accessible via: " + avatarURL);
        }
        return new Avatar(fullAvatarName, avatarURL);
    }

    private byte[] resizeImageIfRequired(final String fileType, final byte[] imageData) {
        if (imageData == null) {
            return null;
        }

        try {
            final BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageData));
            if (originalImage == null) {
                return imageData;
            }

            if (originalImage.getWidth() <= MAX_WIDTH && originalImage.getHeight() <= MAX_HEIGHT) {
                return imageData;
            }

            final double ratio = Math.min(
                    (double) MAX_WIDTH / (double) originalImage.getWidth(),
                    (double) MAX_HEIGHT / (double) originalImage.getHeight());
            final int targetHeight = (int) (originalImage.getHeight() * ratio);
            final int targetWidth = (int) (originalImage.getWidth() * ratio);

            BufferedImage currentImage = originalImage;
            int currentWidth = originalImage.getWidth();
            int currentHeight = originalImage.getHeight();

            // this does multiple loops, halving size until we reach the final size. This gives much better
            // quality increase than a single resize.
            do {
                if (currentWidth > targetWidth) {
                    currentWidth /= 2;
                    if (currentWidth < targetWidth) {
                        currentWidth = targetWidth;
                    }
                }

                if (currentHeight > targetHeight) {
                    currentHeight /= 2;
                    if (currentHeight < targetHeight) {
                        currentHeight = targetHeight;
                    }
                }

                final BufferedImage resizedImage = new BufferedImage(
                        currentWidth, currentHeight, BufferedImage.TYPE_INT_RGB);
                final Graphics2D g2d = resizedImage.createGraphics();
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2d.drawImage(currentImage, 0, 0, currentWidth, currentHeight, null);
                g2d.dispose();

                currentImage = resizedImage;
            }
            while (currentWidth != targetWidth && currentHeight != targetHeight);

            final ByteArrayOutputStream encoderOutputStream = new ByteArrayOutputStream();
            if ("jpg".equalsIgnoreCase(fileType) || "jpeg".equalsIgnoreCase(fileType)) {
                ImageIO.write(currentImage, "jpg", encoderOutputStream);
            } else {
                ImageIO.write(currentImage, fileType, encoderOutputStream);
            }
            return encoderOutputStream.toByteArray();
        } catch (IOException e) {
            LOG.error("Image could not be resized with type " + fileType + ", resize will be skipped", e);
            return imageData;
        }

    }

    private String getFileExtension(final String fileName) {
        final String[] tokens = fileName.split("\\.");
        if (StringUtils.isBlank(fileName) || tokens.length == 0) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("Invalid filename %s", fileName));
            }
            throw new IllegalArgumentException(String.format("Invalid filename: %s", fileName));
        }
        final String result = tokens[tokens.length - 1];
        if (!ArrayUtils.contains(ACCEPTED_EXTENSIONS, result.toLowerCase())) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("Extension %s from file %s is invalid.", result, fileName));
            }
            throw new IllegalArgumentException(String.format("Invalid extension: %s of file %s",
                    result.toLowerCase(), fileName));
        }
        return result;
    }
}
