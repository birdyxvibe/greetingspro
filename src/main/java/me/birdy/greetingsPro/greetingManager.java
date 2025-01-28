package me.birdy.greetingsPro;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class greetingManager {
    private FileConfiguration greetingsConfig;
    private File greetingData;
    private String defaultGreeting;
    private Map<Integer, String> customGreetings;

    public greetingManager(JavaPlugin plugin) {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        greetingData = new File(dataFolder, "greetings.yml");
        if (!greetingData.exists()) {
            try {
                plugin.saveResource("greetings.yml", false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        greetingsConfig = YamlConfiguration.loadConfiguration(greetingData);
        loadGreetings();
    }

    private void loadGreetings(){
        defaultGreeting = greetingsConfig.getString("default", "&#00ff00+ &7%player%");

        customGreetings = new HashMap<>();

        if(greetingsConfig.contains("greetings")){
            for(String key : greetingsConfig.getConfigurationSection("greetings").getKeys(false)){
                try {
                    int id = Integer.parseInt(key);
                    String greeting = greetingsConfig.getString("greetings." + key);
                    customGreetings.put(id, greeting);  // Add greeting to map
                } catch (NumberFormatException e){
                    e.printStackTrace();
                }
            }
        }
    }

    public String getGreeting(int id, String playerName){
        if(id == 0){
            return defaultGreeting.replace("%player%", playerName);
        } else {
            String customGreeting = customGreetings.get(id);
            if (customGreeting != null) {
                return customGreeting.replace("%player%", playerName);
            } else {
                return null;
            }
        }
    }

}
