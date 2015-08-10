package com.yazino.platform.persistence;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;

@SpaceClass(replicate = false)
public class SomeSpaceClass {
    private String id;
    private String value;

    public SomeSpaceClass() {
    }

    public SomeSpaceClass(String id, String value) {
        this.id = id;
        this.value = value;
    }

    @SpaceId
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SomeSpaceClass that = (SomeSpaceClass) o;

        if (id != null ? !id.equals(that.id) : that.id != null) {
            return false;
        }
        if (value != null ? !value.equals(that.value) : that.value != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "SomeSpaceClass{" +
                "id='" + id + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
