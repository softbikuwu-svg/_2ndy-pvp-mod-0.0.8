package com.secondy.client.module;

public enum Category {
    HUD("HUD"),
    PVP("PvP"),
    UTILITY("Utility"),
    VISUAL("Visual"),
    OPTIMIZE("Optimize"),
    COLOR("Color");

    public final String label;

    Category(String label) {
        this.label = label;
    }
}
