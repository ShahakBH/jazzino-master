package com.yazino.platform.grid;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openspaces.core.GigaSpace;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SpaceAccessTest {
    @Mock
    private GigaSpace localSpace;
    @Mock
    private GigaSpace globalSpace;
    @Mock
    private Routing routing;

    private SpaceAccess underTest;

    @Before
    public void setUp() {
        underTest = new SpaceAccess(localSpace, globalSpace, routing);
    }

    @Test(expected = NullPointerException.class)
    public void spaceAccessCannotBeCreatedWithANullLocalSpace() {
        new SpaceAccess(null, globalSpace, routing);
    }

    @Test(expected = NullPointerException.class)
    public void spaceAccessCannotBeCreatedWithANullGlobalSpace() {
        new SpaceAccess(localSpace, null, routing);
    }

    @Test(expected = NullPointerException.class)
    public void spaceAccessCannotBeCreatedWithANullRouting() {
        new SpaceAccess(localSpace, globalSpace, null);
    }

    @Test
    public void localReturnsTheLocalSpace() {
        assertThat(underTest.local(), is(equalTo(localSpace)));
    }

    @Test
    public void globalReturnsTheGlobalSpace() {
        assertThat(underTest.global(), is(equalTo(globalSpace)));
    }

    @Test(expected = NullPointerException.class)
    public void forRoutingThrowsANullPointerExceptionForANullRoutingObject() {
        underTest.forRouting(null);
    }

    @Test
    public void forRoutingReturnsTheLocalSpaceForALocallyRoutedObject() {
        final Object localObject = new Object();
        when(routing.isRoutedToCurrentPartition(localObject)).thenReturn(true);

        final GigaSpace matchedSpace = underTest.forRouting(localObject);

        assertThat(matchedSpace, is(equalTo(localSpace)));
    }

    @Test
    public void forRoutingReturnsTheGlobalSpaceForARemotelyRoutedObject() {
        final Object remoteObject = new Object();
        when(routing.isRoutedToCurrentPartition(remoteObject)).thenReturn(false);

        final GigaSpace matchedSpace = underTest.forRouting(remoteObject);

        assertThat(matchedSpace, is(equalTo(globalSpace)));
    }

    @Test(expected = NullPointerException.class)
    public void isRoutedLocallyThrowsANullPointerExceptionForANullRoutingObject() {
        underTest.isRoutedLocally(null);
    }

    @Test
    public void isRoutedLocallyReturnsTrueForALocallyRoutedObject() {
        final Object localObject = new Object();
        when(routing.isRoutedToCurrentPartition(localObject)).thenReturn(true);

        assertThat(underTest.isRoutedLocally(localObject), is(equalTo(true)));
    }

    @Test
    public void forRoutingReturnsFalseForARemotelyRoutedObject() {
        final Object remoteObject = new Object();
        when(routing.isRoutedToCurrentPartition(remoteObject)).thenReturn(false);

        assertThat(underTest.isRoutedLocally(remoteObject), is(equalTo(false)));
    }
}
