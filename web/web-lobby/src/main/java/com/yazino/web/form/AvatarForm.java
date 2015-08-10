package com.yazino.web.form;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.web.multipart.MultipartFile;

import java.io.Serializable;

public class AvatarForm implements Serializable {
    private static final long serialVersionUID = 7241530512266766625L;

    public AvatarForm() {
    }

    public AvatarForm(final MultipartFile file) {
        this.file = file;
    }

    private MultipartFile file;

    public void setFile(final MultipartFile file) {
        this.file = file;
    }

    public MultipartFile getFile() {
        return file;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }

        if (this == obj) {
            return true;
        }

        if (obj.getClass() != getClass()) {
            return false;
        }

        final AvatarForm rhs = (AvatarForm) obj;
        return new EqualsBuilder()
                .append(file, rhs.file)
                .isEquals();
    }


    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 57)
                .append(file)
                .toHashCode();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
