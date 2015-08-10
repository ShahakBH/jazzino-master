package strata.server.lobby.controlcentre.model;

public enum Allocator {

    EVEN_BY_BALANCE("Even, sorted by balance"),
    EVEN_RANDOM("Even, sorted randomly");

    private String description;

    private Allocator(final String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
