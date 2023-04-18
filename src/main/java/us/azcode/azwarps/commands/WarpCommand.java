package us.azcode.azwarps.commands;

import net.milkbowl.vault.economy.Economy;
import us.azcode.azwarps.AzWarps;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;


public class WarpCommand implements CommandExecutor {
    private AzWarps plugin;
    private FileConfiguration config;
    private FileConfiguration messages;
    private FileConfiguration warps;
    private Economy econ;
    private HashMap<UUID, Integer> playerWarpCount;

    public WarpCommand(AzWarps plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        this.messages = plugin.getMessages();
        this.econ = plugin.getEconomy();
        this.playerWarpCount = new HashMap<>();

        // Cargar archivo warps.yml
        this.warps = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "warps.yml"));
    }

    private String getMessage(String key, Object... args) {
        String message = messages.getString(key, key);
        if (args.length > 0) {
            try {
                message = String.format(message, args);
            } catch (IllegalFormatException e) {
                plugin.getLogger().log(Level.WARNING, "Invalid format in message: " + key, e);
            }
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }


    private void saveWarps() {
        try {
            warps.save(new File(plugin.getDataFolder(), "warps.yml"));
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "No se pudo guardar warps.yml", e);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getMessage("noConsole"));
            return true;
        }

        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();

        if (cmd.getName().equalsIgnoreCase("setpwarp")) {
            if (args.length != 1) {
                player.sendMessage(getMessage("incorrectUsage", "setwarp", "<name>"));
                return true;
            }

            if (!playerWarpCount.containsKey(uuid) || playerWarpCount.get(uuid) == 0) {
                player.sendMessage(getMessage("noWarpPermission"));
                return true;
            }

            String warpName = args[0];
            Location location = player.getLocation();

            // Guarda el warp en warps.yml en lugar de config.yml
            warps.set("warps." + warpName + ".world", location.getWorld().getName());
            warps.set("warps." + warpName + ".x", location.getX());
            warps.set("warps." + warpName + ".y", location.getY());
            warps.set("warps." + warpName + ".z", location.getZ());
            warps.set("warps." + warpName + ".yaw", location.getYaw());
            warps.set("warps." + warpName + ".pitch", location.getPitch());
            warps.set("warps." + warpName + ".owner", uuid.toString());
            warps.set("warps." + warpName + ".public", false);
            saveWarps();

            playerWarpCount.put(uuid, playerWarpCount.get(uuid) - 1);
            player.sendMessage(getMessage("warpSet", warpName));

        } else if (cmd.getName().equalsIgnoreCase("delwarp")) {
            if (args.length != 1) {
                player.sendMessage(getMessage("incorrectUsage", "delpwarp", "<name>"));
                return true;
            }

            String warpName = args[0];
            if (!config.contains("warps." + warpName)) {
                player.sendMessage(getMessage("warpNotFound", warpName));
                return true;
            }

            config.set("warps." + warpName, null);
            plugin.saveConfig();
            player.sendMessage(getMessage("warpDeleted", warpName));

        } else if (cmd.getName().equalsIgnoreCase("playerwarp")) {
            if (args.length != 2) {
                player.sendMessage(getMessage("incorrectUsage", "warp", "<name> <owner>"));
                return true;
            }

            String warpName = args[0];
            String ownerName = args[1];
            @SuppressWarnings("deprecation")
			UUID ownerUUID = Bukkit.getOfflinePlayer(ownerName).getUniqueId();

            if (!warps.contains("warps." + warpName) || !warps.getString("warps." + warpName + ".owner").equals(ownerUUID.toString())) {
                player.sendMessage(getMessage("warpNotFound", warpName));
                return true;
            }

            Location warpLocation = new Location(
                    Bukkit.getWorld(warps.getString("warps." + warpName + ".world")),
                    warps.getDouble("warps." + warpName + ".x"),
                    warps.getDouble("warps." + warpName + ".y"),
                    warps.getDouble("warps." + warpName + ".z"),
                    (float) warps.getDouble("warps." + warpName + ".yaw"),
                    (float) warps.getDouble("warps." + warpName + ".pitch")
            );

            player.teleport(warpLocation);
            player.sendMessage(getMessage("teleportedToWarp", warpName));
        }
        else if (cmd.getName().equalsIgnoreCase("buywarp")) {
            double warpCost = config.getDouble("warpCost");

            if (econ.getBalance(player) < warpCost) {
                player.sendMessage(getMessage("notEnoughMoney", econ.format(warpCost)));
                return true;
            }

            econ.withdrawPlayer(player, warpCost);
            playerWarpCount.put(uuid, playerWarpCount.getOrDefault(uuid, 0) + 1);
            player.sendMessage(getMessage("warpBought", playerWarpCount.get(uuid)));

        } else if (cmd.getName().equalsIgnoreCase("listwarps")) {
            int warpCount = playerWarpCount.getOrDefault(uuid, 0);
            List<String> playerWarps = new ArrayList<>();
            ConfigurationSection warpsSection = warps.getConfigurationSection("warps");
            if (warpsSection != null) {
                for (String warpName : warpsSection.getKeys(false)) {
                    String ownerUUIDString = warps.getString("warps." + warpName + ".owner");
                    if (uuid.toString().equals(ownerUUIDString)) {
                        playerWarps.add(warpName);
                    }
                }
            }

            if (playerWarps.isEmpty()) {
                if (warpCount > 0) {
                    player.sendMessage(getMessage("warpsRemaining", warpCount));
                } else {
                    player.sendMessage(getMessage("noWarps"));
                }
            } else {
                player.sendMessage(getMessage("yourWarps", String.join(", ", playerWarps)));
            }

        } else if (cmd.getName().equalsIgnoreCase("addwarpaccess")) {
            if (args.length != 2) {
                player.sendMessage(getMessage("incorrectUsage", "addwarpaccess", "<nombre> <jugador>"));
                return true;
            }

            String warpName = args[0];
            if (!config.contains("warps." + warpName)) {
                player.sendMessage(getMessage("warpNotFound", warpName));
                return true;
            }

            String ownerUUID = config.getString("warps." + warpName + ".owner");
            if (!uuid.toString().equals(ownerUUID)) {
                player.sendMessage(getMessage("notWarpOwner", warpName));
                return true;
            }

            Player targetPlayer = Bukkit.getPlayer(args[1]);
            if (targetPlayer == null) {
                player.sendMessage(getMessage("playerNotFound", args[1]));
                return true;
            }

            String targetUUID = targetPlayer.getUniqueId().toString();
            List<String> accessList = config.getStringList("warps." + warpName + ".access");

            if (accessList.contains(targetUUID)) {
                player.sendMessage(getMessage("accessAlreadyGranted", targetPlayer.getName(), warpName));
                return true;
            }

            accessList.add(targetUUID);
            config.set("warps." + warpName + ".access", accessList);
            plugin.saveConfig();

            player.sendMessage(getMessage("accessGranted", targetPlayer.getName(), warpName));

        } else if (cmd.getName().equalsIgnoreCase("setwarpaccess")) {
            if (args.length != 2) {
                player.sendMessage(getMessage("incorrectUsage", "setwarpaccess", "<nombre> <public|private>"));
                return true;
            }

            String warpName = args[0];

            if (!warps.contains("warps." + warpName)) {
                player.sendMessage(getMessage("warpNotFound", warpName));
                return true;
            }

            String ownerUUID = warps.getString("warps." + warpName + ".owner");
            if (!uuid.toString().equals(ownerUUID)) {
                player.sendMessage(getMessage("notWarpOwner", warpName));
                return true;
            }

            String accessType = args[1];
            boolean isPublic = accessType.equalsIgnoreCase("public");
            warps.set("warps." + warpName + ".public", isPublic);
            saveWarps();

            player.sendMessage(getMessage("accessTypeChanged", warpName, accessType));

        } else if (cmd.getName().equalsIgnoreCase("listpublicwarps")) {
            List<String> publicWarps = new ArrayList<>();

            ConfigurationSection warpsSection = warps.getConfigurationSection("warps");
            if (warpsSection != null) {
                for (String warpName : warpsSection.getKeys(false)) {
                    boolean isPublic = warps.getBoolean("warps." + warpName + ".public");
                    if (isPublic) {
                        publicWarps.add(warpName);
                    }
                }
            }

            if (publicWarps.isEmpty()) {
                player.sendMessage(getMessage("noPublicWarps"));
            } else {
                player.sendMessage(getMessage("publicWarpsHeader"));
                player.sendMessage(String.join(", ", publicWarps));
            }
        }
        return true;
    }
}
