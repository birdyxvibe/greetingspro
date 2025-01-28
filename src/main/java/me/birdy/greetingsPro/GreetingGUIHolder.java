package me.birdy.greetingsPro;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class GreetingGUIHolder implements InventoryHolder {
    private final String invName;

    public GreetingGUIHolder(String invName) {
        this.invName = invName;
    }

    @Override
    public Inventory getInventory(){
        return null;
    }

    public String getInvName(){
        return invName;
    }

}
