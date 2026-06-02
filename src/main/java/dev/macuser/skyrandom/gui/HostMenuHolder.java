package dev.macuser.skyrandom.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class HostMenuHolder implements InventoryHolder {

    public enum Section {
        MAIN,
        SUDDEN_NIGHT,
        GAME_SPEED,
        MAP_SELECTION,
        SHRINKING_ZONE,
        RANDOM_EVENTS
    }

    private final Section section;
    private Inventory inventory;

    public HostMenuHolder() {
        this(Section.MAIN);
    }

    public HostMenuHolder(Section section) {
        this.section = section == null ? Section.MAIN : section;
    }

    public Section getSection() {
        return section;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
