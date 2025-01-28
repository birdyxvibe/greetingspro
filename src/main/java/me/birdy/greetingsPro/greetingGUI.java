package me.birdy.greetingsPro;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.json.JSONArray;

import org.bukkit.persistence.PersistentDataType;

import static me.birdy.greetingsPro.GreetingsPro.connection;
import me.birdy.greetingsPro.greetingManager.*;

public class greetingGUI implements Listener {
    private static final int INVENTORY_SIZE = 9;
    private static final String INVENTORY_TITLE = "Select A Greeting!";

    private static greetingManager greetingManager;
    private static me.birdy.greetingsPro.GreetingsPro GreetingsPro;

    public greetingGUI(GreetingsPro greetingsPro, greetingManager greetingManager){
        this.GreetingsPro = greetingsPro;
        this.greetingManager = greetingManager;
    }

    public static void openGreetingsGUI(Player player){
        GreetingGUIHolder holder = new GreetingGUIHolder(INVENTORY_TITLE);
        Inventory gui = Bukkit.createInventory(holder, INVENTORY_SIZE, INVENTORY_TITLE);

        List<Integer> greetings = getAvailableGreetings(player);
        int slot = 0;

        for(int greetingId: greetings){

            String greeting = greetingManager.getGreeting(greetingId, ""+player.getName()).replaceAll("&", "§");

            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            if(meta != null){
                meta.setDisplayName(greeting);
                meta.getPersistentDataContainer().set(new NamespacedKey("greetingspro", "greetingid"), PersistentDataType.INTEGER, greetingId);
                item.setItemMeta(meta);
            }

            gui.setItem(slot, item);
            slot++;
        }

        ItemStack itemC = new ItemStack(Material.BARRIER);
        ItemMeta meta  = itemC.getItemMeta();
        if(meta != null){
            meta.setDisplayName(ChatColor.RED + "Remove Greeting");
            meta.getPersistentDataContainer().set(new NamespacedKey("greetingspro", "greetingid"), PersistentDataType.INTEGER, 0);
            itemC.setItemMeta(meta);
        }
        gui.setItem(8, itemC);

        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event){
        ItemStack clickedItem = event.getCurrentItem();
        Inventory inventory = event.getInventory();

        if(inventory.getHolder() instanceof GreetingGUIHolder) {
            event.setCancelled(true);

            if (clickedItem != null && clickedItem.hasItemMeta()) {
                ItemMeta meta = clickedItem.getItemMeta();
                if (meta != null) {
                    PersistentDataContainer container = meta.getPersistentDataContainer();
                    NamespacedKey greetingKey = new NamespacedKey("greetingspro", "greetingid");

                    if (container.has(greetingKey, PersistentDataType.INTEGER)) {
                        int greetingId = container.get(greetingKey, PersistentDataType.INTEGER);

                        Player player = (Player) event.getWhoClicked();

                        GreetingsPro.setPlayerGreetingId(player, greetingId);

                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    private static List<Integer> getAvailableGreetings(Player player) {
        try {
            GreetingsPro.ensurePlayerExists(""+player.getUniqueId());

            String checkQuery = "SELECT * FROM player_greetings WHERE player_uuid = ?";
            PreparedStatement checkStatement = connection.prepareStatement(checkQuery);
            checkStatement.setString(1, "" + player.getUniqueId());
            ResultSet resultSet = checkStatement.executeQuery();

            List<Integer> greetings = new ArrayList<>();

            if(resultSet.next()){
                String ownedGreetingsJson = resultSet.getString("owned_greetings");
                JSONArray ownedGreetings = new JSONArray(ownedGreetingsJson);
                for(int i = 0; i < ownedGreetings.length(); i++){
                    greetings.add(ownedGreetings.getInt(i));
                }
            }

            return greetings;

        }catch (SQLException e){
            e.printStackTrace();
            return List.of();
        }
    }
}
