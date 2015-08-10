package com.yazino.bi.operations.engagement;

import com.yazino.engagement.ChannelType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.validation.BindingResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.mockito.MockitoAnnotations.initMocks;

public class GameTypeValidatorTest {
    @Mock
    private BindingResult bindingResult;
    GameTypeValidator underTest;
    ArrayList<String> validGameTypes = new ArrayList<String>();
    Set<AppRequestTarget> targets;

    @Before
    public void setup() {
        validGameTypes.add("known type");
        targets = new HashSet<AppRequestTarget>();
        initMocks(this);
        underTest = new GameTypeValidator(validGameTypes);

    }

    @Test
    public void testValidateReturnsErrorOnBlankGameType() throws Exception {
        targets.add(new AppRequestTargetBuilder().withId(123).withGameType("").build());
        underTest.validate(targets, bindingResult);
        Mockito.verify(bindingResult).rejectValue(null, "unknown", " is an unknown gameType for Id: " + "123");
    }

    @Test
    public void testValidateReturnsErrorOnUnknownGameType() throws Exception {
        targets.add(new AppRequestTargetBuilder().withGameType(ChannelType.IOS.name()).withId(123).withGameType("unknown type").build());
        underTest.validate(targets, bindingResult);
        Mockito.verify(bindingResult).rejectValue(null, "unknown", "unknown type is an unknown gameType for Id: " + "123");
    }

    @Test
    public void testValidateShouldNotErrorOnValidGameType() throws Exception {
        targets.add(new AppRequestTargetBuilder().withGameType(ChannelType.IOS.name()).withId(123).withGameType("known type").build());
        underTest.validate(targets, bindingResult);
        Mockito.verifyZeroInteractions(bindingResult);
    }

    @Test
    public void testValidateShouldRemoveInvalidTargetsFromSet() {
        targets.add(new AppRequestTargetBuilder().withGameType(ChannelType.IOS.name()).withId(123).withGameType("unknown type").build());
        targets.add(new AppRequestTargetBuilder().withId(123).withGameType("").build());
        targets.add(new AppRequestTargetBuilder().withGameType(ChannelType.IOS.name()).withId(123).withGameType("known type").build());

        Set<AppRequestTarget> expectedTargets = new HashSet<AppRequestTarget>();
        expectedTargets.add(new AppRequestTargetBuilder().withGameType(ChannelType.IOS.name()).withId(123).withGameType("known type").build());

        underTest.validate(targets, bindingResult);
        assertEquals(expectedTargets, targets);
    }
}
