package com.mythicrpg.traveling;

public enum TravelingXpSource {
    MOVEMENT(false),
    STRUCTURE(true),
    DIMENSION(true),
    TREASURE(true);

    private final boolean discovery;

    TravelingXpSource(boolean discovery) {
        this.discovery = discovery;
    }

    public boolean isDiscovery() {
        return discovery;
    }
}
