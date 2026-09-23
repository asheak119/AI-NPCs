package com.alexlego19.ainpcs.data;

public enum Fortitude {
    LOW("Low", 2.0f),
    MEDIUM("Medium", 4.0f),
    HIGH("High", 6.0f);

    private final String displayName;
    private final float damageThreshold;

    Fortitude(String displayName, float damageThreshold) {
        this.displayName = displayName;
        this.damageThreshold = damageThreshold;
    }

    public String getDisplayName() {
        return displayName;
    }

    public float getDamageThreshold() {
        return damageThreshold;
    }
}
