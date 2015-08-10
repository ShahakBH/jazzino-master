package strata.server.lobby.promotion.matchers;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.TypeSafeMatcher;
import strata.server.lobby.promotion.domain.ExternalCredentials;

public class ExternalIdIsEqual extends TypeSafeMatcher<ExternalCredentials> {
    private ExternalCredentials externalCredentials;

    public ExternalIdIsEqual(final ExternalCredentials externalCredentials) {
        this.externalCredentials = externalCredentials;
    }

    @Override
    protected boolean matchesSafely(final ExternalCredentials other) {
        return EqualsBuilder.reflectionEquals(externalCredentials,  other);
    }

    @Override
    public void describeTo(final Description description) {
        description.appendText(externalCredentials == null ? "null" : externalCredentials.toString());
    }

    @Factory
    public static <T> TypeSafeMatcher<ExternalCredentials> equalTo(ExternalCredentials externalCredentials) {
        return new ExternalIdIsEqual(externalCredentials);
    }
}