package com.alexlego19.ainpcs.data;

public enum Temperament {
    PASSIVE("Passive"),
    NEUTRAL("Neutral");

    private final String displayName;

    Temperament(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
