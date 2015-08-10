package com.yazino.web.domain.social;

public class Buddy {
    public Buddy(final String name, final String id, final String provider, String externalId) {
        this.name = name;
        this.id = id;
        this.provider = provider;
        this.externalId = externalId;
    }

    private String name;
    private final String id;
    private final String provider;
    private final String externalId;

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Buddy buddy = (Buddy) o;

        if (externalId != null ? !externalId.equals(buddy.externalId) : buddy.externalId != null) {
            return false;
        }
        if (id != null ? !id.equals(buddy.id) : buddy.id != null) {
            return false;
        }
        if (name != null ? !name.equals(buddy.name) : buddy.name != null) {
            return false;
        }
        if (provider != null ? !provider.equals(buddy.provider) : buddy.provider != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (provider != null ? provider.hashCode() : 0);
        result = 31 * result + (externalId != null ? externalId.hashCode() : 0);
        return result;
    }

    public String getId() {
        return id;
    }

    public String getProvider() {
        return provider;
    }

    public String getExternalId() {
        return externalId;
    }


    public String getName() {
        return name;
    }

}
