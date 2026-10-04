package dev.toolkit;
public enum Category {
    COMBAT("Combat"), RENDER("Render"), PLAYER("Player"), CLIENT("Client");
    public final String title;
    Category(String t) { title = t; }
}
