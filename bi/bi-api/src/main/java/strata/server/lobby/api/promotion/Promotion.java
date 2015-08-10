package strata.server.lobby.api.promotion;

import com.yazino.platform.Platform;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public abstract class Promotion implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int MIN_SEED_VALUE = 0;
    public static final int MAX_SEED_VALUE = 99;
    public static final int MIN_CONTROL_GROUP_PERCENTAGE = 0;
    public static final int MAX_CONTROL_GROUP_PERCENTAGE = 100;

    private PromotionType promotionType;
    private Long id;
    private String name;
    private List<Platform> platforms = new ArrayList<>();
    private DateTime startDate;
    private DateTime endDate;
    private Integer priority; // optional
    private boolean allPlayers;
    private int playerCount = 0;
    private int seed;
    private int controlGroupPercentage;
    private ControlGroupFunctionType controlGroupFunction;
    private PromotionConfiguration configuration = new PromotionConfiguration();

    public Promotion(final PromotionType promotionType) {
        this.promotionType = promotionType;
    }

    public PromotionType getPromotionType() {
        return promotionType;
    }

    public void setPromotionType(final PromotionType promotionType) {
        this.promotionType = promotionType;
    }

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public List<Platform> getPlatforms() {
        return platforms;
    }

    public String getPlatformsAsString() {
        if (platforms == null || platforms.isEmpty()) {
            return "";
        }
        return StringUtils.join(platforms.toArray(), ",");
    }

    public void setPlatforms(final List<Platform> platforms) {
        this.platforms = platforms;
    }

    public DateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(final DateTime startDate) {
        this.startDate = startDate;
    }

    public DateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(final DateTime endDate) {
        this.endDate = endDate;
    }

    public boolean isAllPlayers() {
        return allPlayers;
    }

    public void setAllPlayers(final boolean allPlayers) {
        this.allPlayers = allPlayers;
    }

    public int getPlayerCount() {
        return playerCount;
    }

    public void setPlayerCount(final int playerCount) {
        this.playerCount = playerCount;
    }

    public int getSeed() {
        return seed;
    }

    public void setSeed(final int seed) {
        this.seed = seed;
    }

    public int getControlGroupPercentage() {
        return controlGroupPercentage;
    }

    public void setControlGroupPercentage(final int controlGroupPercentage) {
        this.controlGroupPercentage = controlGroupPercentage;
    }

    public PromotionConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(final PromotionConfiguration configuration) {
        if (configuration == null) {
            this.configuration = new PromotionConfiguration();
        } else {
            this.configuration = configuration;
        }
    }

    public void addConfigurationItem(final String key, final String value) {
        if (key != null && value != null) {
            configuration.addConfigurationItem(key, value);
        }
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(final Integer priority) {
        this.priority = priority;
    }

    public ControlGroupFunctionType getControlGroupFunction() {
        return controlGroupFunction;
    }

    public void setControlGroupFunction(final ControlGroupFunctionType controlGroupFunction) {
        this.controlGroupFunction = controlGroupFunction;
    }

    public boolean isExpired() {
        final DateTime currentDateTime = new DateTime();
        if (startDate == null || endDate == null) {
            return false;
        }
        return (endDate.compareTo(currentDateTime) < 0);

    }

    public boolean isInFuture() {
        final DateTime currentDateTime = new DateTime();
        if (startDate == null || endDate == null) {
            return false;
        }
        return (startDate.compareTo(currentDateTime) > 0);
    }

    public boolean isActive() {
        return (!this.isInFuture() && !this.isExpired());
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        Promotion rhs = (Promotion) obj;
        return new EqualsBuilder()
                .append(this.promotionType, rhs.promotionType)
                .append(this.id, rhs.id)
                .append(this.name, rhs.name)
                .append(this.platforms, rhs.platforms)
                .append(this.startDate, rhs.startDate)
                .append(this.endDate, rhs.endDate)
                .append(this.priority, rhs.priority)
                .append(this.allPlayers, rhs.allPlayers)
                .append(this.playerCount, rhs.playerCount)
                .append(this.seed, rhs.seed)
                .append(this.controlGroupPercentage, rhs.controlGroupPercentage)
                .append(this.controlGroupFunction, rhs.controlGroupFunction)
                .append(this.configuration, rhs.configuration)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(promotionType)
                .append(id)
                .append(name)
                .append(platforms)
                .append(startDate)
                .append(endDate)
                .append(priority)
                .append(allPlayers)
                .append(playerCount)
                .append(seed)
                .append(controlGroupPercentage)
                .append(controlGroupFunction)
                .append(configuration)
                .toHashCode();
    }


    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("MIN_SEED_VALUE", MIN_SEED_VALUE)
                .append("MAX_SEED_VALUE", MAX_SEED_VALUE)
                .append("MIN_CONTROL_GROUP_PERCENTAGE", MIN_CONTROL_GROUP_PERCENTAGE)
                .append("MAX_CONTROL_GROUP_PERCENTAGE", MAX_CONTROL_GROUP_PERCENTAGE)
                .append("promotionType", promotionType)
                .append("id", id)
                .append("name", name)
                .append("platforms", platforms)
                .append("startDate", startDate)
                .append("endDate", endDate)
                .append("priority", priority)
                .append("allPlayers", allPlayers)
                .append("playerCount", playerCount)
                .append("seed", seed)
                .append("controlGroupPercentage", controlGroupPercentage)
                .append("controlGroupFunction", controlGroupFunction)
                .append("configuration", configuration)
                .toString();
    }
}
