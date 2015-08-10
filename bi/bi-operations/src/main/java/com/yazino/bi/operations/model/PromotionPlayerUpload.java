package com.yazino.bi.operations.model;

import org.springframework.web.multipart.MultipartFile;

public class PromotionPlayerUpload {
    private Long promotionId;

    public Long getPromotionId() {
        return promotionId;
    }

    public void setPromotionId(final Long promotionId) {
        this.promotionId = promotionId;
    }

    public MultipartFile getFile() {
        return file;
    }

    public void setFile(final MultipartFile file) {
        this.file = file;
    }

    private MultipartFile file;
}
