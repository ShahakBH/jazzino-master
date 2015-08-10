package com.yazino.web.domain.email;

import com.yazino.platform.player.service.PlayerProfileService;
import org.junit.Test;

import java.math.BigDecimal;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class AbstractEmailBuilderTest {

    private final AbstractEmailBuilder builder = new AbstractEmailBuilder() {
        @Override
        public EmailRequest buildRequest(PlayerProfileService profileService) {
            return null;
        }
    };


    @Test
    public void shouldAddTemplateProperty() throws Exception {
        String key = "Foo";
        String value = "bar";
        builder.setTemplateProperty(key, value);
        assertEquals(1, builder.getTemplateProperties().size());
        assertEquals(value, builder.getTemplateProperties().get(key));
        assertEquals(value, builder.getTemplateProperty(key));
    }

    @Test
    public void shouldAddOtherProperty() throws Exception {
        String value = "TestValue";
        String key = "TestKey";
        builder.setOtherProperty(key, value);
        assertEquals(1, builder.getOtherProperties().size());
        assertEquals(value, builder.getOtherProperties().get(key));
        assertEquals(value, builder.getOtherProperty(key));
    }

    @Test
    public void shouldAddPlayerId() throws Exception {
        builder.withPlayerId(BigDecimal.TEN);
        assertEquals(BigDecimal.TEN, builder.getPlayerId());
    }

    @Test
    public void formattingNameShouldAddName(){
        assertThat(builder.formattedEmailWithName("bob", "from@your.mum"), equalTo("bob <from@your.mum>"));
    }

    @Test
    public void formattingNameShouldReplaceNameWhereNeeded(){
        assertThat(builder.formattedEmailWithName("bob", "mclovin <from@your.mum>"), equalTo("bob <from@your.mum>"));
        assertThat(builder.formattedEmailWithName("bob", "mclovin <from@your.mum"), equalTo("bob <from@your.mum>"));
        assertThat(builder.formattedEmailWithName("bob", "mclovin<from@your.mum"), equalTo("bob <from@your.mum>"));
        assertThat(builder.formattedEmailWithName("bob", "i am mclovin <from@your.mum>"), equalTo("bob <from@your.mum>"));
    }
}
