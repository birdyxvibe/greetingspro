package me.birdy.greetingsPro;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;

public final class GreetingsPro extends JavaPlugin implements Listener {

    static Connection connection;
    private greetingManager greetingManager;
    private greetingGUI greetingGUI;

    @Override
    public void onEnable() {
        greetingManager = new greetingManager(this);
        greetingGUI = new greetingGUI(this, greetingManager);

        setupDatabase();

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(greetingGUI, this);

        getCommand("greetings").setExecutor(new GreetingGUIExecutor());
        getCommand("greetings-give").setExecutor(new GiveGreetingCommand());

        System.out.println("GreetingsPro has loaded.");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event){
        Player player = event.getPlayer();
        int greetingId = getPlayerGreetingId(player);
        String rawGreeting = greetingManager.getGreeting(greetingId, player.getName());
        String coloredGreeting =  rawGreeting.replaceAll("&", "§");
        event.setJoinMessage(ChatColor.GREEN + "+ " + coloredGreeting);
    }

    @EventHandler
    public void onPlayerJoin(PlayerQuitEvent event){
        String playerName = event.getPlayer().getName();
        event.setQuitMessage(ChatColor.RED + "- " + ChatColor.GRAY + playerName);
    }

    @Override
    public void onDisable() {
        closeDatabaseConnection();
        System.out.println("GreetingsPro has stopped.");
    }

    public class GreetingGUIExecutor implements CommandExecutor{
        @Override
        public boolean onCommand(CommandSender sender, Command common, String label, String[] args){
            if(sender instanceof Player) {
                Player player = (Player) sender;
            greetingGUI.openGreetingsGUI(player);
            return true;
            } else {
                sender.sendMessage("Only players can use this command!");
                return false;
            }
        }
    }

    public class GiveGreetingCommand implements CommandExecutor{
        @Override
        public boolean onCommand(CommandSender sender, Command common, String label, String[] args){
            if(sender instanceof Player){
                Player player = (Player) sender;
                if(args.length == 2){
                    Player target = Bukkit.getPlayer(args[0]);
                    try {
                        int greetingId = Integer.parseInt(args[1]);

                        if(greetingManager.getGreeting(greetingId, player.getName()) != null){
                            giveGreeting(player, target, greetingId);
                        } else {
                            player.sendMessage(ChatColor.RED + "Invalid greeting ID!");
                        }
                    } catch (NumberFormatException e){
                        player.sendMessage(ChatColor.RED + "Invalid ID format! Please provide a valid number.");
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "Usage: /greetings give <user> <greeting-id>");
                }
            }
            return true;
        }
    }

    private void setupDatabase(){
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + getDataFolder() + "/greetings.db");

            String createTableQuery = "CREATE TABLE IF NOT EXISTS player_greetings (" +
                    "player_uuid TEXT PRIMARY KEY, " +
                    "greeting_id INTEGER, " +
                    "owned_greetings TEXT)";
            Statement statement = connection.createStatement();
            statement.executeUpdate(createTableQuery);
        } catch (SQLException e){
            getLogger().severe("Failed to set up the database connection!");
            e.printStackTrace();
        }
    }

    private int getPlayerGreetingId(Player player){
        try {
            String query = "SELECT greeting_id FROM player_greetings WHERE player_uuid = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, ""+player.getUniqueId());
            ResultSet resultSet = statement.executeQuery();

            if(resultSet.next()) {
                return resultSet.getInt("greeting_id");
            } else {
                return 0;
            }
        } catch (SQLException e){
            e.printStackTrace();
            return 1;
        }
    }

    public void setPlayerGreetingId(Player player, int greetingId){
        try {
            String query;
            query = "UPDATE player_greetings SET greeting_id = ? WHERE player_uuid = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, greetingId);
            statement.setString(2, ""+player.getUniqueId());
            statement.executeUpdate();

            player.sendMessage(ChatColor.GREEN + "Your greeting has been updated to: + "+ greetingManager.getGreeting(greetingId, player.getName()).replaceAll("&", "§"));

        } catch (SQLException e){
            e.printStackTrace();
        }
    }

    private void giveGreeting(Player user, Player target, int greetingId){
        try {

            ensurePlayerExists(""+target.getUniqueId());

            String checkQuery = "SELECT player_uuid FROM player_greetings, json_each(owned_greetings) WHERE player_uuid = ? AND json_each.value = ?";
            PreparedStatement checkStatement = connection.prepareStatement(checkQuery);
            checkStatement.setString(1, ""+target.getUniqueId());
            checkStatement.setInt(2, greetingId);
            ResultSet resultSet = checkStatement.executeQuery();

            String query;
            if (resultSet.next()) {
                user.sendMessage(ChatColor.YELLOW + target.getName() + " already has that greeting!");
            } else {
                query = "UPDATE player_greetings " +
                        "SET owned_greetings = json_insert(owned_greetings, '$[#]', ?) " +
                        "WHERE player_uuid = ?";
                PreparedStatement statement = connection.prepareStatement(query);
                statement.setInt(1, greetingId);
                statement.setString(2, ""+target.getUniqueId());
                statement.executeUpdate();

                user.sendMessage(ChatColor.GREEN + "Gave " + target.getName() + " greeting ID "+ greetingId + "!");
            }
        } catch (SQLException e){
            e.printStackTrace();
        }
    }

    public static void ensurePlayerExists(String playerUuid){
        try {
            String checkQuery = "SELECT player_uuid FROM player_greetings WHERE player_uuid = ?";
            PreparedStatement checkStatement = connection.prepareStatement(checkQuery);
            checkStatement.setString(1, playerUuid);
            ResultSet resultSet = checkStatement.executeQuery();

            if(!resultSet.next()){
                String insertQuery = "INSERT INTO player_greetings (player_uuid, owned_greetings, greeting_id) VALUES (?, ?, ?)";
                PreparedStatement insertCheck = connection.prepareStatement(insertQuery);
                insertCheck.setString(1, playerUuid);
                insertCheck.setString(2, "[]");
                insertCheck.setInt(3, 0);
                insertCheck.executeUpdate();
            }

        } catch(SQLException e){
            e.printStackTrace();
        }
    }

    private void closeDatabaseConnection() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
