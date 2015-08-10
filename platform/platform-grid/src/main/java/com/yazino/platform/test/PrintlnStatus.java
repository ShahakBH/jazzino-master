package com.yazino.platform.test;

import com.yazino.game.api.*;
import com.yazino.game.api.document.DocumentBuilder;
import com.yazino.game.api.document.Documentable;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.*;

import static com.yazino.game.api.document.DocumentAccessors.*;
import static org.apache.commons.lang3.Validate.notNull;

public class PrintlnStatus implements Serializable, ObservableStatus, Documentable {
    private static final long serialVersionUID = 3009245397229647302L;

    private final List<PlayerAtTableInformation> playersAtTable = new ArrayList<>();

    private String name;
    private Long id;
    private int joiningDesirability = 0;

    public PrintlnStatus() {
    }

    public PrintlnStatus(final Map<String, Object> document) {
        notNull(document, "document may not be null");

        for (Map<String, Object> playerAtTable : listOfDocumentsFor(document, "playersAtTable")) {
            playersAtTable.add(PlayerAtTableInformation.fromDocument(playerAtTable));
        }

        name = stringFor(document, "name");
        id = nullableLongFor(document, "id");
        joiningDesirability = intFor(document, "joiningDesirability");
    }

    @Override
    public Map<String, Object> toDocument() {
        return new DocumentBuilder()
                .withListOf("playersAtTable", playersAtTable)
                .withString("name", name)
                .withNullableLong("id", id)
                .withInt("joiningDesirability", joiningDesirability)
                .toDocument();
    }

    public Object observableStatus() {
        return this;
    }

    public Object statusForPlayer(final GamePlayer player) {
        return this;
    }

    Collection<PlayerAtTableInformation> getPlayerInformation() {
        return playersAtTable;
    }

    public void setPlayersAtTable(final Collection<PlayerAtTableInformation> playersAtTable) {
        this.playersAtTable.clear();
        this.playersAtTable.addAll(playersAtTable);
    }

    public void addPlayer(final GamePlayer gamePlayer) {
        this.playersAtTable.add(new PlayerAtTableInformation(gamePlayer, Collections.<String, String>emptyMap()));
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public List<ObservableChange> getObservableChanges() {
        return new ArrayList<ObservableChange>(Arrays.asList(new ObservableChange(2, new String[0])));
    }

    public Set<String> getAllowedActions() {
        return new HashSet<String>();
    }

    public Set<String> getWarningCodes() {
        return new HashSet<String>();
    }

    int getJoiningDesirability() {
        return joiningDesirability;
    }

    public void setJoiningDesirability(final int joiningDesirability) {
        this.joiningDesirability = joiningDesirability;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        final PrintlnStatus rhs = (PrintlnStatus) obj;
        return new EqualsBuilder()
                .append(id, rhs.id)
                .append(name, rhs.name)
                .append(playersAtTable, rhs.playersAtTable)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(id)
                .append(name)
                .append(playersAtTable)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(id)
                .append(name)
                .append(playersAtTable)
                .toString();
    }

    public ObservableTimeOutEventInfo getNextEvent() {
        return null;
    }
}
