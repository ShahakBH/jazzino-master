package com.yazino.platform.grid;

import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceIndex;
import com.gigaspaces.annotation.pojo.SpaceRouting;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.openspaces.core.cluster.ClusterInfo;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@RunWith(MockitoJUnitRunner.class)
public class RoutingTest {
    private final static int PARTITION_NUMBER = 2;
    private final static int PARTITION_TOTAL = 8;

    private Routing underTest;

    @Before
    public void setUp() {
        final ClusterInfo clusterInfo = new ClusterInfo();
        clusterInfo.setInstanceId(PARTITION_NUMBER);
        clusterInfo.setNumberOfInstances(PARTITION_TOTAL);

        underTest = new Routing();
        underTest.setClusterInfo(clusterInfo);
    }

    @Test(expected = NullPointerException.class)
    public void gettingTheSpaceIdOfANullObjectThrowsANullPointerException() {
        underTest.spaceIdFor(null);
    }

    @Test
    public void gettingTheSpaceIdOfBigDecimalReturnsTheBigDecimal() {
        assertThat(underTest.spaceIdFor(BigDecimal.valueOf(100)), is(equalTo((Object) BigDecimal.valueOf(100))));
    }

    @Test
    public void gettingTheSpaceIdOfBigIntegerReturnsTheBigDecimal() {
        assertThat(underTest.spaceIdFor(BigInteger.valueOf(100)), is(equalTo((Object) BigInteger.valueOf(100))));
    }

    @Test
    public void gettingTheSpaceIdOfStringReturnsTheString() {
        assertThat(underTest.spaceIdFor("aString"), is(equalTo((Object) "aString")));
    }

    @Test
    public void gettingTheSpaceIdOfShortReturnsTheShort() {
        assertThat(underTest.spaceIdFor((short) 3), is(equalTo((Object) (short) 3)));
    }

    @Test
    public void gettingTheSpaceIdOfIntegerReturnsTheInteger() {
        assertThat(underTest.spaceIdFor(100), is(equalTo((Object) 100)));
    }

    @Test
    public void gettingTheSpaceIdOfLongReturnsTheLong() {
        assertThat(underTest.spaceIdFor(100L), is(equalTo((Object) 100L)));
    }

    @Test
    public void gettingTheSpaceIdOfAnObjectWithASpaceRoutingAnnotationReturnsTheRoutingProperty() {
        assertThat(underTest.spaceIdFor(new SpaceRoutingObject()), is(equalTo((Object) "propertyTwo")));
    }

    @Test
    public void gettingTheSpaceIdOfAnObjectWithNoRoutingAndASpaceIdAnnotationReturnsTheIdProperty() {
        assertThat(underTest.spaceIdFor(new SpaceIdObject()), is(equalTo((Object) "propertyOne")));
    }

    @Test
    public void gettingTheSpaceIdOfAnObjectWithNoRoutingOrIdAndASpaceIndexAnnotationReturnsTheAlphabeticFirstIndexProperty() {
        assertThat(underTest.spaceIdFor(new SpaceIndexedObject()), is(equalTo((Object) "propertyOne")));
    }

    @Test
    public void gettingTheSpaceIdOfAnObjectWithNoRoutingOrIdOrIndexReturnsTheAlphabeticFirstProperty() {
        assertThat(underTest.spaceIdFor(new SpacePropertyObject()), is(equalTo((Object) "propertyOne")));
    }

    @Test(expected = RuntimeException.class)
    public void gettingTheSpaceIdOfAnObjectWithNoDefinedPropertiesThrowsARuntimeException() {
        underTest.spaceIdFor(new Object());
    }

    @Test
    public void thePartitionIdOfTheProvidedSpaceIsReturned() {
        assertThat(underTest.partitionId(), is(equalTo(PARTITION_NUMBER)));
    }

    @Test
    public void thePartitionIdOfTheProvidedSpaceIsOneIfNotClustered() {
        underTest.setClusterInfo(null);

        assertThat(underTest.partitionId(), is(equalTo(1)));
    }

    @Test
    public void theTotalPartitionCountOfTheProvidedSpaceIsReturned() {
        assertThat(underTest.partitionCount(), is(equalTo(PARTITION_TOTAL)));
    }

    @Test
    public void theTotalPartitionCountOfAnUnclusteredSpaceIsOne() {
        underTest.setClusterInfo(null);

        assertThat(underTest.partitionCount(), is(equalTo(1)));
    }

    @Test
    public void aCurrentPartitionMatchCheckReturnsTrueWhenTheCurrentPartitionMatchesTheRouting() {
        assertThat(underTest.isRoutedToCurrentPartition(new SpaceIdObject()), is(true));
    }

    @Test
    public void aCurrentPartitionMatchCheckReturnsFalseWhenTheCurrentPartitionDoesNotMatchTheRouting() {
        assertThat(underTest.isRoutedToCurrentPartition(new SpaceRoutingObject()), is(false));
    }

    @Test(expected = NullPointerException.class)
    public void aCurrentPartitionMatchCheckThrowsANullPointerExceptionForANullObject() {
        assertThat(underTest.isRoutedToCurrentPartition(null), is(false));
    }

    @Test
    public void aPartitionIsNotABackupIfItHasNoBackupId() {
        underTest.setClusterInfo(new ClusterInfo("aSchema", 1, null, 1, null));

        assertThat(underTest.isBackup(), is(false));
    }

    @Test
    public void aPartitionIsNotABackupIfItHasNoClusterInfo() {
        underTest.setClusterInfo(null);

        assertThat(underTest.isBackup(), is(false));
    }

    @Test
    public void aPartitionIsABackupIfPostBackupHasBeenCalled() {
        underTest.postBackup();

        assertThat(underTest.isBackup(), is(true));
    }

    @Test
    public void aPartitionIsNotABackupIfPreBackupHasBeenCalled() {
        underTest.preBackup();

        assertThat(underTest.isBackup(), is(false));
    }

    @Test
    public void aPartitionIsNotABackupIfPrePrimaryHasBeenCalled() {
        underTest.prePrimary();

        assertThat(underTest.isBackup(), is(false));
    }

    @Test
    public void aPartitionIsNotABackupIfPostPrimaryHasBeenCalled() {
        underTest.postPrimary();

        assertThat(underTest.isBackup(), is(false));
    }

    @Test
    public void aPartitionIsNotABackupByDefault() {
        assertThat(underTest.isBackup(), is(false));
    }

    private class SpaceRoutingObject {
        @SpaceId
        public String getPropertyOne() {
            return "propertyOne";
        }

        @SpaceRouting
        public String getPropertyTwo() {
            return "propertyTwo";
        }
    }

    private class SpaceIdObject {
        @SpaceId
        public String getPropertyOne() {
            return "propertyOne";
        }

        @SpaceIndex
        public String getPropertyTwo() {
            return "propertyTwo";
        }
    }

    private class SpaceIndexedObject {
        @SpaceIndex
        public String getPropertyTwo() {
            return "propertyTwo";
        }

        @SpaceIndex
        public String getPropertyOne() {
            return "propertyOne";
        }

        public String getPropertyThree() {
            return "propertyThree";
        }
    }

    private class SpacePropertyObject {
        public String getPropertyTwo() {
            return "propertyTwo";
        }

        public String getPropertyOne() {
            return "propertyOne";
        }

        public String getPropertyThree() {
            return "propertyThree";
        }
    }

}
