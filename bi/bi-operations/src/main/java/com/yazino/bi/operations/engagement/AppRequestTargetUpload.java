package com.yazino.bi.operations.engagement;

import com.yazino.engagement.ChannelType;
import org.springframework.web.multipart.MultipartFile;

public class AppRequestTargetUpload {
    private Integer id;
    private MultipartFile file;
    private ChannelType channelType;

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public MultipartFile getFile() {
        return file;
    }

    public void setFile(final MultipartFile file) {
        this.file = file;
    }

    public ChannelType getChannelType() {
        return channelType;
    }

    public void setChannelType(final ChannelType channelType) {
        this.channelType = channelType;
    }
}
